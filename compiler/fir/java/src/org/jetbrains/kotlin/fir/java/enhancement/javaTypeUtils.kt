/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildQualifiedAccessExpression
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.JavaDefaultQualifiers
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.DEFAULT_NULL_FQ_NAME
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.DEFAULT_VALUE_FQ_NAME
import org.jetbrains.kotlin.load.java.descriptors.AnnotationDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.NullDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.StringDefaultValue
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.RawType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.extractRadix

internal class IndexedJavaTypeQualifiers(private val data: Array<JavaTypeQualifiers>) {
    constructor(size: Int, compute: (Int) -> JavaTypeQualifiers) : this(Array(size) { compute(it) })

    operator fun invoke(index: Int) = data.getOrElse(index) { JavaTypeQualifiers.NONE }

    val size: Int get() = data.size
}

internal fun FirJavaTypeRef.enhance(
    session: FirSession,
    qualifiers: IndexedJavaTypeQualifiers,
    typeWithoutEnhancement: ConeKotlinType,
): FirResolvedTypeRef {
    return typeWithoutEnhancement.enhancePossiblyFlexible(session, annotations, qualifiers, 0)
}

// The index in the lambda is the position of the type component:
// Example: for `A<B, C<D, E>>`, indices go as follows: `0 - A<...>, 1 - B, 2 - C<D, E>, 3 - D, 4 - E`,
// which corresponds to the left-to-right breadth-first walk of the tree representation of the type.
// For flexible types, both bounds are indexed in the same way: `(A<B>..C<D>)` gives `0 - (A<B>..C<D>), 1 - B and D`.
private fun ConeKotlinType.enhancePossiblyFlexible(
    session: FirSession,
    annotations: List<FirAnnotationCall>,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int
): FirResolvedTypeRef {
    val enhanced = enhanceConeKotlinType(session, qualifiers, index)

    return buildResolvedTypeRef {
        this.type = enhanced
        this.annotations += annotations
    }
}

private fun ConeKotlinType.enhanceConeKotlinType(
    session: FirSession,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int
): ConeKotlinType {
    return when (this) {
        is ConeFlexibleType -> {
            val lowerResult = lowerBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_LOWER, qualifiers, index
            )
            val upperResult = upperBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_UPPER, qualifiers, index
            )

            when {
                lowerResult === lowerBound && upperResult === upperBound -> this
                this is ConeRawType -> ConeRawType(lowerResult, upperResult)
                else -> coneFlexibleOrSimpleType(
                    session, lowerResult, upperResult, isNotNullTypeParameter = qualifiers(index).isNotNullTypeParameter
                )
            }
        }
        is ConeSimpleKotlinType -> enhanceInflexibleType(session, TypeComponentPosition.INFLEXIBLE, qualifiers, index)
        else -> this
    }
}

private fun ConeKotlinType.subtreeSize(): Int {
    return 1 + typeArguments.sumBy { ((it as? ConeKotlinType)?.subtreeSize() ?: 0) + 1 }
}

private fun coneFlexibleOrSimpleType(
    session: FirSession,
    lowerBound: ConeKotlinType,
    upperBound: ConeKotlinType,
    isNotNullTypeParameter: Boolean
): ConeKotlinType {
    if (AbstractStrictEqualityTypeChecker.strictEqualTypes(session.typeContext, lowerBound, upperBound)) {
        val lookupTag = (lowerBound as? ConeLookupTagBasedType)?.lookupTag
        if (isNotNullTypeParameter && lookupTag is ConeTypeParameterLookupTag && !lowerBound.isMarkedNullable) {
            // TODO: we need enhancement for type parameter bounds for this code to work properly
            // At this moment, this condition is always true
            if (lookupTag.typeParameterSymbol.fir.bounds.any {
                    val type = it.coneType
                    type is ConeTypeParameterType || type.isNullable
                }
            ) {
                return ConeDefinitelyNotNullType.create(lowerBound) ?: lowerBound
            }
        }
        return lowerBound
    }
    return ConeFlexibleType(lowerBound, upperBound)
}

private val KOTLIN_COLLECTIONS = FqName("kotlin.collections")

private val KOTLIN_COLLECTIONS_PREFIX_LENGTH = KOTLIN_COLLECTIONS.asString().length + 1

internal fun ClassId.readOnlyToMutable(): ClassId? {
    val mutableFqName = JavaToKotlinClassMap.readOnlyToMutable(asSingleFqName().toUnsafe())
    return mutableFqName?.let {
        ClassId(KOTLIN_COLLECTIONS, FqName(it.asString().substring(KOTLIN_COLLECTIONS_PREFIX_LENGTH)), false)
    }
}

private fun ClassId.mutableToReadOnly(): ClassId? {
    val readOnlyFqName = JavaToKotlinClassMap.mutableToReadOnly(asSingleFqName().toUnsafe())
    return readOnlyFqName?.let {
        ClassId(KOTLIN_COLLECTIONS, FqName(it.asString().substring(KOTLIN_COLLECTIONS_PREFIX_LENGTH)), false)
    }
}

private fun ConeKotlinType.enhanceInflexibleType(
    session: FirSession,
    position: TypeComponentPosition,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int
): ConeKotlinType {
    require(this !is ConeFlexibleType) {
        "$this should not be flexible"
    }
    if (this !is ConeLookupTagBasedType) return this

    val originalTag = lookupTag

    val effectiveQualifiers = qualifiers(index)
    val enhancedTag = originalTag.enhanceMutability(effectiveQualifiers, position)

    var wereChangesInArgs = false

    val enhancedArguments = if (this is RawType) {
        // TODO: Support enhancing for raw types
        typeArguments
    } else {
        var globalArgIndex = index + 1
        typeArguments.map { arg ->
            if (arg.kind != ProjectionKind.INVARIANT) {
                globalArgIndex++
                arg
            } else {
                require(arg is ConeKotlinType) { "Should be invariant type: $arg" }
                globalArgIndex += arg.subtreeSize()

                arg.enhanceConeKotlinType(session, qualifiers, globalArgIndex).also {
                    if (it !== arg) {
                        wereChangesInArgs = true
                    }
                }
            }
        }.toTypedArray()
    }

    val enhancedNullability = getEnhancedNullability(effectiveQualifiers, position)

    if (!wereChangesInArgs && originalTag == enhancedTag && enhancedNullability == isNullable) return this

    val enhancedType = enhancedTag.constructType(enhancedArguments, enhancedNullability)

    // TODO: why all of these is needed
//    val enhancement = if (effectiveQualifiers.isNotNullTypeParameter) NotNullTypeParameter(enhancedType) else enhancedType
//    val nullabilityForWarning = nullabilityChanged && effectiveQualifiers.isNullabilityQualifierForWarning
//    val result = if (nullabilityForWarning) wrapEnhancement(enhancement) else enhancement

    return enhancedType
}

private fun getEnhancedNullability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition
): Boolean {
    if (!position.shouldEnhance()) return position == TypeComponentPosition.FLEXIBLE_UPPER

    return when (qualifiers.nullability) {
        NullabilityQualifier.NULLABLE -> true
        NullabilityQualifier.NOT_NULL -> false
        else -> position == TypeComponentPosition.FLEXIBLE_UPPER
    }
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
    }

    return this
}


internal data class TypeAndDefaultQualifiers(
    val type: FirTypeRef?, // null denotes '*' here
    val defaultQualifiers: JavaDefaultQualifiers?
)

internal fun FirTypeRef.typeArguments(): List<FirTypeProjection> =
    (this as? FirUserTypeRef)?.qualifier?.lastOrNull()?.typeArgumentList?.typeArguments.orEmpty()

internal fun JavaType.typeArguments(): List<JavaType?> = (this as? JavaClassifierType)?.typeArguments.orEmpty()

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

        return if (firEnumEntry != null) buildQualifiedAccessExpression {
            calleeReference = buildResolvedNamedReference {
                this.name = name
                resolvedSymbol = firEnumEntry.symbol
            }
        } else if (firElement is FirJavaClass) {
            val firStaticProperty = firElement.declarations.filterIsInstance<FirJavaField>().find {
                it.isStatic && it.modality == Modality.FINAL && it.name == name
            }
            if (firStaticProperty != null) {
                buildQualifiedAccessExpression {
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
        "Boolean" -> buildConstExpression(null, FirConstKind.Boolean, value.toBoolean())
        "Char" -> buildConstExpression(null, FirConstKind.Char, value.singleOrNull() ?: return null)
        "Byte" -> buildConstExpression(null, FirConstKind.Byte, number.toByteOrNull(radix) ?: return null)
        "Short" -> buildConstExpression(null, FirConstKind.Short, number.toShortOrNull(radix) ?: return null)
        "Int" -> buildConstExpression(null, FirConstKind.Int, number.toIntOrNull(radix) ?: return null)
        "Long" -> buildConstExpression(null, FirConstKind.Long, number.toLongOrNull(radix) ?: return null)
        "Float" -> buildConstExpression(null, FirConstKind.Float, value.toFloatOrNull() ?: return null)
        "Double" -> buildConstExpression(null, FirConstKind.Double, value.toDoubleOrNull() ?: return null)
        "String" -> buildConstExpression(null, FirConstKind.String, value)
        else -> null
    }
}

internal fun FirValueParameter.getDefaultValueFromAnnotation(): AnnotationDefaultValue? {
    annotations.find { it.classId == DEFAULT_VALUE_ID }
        ?.arguments?.firstOrNull()
        ?.safeAs<FirConstExpression<*>>()?.value?.safeAs<String>()
        ?.let { return StringDefaultValue(it) }

    if (annotations.any { it.classId == DEFAULT_NULL_ID }) {
        return NullDefaultValue
    }

    return null
}

private val DEFAULT_VALUE_ID = ClassId.topLevel(DEFAULT_VALUE_FQ_NAME)
private val DEFAULT_NULL_ID = ClassId.topLevel(DEFAULT_NULL_FQ_NAME)

