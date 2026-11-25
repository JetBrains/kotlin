/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import java.util.BitSet

/**
 * An immutable wrapper around [java.util.BitSet] for efficient syntax ID management (fast `contains` operations).
 *
 * Analogue of `com.intellij.platform.syntax.SyntaxElementTypeSet` and `com.intellij.psi.tree.TokenSet`
 * but without dependency on specific nodes.
 *
 * Currently, the max [org.jetbrains.kotlin.syntax.SyntaxId] is < 300 ([KDocSyntaxNodeTypeIds.KDOC_NAME]),
 * so the underlying BitSet array has at most 5 words (40 bytes).
 *
 * [BasicSyntaxElementTypeIds.NO_ID] denotes the absence of an ID rather than a specific element type, so it can't be added
 * to the set, and [contains] always returns `false` for it.
 *
 * Iteration is not intended for hot paths: the set is boxed when used as an [Iterable],
 * and each ID is boxed by the [Iterator]. Use [contains] for fast checks.
 */
@JvmInline
value class SyntaxIdSet private constructor(private val bits: BitSet) : Iterable<SyntaxId> {
    operator fun contains(id: SyntaxId): Boolean =
        id > BasicSyntaxElementTypeIds.NO_ID && bits.get(id)

    operator fun plus(id: SyntaxId): SyntaxIdSet {
        require(id > BasicSyntaxElementTypeIds.NO_ID)
        if (id in this) return this

        val copy = bits.clone() as BitSet
        copy.set(id)
        return SyntaxIdSet(copy)
    }

    override fun iterator(): Iterator<SyntaxId> = bits.stream().iterator()

    companion object {
        fun of(vararg ids: SyntaxId): SyntaxIdSet {
            val bits = BitSet()
            for (id in ids) {
                require(id > BasicSyntaxElementTypeIds.NO_ID)
                bits.set(id)
            }
            return SyntaxIdSet(bits)
        }
    }
}
