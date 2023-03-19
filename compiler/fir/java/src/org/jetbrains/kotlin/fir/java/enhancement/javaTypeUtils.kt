/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.name.ClassId

internal fun ConeKotlinType.enhance(session: FirSession, qualifiers: IndexedJavaTypeQualifiers): ConeKotlinType? =
    enhanceConeKotlinType(session, qualifiers, 0, mutableListOf<Int>().apply { computeSubtreeSizes(this) })

// The index in the lambda is the position of the type component in a depth-first walk of the tree.
// Example: A<B<C, D>, E<F>> - 0<1<2, 3>, 4<5>>. For flexible types, some arguments in the lower bound
// may be replaced with star projections in the upper bound, but otherwise corresponding arguments
// have the same index: (A<B<C>, D>..E<*, F>) -> (0<1<2>, 3>..0<1, 3>). This function precomputes
// the size of each subtree so that we can quickly skip to the next type argument; e.g. result[1] will
// give 3 for B<C, D>, indicating that E<F> is at 1 + 3 = 4.
private fun ConeKotlinType.computeSubtreeSizes(result: MutableList<Int>): Int {
    val index = result.size
    result.add(0) // reserve space at index
    result[index] = 1 + typeArguments.sumOf {
        // Star projections take up one (empty) entry.
        it.type?.computeSubtreeSizes(result) ?: 1.also { result.add(1) }
    }
    return result[index]
}

private fun ConeKotlinType.enhanceConeKotlinType(
    session: FirSession,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int,
    subtreeSizes: List<Int>
): ConeKotlinType? {
    return when (this) {
        is ConeFlexibleType -> {
            // Currently, the warnings are left unsupported in K2 (see KT-57307)
            // But modulo information for warnings, we reproduce the K1 behavior: if head type qualifier is for warnings, we totally ignore
            // enhancement on its arguments, too (see JavaTypeEnhancement.enhancePossiblyFlexible).
            // It's not totally correct, but tolerable since we would like to avoid excessive breaking changes and the warnings should be
            // anyway reported.
            // TODO: support not loosing information for warnings here, too
            if (qualifiers(index).isNullabilityQualifierForWarning) return null

            val lowerResult = lowerBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_LOWER, qualifiers, index, subtreeSizes
            )
            val upperResult = upperBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_UPPER, qualifiers, index, subtreeSizes
            )

            when {
                lowerResult == null && upperResult == null -> null
                this is ConeRawType -> ConeRawType.create(lowerResult ?: lowerBound, upperResult ?: upperBound)
                else -> coneFlexibleOrSimpleType(session.typeContext, lowerResult ?: lowerBound, upperResult ?: upperBound)
            }
        }
        is ConeSimpleKotlinType -> enhanceInflexibleType(
            session, TypeComponentPosition.INFLEXIBLE, qualifiers, index, subtreeSizes
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

private fun ConeSimpleKotlinType.enhanceInflexibleType(
    session: FirSession,
    position: TypeComponentPosition,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int,
    subtreeSizes: List<Int>,
    isFromDefinitelyNotNullType: Boolean = false,
): ConeSimpleKotlinType? {
    if (this is ConeDefinitelyNotNullType) {
        return original.enhanceInflexibleType(session, position, qualifiers, index, subtreeSizes, isFromDefinitelyNotNullType = true)
    }

    val shouldEnhance = position.shouldEnhance()
    if ((!shouldEnhance && typeArguments.isEmpty()) || this !is ConeLookupTagBasedType) {
        return null
    }

    val effectiveQualifiers = qualifiers(index)
    val enhancedTag = lookupTag.enhanceMutability(effectiveQualifiers, position)

    // TODO: implement warnings (see KT-57307)
    val nullabilityFromQualifiers = effectiveQualifiers.nullability
        .takeIf { shouldEnhance && !effectiveQualifiers.isNullabilityQualifierForWarning }
    val enhancedIsNullable = when (nullabilityFromQualifiers) {
        NullabilityQualifier.NULLABLE -> true
        NullabilityQualifier.NOT_NULL -> false
        else -> isNullable
    }

    var globalArgIndex = index + 1
    val enhancedArguments = typeArguments.map { arg ->
        val argIndex = globalArgIndex.also { globalArgIndex += subtreeSizes[it] }
        arg.type?.enhanceConeKotlinType(session, qualifiers, argIndex, subtreeSizes)?.let {
            when (arg.kind) {
                ProjectionKind.IN -> ConeKotlinTypeProjectionIn(it)
                ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(it)
                ProjectionKind.STAR -> ConeStarProjection
                ProjectionKind.INVARIANT -> it
            }
        }
    }

    val shouldAddAttribute = nullabilityFromQualifiers == NullabilityQualifier.NOT_NULL && !hasEnhancedNullability
    if (lookupTag == enhancedTag && enhancedIsNullable == isNullable && !shouldAddAttribute && enhancedArguments.all { it == null }) {
        return null // absolutely no changes
    }

    val mergedArguments = Array(typeArguments.size) { enhancedArguments[it] ?: typeArguments[it] }
    val mergedAttributes = if (shouldAddAttribute) attributes + CompilerConeAttributes.EnhancedNullability else attributes
    val enhancedType = enhancedTag.constructType(mergedArguments, enhancedIsNullable, mergedAttributes)
    return if (effectiveQualifiers.definitelyNotNull || (isFromDefinitelyNotNullType && nullabilityFromQualifiers == null))
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
                return readOnlyId.toLookupTag()
            }
        }
        MutabilityQualifier.MUTABLE -> {
            val mutableId = classId.readOnlyToMutable()
            if (position == TypeComponentPosition.FLEXIBLE_UPPER && mutableId != null) {
                return mutableId.toLookupTag()
            }
        }
        null -> {}
    }

    return this
}
