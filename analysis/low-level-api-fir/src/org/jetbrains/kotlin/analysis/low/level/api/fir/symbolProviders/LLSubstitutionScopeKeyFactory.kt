/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.ConeSubstitutionScopeKey
import org.jetbrains.kotlin.fir.scopes.SubstitutionScopeKeyFactory
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.typeAnnotations

/**
 * An LL-specific [SubstitutionScopeKeyFactory] that includes type annotation lookup tags in the substitution scope cache key.
 *
 * [ConeLookupTagBasedType][org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType] equality intentionally ignores type annotations
 * (the compiler treats `Foo<String>` and `Foo<@Anno String>` as equivalent for almost all purposes). The default
 * [SubstitutionScopeKeyFactory.Default] therefore produces the same key for both, causing the scope cache in `scopeForClassImpl` to return
 * a stale scope when a declaration is first analyzed through one dispatch receiver and then through another with different type annotations.
 * This factory extends the key with the annotation lookup tags and their positions in the type argument tree to prevent such cache hits.
 *
 * Known limitation: Annotation arguments are not included in the key because they are only resolved at the
 * [ANNOTATION_ARGUMENTS][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.ANNOTATION_ARGUMENTS] phase. Scope lookups happen
 * earlier, so argument values are unavailable.
 */
@LLFirInternals
class LLSubstitutionScopeKeyFactory(private val session: LLFirSession) : SubstitutionScopeKeyFactory {
    override fun createKey(
        substitutor: ConeSubstitutor,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag,
        memberOwnerLookupTag: ConeClassLikeLookupTag,
        memberOwnerClass: FirClassSymbol<*>?,
        isFromExpectClass: Boolean
    ): ConeSubstitutionScopeKey {
        return LLConeSubstitutionScopeKey(
            dispatchReceiverLookupTag,
            isFromExpectClass,
            substitutor,
            memberOwnerLookupTag,
            computePlacedAnnotations(substitutor, memberOwnerLookupTag, memberOwnerClass),
        )
    }

    private data class LLConeSubstitutionScopeKey(
        override val lookupTag: ConeClassLikeLookupTag,
        override val isFromExpectClass: Boolean,
        override val substitutor: ConeSubstitutor,
        override val derivedClassLookupTag: ConeClassLikeLookupTag?,
        val derivedTypeAnnotations: List<TypeArgumentAnnotation>,
    ) : ConeSubstitutionScopeKey()

    /**
     * Computes the annotations reachable from the substituted type arguments of [memberOwnerLookupTag], along with their positions in the
     * type argument tree. Returns an empty list quickly when no annotations are present.
     */
    private fun computePlacedAnnotations(
        substitutor: ConeSubstitutor,
        memberOwnerLookupTag: ConeClassLikeLookupTag,
        memberOwnerClass: FirClassSymbol<*>?,
    ): List<TypeArgumentAnnotation> {
        val memberOwnerClass = memberOwnerClass
            ?: memberOwnerLookupTag.toClassSymbol(session)
            ?: return emptyList()

        if (memberOwnerClass.typeParameterSymbols.isEmpty()) {
            return emptyList()
        }

        val substitutedTypes = memberOwnerClass.typeParameterSymbols
            .map { substitutor.substituteOrNull(it.toConeType()) }

        return collectPlacedAnnotations(substitutedTypes)
    }

    fun collectPlacedAnnotations(types: List<ConeKotlinType?>): List<TypeArgumentAnnotation> {
        if (types.none { it != null && isAnnotated(it) }) {
            return emptyList()
        }

        val result = ArrayList<TypeArgumentAnnotation>()
        val stack = Stack()
        for ((index, type) in types.withIndex()) {
            if (type == null) {
                continue
            }

            stack.push(index)
            try {
                recordAnnotations(type, stack, result)
            } finally {
                stack.pop(index)
            }
        }

        assert(result.isNotEmpty()) { "Result is inconsistent with 'isAnnotated()'" }
        return result
    }

    /**
     * Fast pre-check: returns `true` if [type] or any of its nested type arguments carry at least one annotation.
     * Used to avoid allocations in [collectPlacedAnnotations] when the common case of no annotations applies
     * (type annotations are very rare).
     */
    private fun isAnnotated(type: ConeKotlinType): Boolean {
        if (type.typeAnnotations.isNotEmpty()) {
            return true
        }

        for (typeProjection in type.typeArguments) {
            val type = typeProjection.type ?: continue
            if (isAnnotated(type)) {
                return true
            }
        }

        return false
    }

    /**
     * Recursively walks [type] and its nested type arguments, appending a [TypeArgumentAnnotation] to [consumer] for each annotation
     * found. The [stack] encodes the current path from the top-level substituted type argument down to [type].
     *
     * As order of type traversal is stable, there is no need in sorting the [consumer] afterward.
     */
    private fun recordAnnotations(type: ConeKotlinType, stack: Stack, consumer: MutableList<TypeArgumentAnnotation>) {
        for (typeAnnotation in type.typeAnnotations) {
            val lookupTag = typeAnnotation.annotationTypeRef.coneType.classLikeLookupTagIfAny
            consumer += stack.createAnnotation(lookupTag)
        }

        for ((index, typeArgument) in type.typeArguments.withIndex()) {
            val type = typeArgument.type ?: continue

            stack.push(index)
            try {
                recordAnnotations(type, stack, consumer)
            } finally {
                stack.pop(index)
            }
        }
    }

    /**
     * Tracks the current position path (as a sequence of type-argument indices) during the recursive type tree traversal.
     * [createAnnotation] snapshots the current path into a [TypeArgumentAnnotation].
     */
    private class Stack {
        private var stack = IntArray(8)
        private var size = 0

        fun push(index: Int) {
            if (stack.size == size) {
                stack = stack.copyOf(stack.size * 2)
            }
            stack[size] = index
            size += 1
        }

        fun pop(index: Int) {
            assert(size > 0 && stack[size - 1] == index)
            stack[size - 1] = -1
            size -= 1
        }

        fun createAnnotation(lookupTag: ConeClassLikeLookupTag?): TypeArgumentAnnotation {
            if (size <= ShallowTypeArgumentAnnotation.CAPACITY) {
                val a = if (size >= 1) stack[0] else -1
                val b = if (size >= 2) stack[1] else -1
                val c = if (size >= 3) stack[2] else -1
                return ShallowTypeArgumentAnnotation(lookupTag, a, b, c)
            } else {
                val path = stack.copyOf(size)
                return DeepTypeArgumentAnnotation(lookupTag, path)
            }
        }
    }

    /**
     * An annotation found at a specific position in the substituted type argument tree.
     *
     * The position is the path of type-argument indices from the top-level substituted type argument down to the annotated type
     * (e.g., path `[1, 0]` means the first nested type argument of the second top-level type argument).
     */
    interface TypeArgumentAnnotation {
        val lookupTag: ConeClassLikeLookupTag?
    }

    /**
     * An annotation at a path of depth < 4. Stores the path as three [Int] fields ([a], [b], [c]) to avoid an [IntArray] allocation.
     * Unused tail slots are set to `-1`.
     *
     * Capacity=3 is chosen for pragmatic reasons (deep generics are uncommon), and because this way the object layout will be
     * 32 bytes for all reference compression modes (12B header, 4B * 3 ints, 4B/8B for a lookup tag reference).
     */
    private data class ShallowTypeArgumentAnnotation(
        override val lookupTag: ConeClassLikeLookupTag?,
        private val a: Int,
        private val b: Int,
        private val c: Int
    ) : TypeArgumentAnnotation {
        companion object {
            const val CAPACITY = 3
        }

        override fun toString(): String {
            return "SmallStack[$a, $b, $c] for $lookupTag"
        }
    }

    /** An annotation at a path of depth >= 4. Stores the full path as an [IntArray]. */
    private class DeepTypeArgumentAnnotation(
        override val lookupTag: ConeClassLikeLookupTag?,
        private val path: IntArray,
    ) : TypeArgumentAnnotation {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DeepTypeArgumentAnnotation) return false
            return lookupTag == other.lookupTag && path.contentEquals(other.path)
        }

        private val cachedHashCode = 31 * lookupTag.hashCode() + path.contentHashCode()
        override fun hashCode(): Int = cachedHashCode

        override fun toString(): String {
            return "LargeStack" + path.contentToString() + " for $lookupTag"
        }
    }
}
