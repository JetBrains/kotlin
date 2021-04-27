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
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.isStatic
import org.jetbrains.kotlin.fir.declarations.modality
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildQualifiedAccessExpression
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
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.JavaDefaultQualifiers
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.extractRadix

internal class IndexedJavaTypeQualifiers(private val data: Array<JavaTypeQualifiers>) {
    constructor(size: Int, compute: (Int) -> JavaTypeQualifiers) : this(Array(size) { compute(it) })

    operator fun invoke(index: Int): JavaTypeQualifiers = data.getOrElse(index) { JavaTypeQualifiers.NONE }

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
            val isRawType = this is ConeRawType

            val lowerResult = lowerBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_LOWER, qualifiers, index, isRawType
            )
            val upperResult = upperBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_UPPER, qualifiers, index, isRawType
            )

            when {
                lowerResult === lowerBound && upperResult === upperBound -> this
                this is ConeRawType -> ConeRawType(lowerResult, upperResult)
                else -> coneFlexibleOrSimpleType(
                    session, lowerResult, upperResult, isNotNullTypeParameter = qualifiers(index).isNotNullTypeParameter
                )
            }
        }
        is ConeSimpleKotlinType -> enhanceInflexibleType(
            session, TypeComponentPosition.INFLEXIBLE, qualifiers, index, isBoundOfRawType = false
        )
        else -> this
    }
}

private fun ConeKotlinType.subtreeSize(): Int {
    return 1 + typeArguments.sumOf { ((it as? ConeKotlinType)?.subtreeSize() ?: 0) + 1 }
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
                return ConeDefinitelyNotNullType.create(
                    lowerBound,
                    session.typeContext,
                    useCorrectedNullabilityForFlexibleTypeParameters = true
                ) ?: lowerBound
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
    index: Int,
    @Suppress("UNUSED_PARAMETER") isBoundOfRawType: Boolean,
): ConeKotlinType {
    require(this !is ConeFlexibleType) { "$this should not be flexible" }
    val shouldEnhance = position.shouldEnhance()
    if (!shouldEnhance && typeArguments.isEmpty() || this !is ConeLookupTagBasedType) {
        return this
    }

    val originalTag = lookupTag

    val effectiveQualifiers = qualifiers(index)
    val enhancedTag = originalTag.enhanceMutability(effectiveQualifiers, position)

    var wereChangesInArgs = false

    var globalArgIndex = index + 1
    val enhancedArguments = typeArguments.map { arg ->
        if (arg.kind != ProjectionKind.INVARIANT) {
            globalArgIndex++
            arg
        } else {
            require(arg is ConeKotlinType) { "Should be invariant type: $arg" }
            globalArgIndex += arg.subtreeSize()

            val enhanced = arg.enhanceConeKotlinType(session, qualifiers, globalArgIndex)
            wereChangesInArgs = wereChangesInArgs || enhanced !== arg
            enhanced.type
        }
    }.toTypedArray()

    val (enhancedNullability, enhancedNullabilityAttribute) = getEnhancedNullability(effectiveQualifiers, position)
    wereChangesInArgs = wereChangesInArgs || (enhancedNullabilityAttribute != null && !this.hasEnhancedNullability)

    if (!wereChangesInArgs && originalTag == enhancedTag && enhancedNullability == isNullable) {
        return this
    }

    var attributes = this.attributes
    enhancedNullabilityAttribute?.let { attributes += it }

    // TODO: why all of these is needed
//    val enhancement = if (effectiveQualifiers.isNotNullTypeParameter) NotNullTypeParameter(enhancedType) else enhancedType
//    val nullabilityForWarning = nullabilityChanged && effectiveQualifiers.isNullabilityQualifierForWarning
//    val result = if (nullabilityForWarning) wrapEnhancement(enhancement) else enhancement

    return enhancedTag.constructType(enhancedArguments, enhancedNullability, attributes)
}

private data class EnhancementResult<out T>(val result: T, val enhancementAttribute: ConeAttribute<*>?)

private fun <T> T.noChange(): EnhancementResult<T> = EnhancementResult(this, null)
private fun <T> T.enhancedNullability(): EnhancementResult<T> = EnhancementResult(this, CompilerConeAttributes.EnhancedNullability)

private fun ConeKotlinType.getEnhancedNullability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition
): EnhancementResult<Boolean> {
    if (!position.shouldEnhance()) return this.isMarkedNullable.noChange()

    return when (qualifiers.nullability) {
        NullabilityQualifier.NULLABLE -> true.noChange()
        NullabilityQualifier.NOT_NULL -> false.enhancedNullability()
        else -> this.isMarkedNullable.noChange()
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

internal fun FirTypeRef.typeArguments(): List<FirTypeProjection> = when (this) {
    is FirUserTypeRef -> qualifier.lastOrNull()?.typeArgumentList?.typeArguments.orEmpty()
    is FirResolvedTypeRef -> type.typeArguments.map {
        when (it) {
            is ConeStarProjection -> buildStarProjection {}
            else -> {
                val kind = it.kind
                val type = when (it) {
                    is ConeKotlinTypeProjection -> it.type
                    is ConeKotlinType -> it
                    else -> error("Should not be here")
                }
                buildTypeProjectionWithVariance {
                    variance = when (kind) {
                        ProjectionKind.IN -> Variance.IN_VARIANCE
                        ProjectionKind.OUT -> Variance.OUT_VARIANCE
                        ProjectionKind.INVARIANT -> Variance.INVARIANT
                        else -> error("Should not be here")
                    }
                    typeRef = buildResolvedTypeRef {
                        this.type = type
                    }
                }
            }
        }
    }
    else -> emptyList()
}

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

internal fun List<FirAnnotationCall>.computeTypeAttributesForJavaType(): ConeAttributes =
    computeTypeAttributes { classId ->
        when (classId) {
            CompilerConeAttributes.EnhancedNullability.ANNOTATION_CLASS_ID -> add(CompilerConeAttributes.EnhancedNullability)
            in NOT_NULL_ANNOTATION_IDS -> add(CompilerConeAttributes.EnhancedNullability)
            JAVAX_NONNULL_ANNOTATION_ID,
            JAVAX_CHECKFORNULL_ANNOTATION_ID,
            COMPATQUAL_NONNULL_ANNOTATION_ID,
            ANDROIDX_RECENTLY_NON_NULL_ANNOTATION_ID
            -> add(CompilerConeAttributes.EnhancedNullability)
        }
    }
