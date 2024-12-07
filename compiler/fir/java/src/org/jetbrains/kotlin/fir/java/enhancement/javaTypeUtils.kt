/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal fun ConeKotlinType.enhance(session: FirSession, qualifiers: IndexedJavaTypeQualifiers): ConeKotlinType? =
    enhanceConeKotlinType(session, qualifiers, 0, mutableListOf<Int>().apply { computeSubtreeSizes(this) }, convertErrorsToWarnings = false)

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
    subtreeSizes: List<Int>,
    convertErrorsToWarnings: Boolean,
): ConeKotlinType? {
    return when (this) {
        is ConeFlexibleType -> {
            val lowerResult = lowerBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_LOWER, qualifiers, index, subtreeSizes,
                isFromDefinitelyNotNullType = false, convertErrorToWarning = convertErrorsToWarnings,
            )
            val upperResult = upperBound.enhanceInflexibleType(
                session, TypeComponentPosition.FLEXIBLE_UPPER, qualifiers, index, subtreeSizes,
                isFromDefinitelyNotNullType = false, convertErrorToWarning = convertErrorsToWarnings,
            )

            when {
                lowerResult == null && upperResult == null -> null
                this is ConeRawType -> ConeRawType.create(lowerResult ?: lowerBound, upperResult ?: upperBound)
                else -> coneFlexibleOrSimpleType(session.typeContext, lowerResult ?: lowerBound, upperResult ?: upperBound).let {
                    it.applyIf(it !is ConeFlexibleType) {
                        it.withAttributes(it.attributes.add(CompilerConeAttributes.EnhancedNullability))
                    }
                }
            }
        }
        is ConeSimpleKotlinType -> enhanceInflexibleType(
            session, TypeComponentPosition.INFLEXIBLE, qualifiers, index, subtreeSizes,
            isFromDefinitelyNotNullType = false, convertErrorToWarning = convertErrorsToWarnings,
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

private fun ConeRigidType.enhanceInflexibleType(
    session: FirSession,
    position: TypeComponentPosition,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int,
    subtreeSizes: List<Int>,
    isFromDefinitelyNotNullType: Boolean,
    convertErrorToWarning: Boolean,
): ConeRigidType? {
    if (this is ConeDefinitelyNotNullType) {
        return original.enhanceInflexibleType(session, position, qualifiers, index, subtreeSizes, isFromDefinitelyNotNullType = true, convertErrorToWarning)
    }

    val shouldEnhance = position.shouldEnhance()
    if ((!shouldEnhance && typeArguments.isEmpty()) || this !is ConeLookupTagBasedType) {
        return null
    }

    val effectiveQualifiers = qualifiers(index)
    val enhancedTag = lookupTag.enhanceMutability(effectiveQualifiers, position)

    val nullabilityFromQualifiers = effectiveQualifiers.nullability.takeIf { shouldEnhance }

    val enhanced = enhanceInflexibleType(
        session,
        qualifiers,
        index,
        subtreeSizes,
        isFromDefinitelyNotNullType,
        effectiveQualifiers.definitelyNotNull,
        nullabilityFromQualifiers,
        enhancedTag,
        convertNestedErrorsToWarnings = convertErrorToWarning ||
                effectiveQualifiers.isNullabilityQualifierForWarning &&
                !session.languageVersionSettings.supportsFeature(LanguageFeature.SupportJavaErrorEnhancementOfArgumentsOfWarningLevelEnhanced),
    )

    return if (enhanced != null && (effectiveQualifiers.isNullabilityQualifierForWarning || convertErrorToWarning)) {
        val newAttributes = attributes.add(
            EnhancedTypeForWarningAttribute(
                enhanced.enhancedTypeForWarningOrSelf,
                isDeprecation = convertErrorToWarning && effectiveQualifiers.enhancesSomethingForError()
            )
        )

        if (enhancedTag != lookupTag) {
            // Handle case when mutability was enhanced and nullability was enhanced for warning.
            enhancedTag.constructType(enhanced.typeArguments, isMarkedNullable, newAttributes)
        } else {
            this.withAttributes(newAttributes).withArguments(enhanced.typeArguments)
        }.applyIf(isFromDefinitelyNotNullType) {
            // If the original type was DNN, we need to wrap the result in a DNN type because `this` is the non-DNN part of the original.
            // In the non-warning case, this happens in the nested call and so `enhanced` is already DNN.
            ConeDefinitelyNotNullType.create(this, session.typeContext)
        }
    } else {
        // It's possible for enhanced (for warning) types to be enhanced again, one instance is when in
        // `JavaClassUseSiteMemberScope.processOverridesForFunctionsWithErasedValueParameter`,
        // `relevantFunctionFromSupertypes` is a substitution override.
        // In this case, the non-enhanced Java method gets a value parameter with an enhanced type.
        // In a situation where this previously enhanced for warning type gets enhanced for error (because of an override, e.g., from a
        // `@PurelyImplements` supertype), we need to remove the enhanced for warning attribute, otherwise we end up with a contradicting,
        // non-flexible(!) type like `EFW(String?) String`.
        enhanced?.withAttributes(enhanced.attributes.remove(EnhancedTypeForWarningAttribute::class))
    }
}

private fun JavaTypeQualifiers.enhancesSomethingForError(): Boolean {
    return !isNullabilityQualifierForWarning && (nullability != null || mutability != null || definitelyNotNull)
}

private fun ConeLookupTagBasedType.enhanceInflexibleType(
    session: FirSession,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int,
    subtreeSizes: List<Int>,
    isFromDefinitelyNotNullType: Boolean,
    isDefinitelyNotNull: Boolean,
    nullabilityFromQualifiers: NullabilityQualifier?,
    enhancedTag: ConeClassifierLookupTag,
    convertNestedErrorsToWarnings: Boolean,
): ConeRigidType? {
    val enhancedIsNullable = when (nullabilityFromQualifiers) {
        NullabilityQualifier.NULLABLE -> true
        NullabilityQualifier.NOT_NULL -> false
        else -> isMarkedNullable
    }

    var globalArgIndex = index + 1
    val enhancedArguments = typeArguments.mapIndexed { currentArgLocalIndex, arg ->
        val currentArgGlobalIndex = globalArgIndex.also { globalArgIndex += subtreeSizes[it] }
        if (arg.type == null && qualifiers(currentArgGlobalIndex).nullability == NullabilityQualifier.FORCE_FLEXIBILITY) {
            // Given `C<T extends @Nullable V>`, unannotated `C<?>` is `C<out (V..V?)>`.
            val typeParameters = this.lookupTag.toClassLikeSymbol(session)?.fir?.typeParameters
            if (typeParameters != null) {
                val bound = typeParameters[currentArgLocalIndex].symbol.fir.bounds.first().coneType
                return@mapIndexed ConeKotlinTypeProjectionOut(
                    ConeFlexibleType(
                        bound.lowerBoundIfFlexible().withNullability(nullable = false, session.typeContext),
                        bound.upperBoundIfFlexible().withNullability(nullable = true, session.typeContext)
                    )
                )
            }
        }
        arg.type?.enhanceConeKotlinType(
            session, qualifiers, currentArgGlobalIndex, subtreeSizes, convertErrorsToWarnings = convertNestedErrorsToWarnings
        )?.let {
            when (arg.kind) {
                ProjectionKind.IN -> ConeKotlinTypeProjectionIn(it)
                ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(it)
                ProjectionKind.STAR -> ConeStarProjection
                ProjectionKind.INVARIANT -> it
            }
        }
    }

    val shouldAddAttribute = nullabilityFromQualifiers == NullabilityQualifier.NOT_NULL && !hasEnhancedNullability
    if (lookupTag == enhancedTag && enhancedIsNullable == isMarkedNullable && !shouldAddAttribute && enhancedArguments.all { it == null }) {
        return null // absolutely no changes
    }

    val mergedArguments = Array(typeArguments.size) { enhancedArguments[it] ?: typeArguments[it] }
    val mergedAttributes = if (shouldAddAttribute) attributes.add(CompilerConeAttributes.EnhancedNullability) else attributes
    val enhancedType = enhancedTag.constructType(mergedArguments, enhancedIsNullable, mergedAttributes)
    return if (isDefinitelyNotNull || (isFromDefinitelyNotNullType && nullabilityFromQualifiers == null))
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
