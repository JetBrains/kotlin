/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.extractRadix

internal fun ConeKotlinType.enhance(session: FirSession, qualifiers: IndexedJavaTypeQualifiers): ConeKotlinType? =
    enhanceConeKotlinType(session, qualifiers, 0)

private fun ConeKotlinType.enhanceConeKotlinType(
    session: FirSession,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int,
): ConeKotlinType? {
    return when (this) {
        is ConeFlexibleType -> {
            val lowerResult = lowerBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_LOWER, qualifiers, index
            )
            val upperResult = upperBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_UPPER, qualifiers, index
            )

            when {
                lowerResult == null && upperResult == null -> null
                this is ConeRawType -> ConeRawType(lowerResult ?: lowerBound, upperResult ?: upperBound)
                else -> coneFlexibleOrSimpleType(session.typeContext, lowerResult ?: lowerBound, upperResult ?: upperBound)
            }
        }
        is ConeSimpleKotlinType -> enhanceInflexibleType(
            session, TypeComponentPosition.INFLEXIBLE, qualifiers, index
        )
        else -> null
    }
}

internal fun ClassId.readOnlyToMutable(): ClassId? {
    return JavaToKotlinClassMap.readOnlyToMutable(this)
}

private fun ClassId.mutableToReadOnly(): ClassId? {
    return JavaToKotlinClassMap.mutableToReadOnly(this)
}

private fun ConeKotlinType.enhanceInflexibleType(
    session: FirSession,
    position: TypeComponentPosition,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int,
): ConeKotlinType? {
    require(this !is ConeFlexibleType) { "$this should not be flexible" }
    val shouldEnhance = position.shouldEnhance()
    if ((!shouldEnhance && typeArguments.isEmpty()) || this !is ConeLookupTagBasedType) {
        return null
    }

    val effectiveQualifiers = qualifiers[index]
    val enhancedTag = lookupTag.enhanceMutability(effectiveQualifiers, position)

    // TODO: implement warnings
    val nullabilityFromQualifiers = effectiveQualifiers.nullability
        .takeIf { shouldEnhance && !effectiveQualifiers.isNullabilityQualifierForWarning }
    val enhancedNullability = when (nullabilityFromQualifiers) {
        NullabilityQualifier.NULLABLE -> true
        NullabilityQualifier.NOT_NULL -> false
        else -> isNullable
    }

    var globalArgIndex = index + 1
    val enhancedArguments = typeArguments.map { arg ->
        val argIndex = globalArgIndex.also { globalArgIndex = qualifiers.nextSibling(it) }
        arg.type?.enhanceConeKotlinType(session, qualifiers, argIndex)?.let {
            when (arg.kind) {
                ProjectionKind.IN -> ConeKotlinTypeProjectionIn(it)
                ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(it)
                ProjectionKind.STAR -> ConeStarProjection
                ProjectionKind.INVARIANT -> it
            }
        }
    }

    val shouldAddAttribute = nullabilityFromQualifiers == NullabilityQualifier.NOT_NULL && !hasEnhancedNullability
    if (lookupTag == enhancedTag && enhancedNullability == isNullable && !shouldAddAttribute && enhancedArguments.all { it == null }) {
        return null // absolutely no changes
    }

    val mergedArguments = Array(typeArguments.size) { enhancedArguments[it] ?: typeArguments[it] }
    val mergedAttributes = if (shouldAddAttribute) attributes + CompilerConeAttributes.EnhancedNullability else attributes
    val enhancedType = enhancedTag.constructType(mergedArguments, enhancedNullability, mergedAttributes)
    return if (effectiveQualifiers.definitelyNotNull)
        ConeDefinitelyNotNullType.create(enhancedType, session.typeContext) ?: enhancedType
    else
        enhancedType
}

private fun ConeClassifierLookupTag.enhanceMutability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition
): ConeClassifierLookupTag {
    if (!position.shouldEnhance()) return this
    if (this !is ConeClassLikeLookupTag) return this // mutability is not applicable for type parameters

    when (qualifiers.mutability) {
        MutabilityQualifier.READ_ONLY -> {
            val readOnlyId = classId.mutableToReadOnly()
            if (position == TypeComponentPosition.FLEXIBLE_LOWER && readOnlyId != null) {
                return ConeClassLikeLookupTagImpl(readOnlyId)
            }
        }
        MutabilityQualifier.MUTABLE -> {
            val mutableId = classId.readOnlyToMutable()
            if (position == TypeComponentPosition.FLEXIBLE_UPPER && mutableId != null) {
                return ConeClassLikeLookupTagImpl(mutableId)
            }
        }
        null -> {}
    }

    return this
}

internal fun ConeKotlinType.lexicalCastFrom(session: FirSession, value: String): FirExpression? {
    val lookupTagBasedType = when (this) {
        is ConeLookupTagBasedType -> this
        is ConeFlexibleType -> return lowerBound.lexicalCastFrom(session, value)
        else -> return null
    }
    val lookupTag = lookupTagBasedType.lookupTag
    val firElement = lookupTag.toSymbol(session)?.fir
    if (firElement is FirRegularClass && firElement.classKind == ClassKind.ENUM_CLASS) {
        val name = Name.identifier(value)
        val firEnumEntry = firElement.collectEnumEntries().find { it.name == name }

        return if (firEnumEntry != null) buildPropertyAccessExpression {
            calleeReference = buildResolvedNamedReference {
                this.name = name
                resolvedSymbol = firEnumEntry.symbol
            }
        } else if (firElement is FirJavaClass) {
            val firStaticProperty = firElement.declarations.filterIsInstance<FirJavaField>().find {
                it.isStatic && it.modality == Modality.FINAL && it.name == name
            }
            if (firStaticProperty != null) {
                buildPropertyAccessExpression {
                    calleeReference = buildResolvedNamedReference {
                        this.name = name
                        resolvedSymbol = firStaticProperty.symbol
                    }
                }
            } else null
        } else null
    }

    if (lookupTag !is ConeClassLikeLookupTag) return null
    val classId = lookupTag.classId
    if (classId.packageFqName != FqName("kotlin")) return null

    val (number, radix) = extractRadix(value)
    return when (classId.relativeClassName.asString()) {
        "Boolean" -> buildConstExpression(null, ConstantValueKind.Boolean, value.toBoolean())
        "Char" -> buildConstExpression(null, ConstantValueKind.Char, value.singleOrNull() ?: return null)
        "Byte" -> buildConstExpression(null, ConstantValueKind.Byte, number.toByteOrNull(radix) ?: return null)
        "Short" -> buildConstExpression(null, ConstantValueKind.Short, number.toShortOrNull(radix) ?: return null)
        "Int" -> buildConstExpression(null, ConstantValueKind.Int, number.toIntOrNull(radix) ?: return null)
        "Long" -> buildConstExpression(null, ConstantValueKind.Long, number.toLongOrNull(radix) ?: return null)
        "Float" -> buildConstExpression(null, ConstantValueKind.Float, value.toFloatOrNull() ?: return null)
        "Double" -> buildConstExpression(null, ConstantValueKind.Double, value.toDoubleOrNull() ?: return null)
        "String" -> buildConstExpression(null, ConstantValueKind.String, value)
        else -> null
    }
}
