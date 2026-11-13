/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import kotlin.jvm.JvmName

public typealias SyntaxId = Int

public const val NO_ID: SyntaxId = 0

/**
 * An immutable set of [SyntaxId] backed by a bit set that provides fast and allocation-free [contains] checks.
 *
 * Analogue of `com.intellij.platform.syntax.SyntaxElementTypeSet` and `com.intellij.psi.tree.TokenSet`
 * but without dependency on specific nodes (`com.intellij.psi.tree.IElementType`, `com.intellij.platform.syntax.SyntaxElementType`)
 *
 * Use [syntaxIdSetOf] or [asSyntaxIdSet] to create an instance.
 * To avoid boxing, declare variables with the [SyntaxIdSet] type rather than [Set].
 */
public class SyntaxIdSet private constructor(
    /** Distinct IDs in insertion order. */
    private val ids: IntArray,
    /**
     * Bit set of [ids] where the bit `id % 64` of the word `id / 64` is set for every ID.
     *
     * Its size is `maxId / 64 + 1`, so it's determined by the max ID in the set rather than by the number of IDs.
     * Currently, the max syntax ID is 293 ([KDocSyntaxNodeTypeIds.KDOC_NAME]), so the array has at most 5 words (40 bytes).
     */
    private val words: LongArray,
) : Set<SyntaxId> {
    @JvmName("containsNullable")
    public operator fun contains(element: SyntaxId?): Boolean = element != null && contains(element)

    override fun contains(element: SyntaxId): Boolean {
        if (element <= NO_ID) return false
        val wordIndex = element shr WORD_INDEX_SHIFT // equivalent to `element / 64`
        // `shl` uses only the lowest 6 bits of `element`, i.e., the bit index inside the word.
        return wordIndex < words.size && (words[wordIndex] and (1L shl element)) != 0L
    }

    override fun containsAll(elements: Collection<SyntaxId>): Boolean =
        if (elements is SyntaxIdSet) elements.ids.all { contains(it) } else elements.all { contains(it) }

    override fun isEmpty(): Boolean = ids.isEmpty()

    override fun iterator(): Iterator<SyntaxId> = ids.iterator()

    override val size: Int
        get() = ids.size

    public operator fun plus(other: Iterable<SyntaxId>): SyntaxIdSet {
        if (other.all { contains(it) }) return this
        return create(ids + other.toList())
    }

    public operator fun plus(other: SyntaxId): SyntaxIdSet {
        if (contains(other)) return this
        return create(ids + other)
    }

    public operator fun minus(other: Iterable<SyntaxId>): SyntaxIdSet {
        if (other.none { contains(it) }) return this
        val otherSet = other.asSyntaxIdSet()
        return create(ids.filter { it !in otherSet }.toIntArray())
    }

    public operator fun minus(other: SyntaxId): SyntaxIdSet {
        if (!contains(other)) return this
        return create(ids.filter { it != other }.toIntArray())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Set<*> || other.size != size) return false
        if (other is SyntaxIdSet) return ids.all { it in other }
        return other.all { it is SyntaxId && contains(it) }
    }

    override fun hashCode(): Int = ids.sum()

    override fun toString(): String = ids.joinToString(prefix = "[", postfix = "]")

    internal companion object {
        internal val EMPTY: SyntaxIdSet = SyntaxIdSet(IntArray(0), LongArray(0))

        /**
         * Creates a set from [ids] that may contain duplicates.
         * IDs must be positive: [NO_ID] is not a real ID and it is rejected.
         */
        internal fun create(ids: IntArray): SyntaxIdSet {
            if (ids.isEmpty()) return EMPTY

            val distinctIds = ids.distinct().toIntArray()
            val min = distinctIds.min()
            require(min > NO_ID) { "Syntax ID must be positive, but was $min" }

            val words = LongArray((distinctIds.max() shr WORD_INDEX_SHIFT) + 1)
            for (id in distinctIds) {
                val wordIndex = id shr WORD_INDEX_SHIFT
                words[wordIndex] = words[wordIndex] or (1L shl id)
            }
            return SyntaxIdSet(distinctIds, words)
        }
    }
}

/**
 * `log2(Long.SIZE_BITS)`: the bits of [SyntaxIdSet] are stored in 64-bit words, so the word index of an ID is `id / 64`,
 * computed as `id shr WORD_INDEX_SHIFT`.
 *
 * The shift is a bit cheaper than the division: for a signed `Int`, `/ 64` needs an extra correction for negative values,
 * while IDs here are always positive (non-positive ones are filtered out before).
 */
private const val WORD_INDEX_SHIFT: Int = 6

public val emptySyntaxIdSet: SyntaxIdSet
    get() = SyntaxIdSet.EMPTY

public fun syntaxIdSetOf(vararg ids: SyntaxId): SyntaxIdSet = SyntaxIdSet.create(ids)

public fun Iterable<SyntaxId>.asSyntaxIdSet(): SyntaxIdSet {
    if (this is SyntaxIdSet) return this
    return SyntaxIdSet.create(toList().toIntArray())
}
