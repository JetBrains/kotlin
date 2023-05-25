/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import java.lang.Long.toBinaryString
import java.lang.Long.compareUnsigned

sealed class TowerGroupKind(val index: Byte) : Comparable<TowerGroupKind> {
    abstract class WithDepth(index: Byte, val depth: Int) : TowerGroupKind(index) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as WithDepth

            if (index != other.index) return false
            if (depth != other.depth) return false

            return true
        }

        override fun hashCode(): Int {
            return 31 * depth + index
        }
    }

    data object Start : TowerGroupKind(0b0)

    data object Qualifier : TowerGroupKind(1)

    data object Classifier : TowerGroupKind(2)

    class TopPrioritized(depth: Int) : WithDepth(3, depth)

    data object Member : TowerGroupKind(4)

    class Local(depth: Int) : WithDepth(5, depth)

    class ImplicitOrNonLocal(depth: Int, val kindForDebugSake: String) : WithDepth(6, depth)

    class ContextReceiverGroup(depth: Int) : WithDepth(7, depth)

    data object InvokeExtensionWithImplicitReceiver : TowerGroupKind(8)

    data object QualifierValue : TowerGroupKind(9)

    class UnqualifiedEnum(depth: Int) : WithDepth(9, depth)

    data object Last : TowerGroupKind(0b1111)

    override fun compareTo(other: TowerGroupKind): Int {
        val indexResult = index.compareTo(other.index)
        if (indexResult != 0) return indexResult
        if (this is WithDepth && other is WithDepth) {
            return depth.compareTo(other.depth)
        }
        return 0
    }

    @Suppress("FunctionName")
    companion object {
        // These two groups intentionally have the same priority
        fun Implicit(depth: Int): TowerGroupKind = ImplicitOrNonLocal(depth, "Implicit")
        fun NonLocal(depth: Int): TowerGroupKind = ImplicitOrNonLocal(depth, "NonLocal")
    }
}

@Suppress("FunctionName", "unused", "PropertyName")
class TowerGroup
private constructor(
    private val code: Long,
    private val debugKinds: Array<TowerGroupKind>,
    private val invokeResolvePriority: InvokeResolvePriority = InvokeResolvePriority.NONE,
    private val receiverGroup: TowerGroup? = null
) : Comparable<TowerGroup> {
    companion object {

        private const val KIND_MASK = 0b1111
        private val KIND_SIZE_BITS: Int = Integer.bitCount(KIND_MASK)
        private const val DEPTH_MASK = 0xFFFF // 16bit
        private val DEPTH_SIZE_BITS: Int = Integer.bitCount(DEPTH_MASK)
        private const val USED_BITS_MASK: Long = 0b111111 // max size 64 bits
        private const val TOTAL_BITS = 64
        private val USABLE_BITS = java.lang.Long.numberOfLeadingZeros(USED_BITS_MASK)

        private val EMPTY_KIND_ARRAY = emptyArray<TowerGroupKind>()

        private const val DEBUG = false // enables tower group debugging

        private fun appendDebugKind(kinds: Array<TowerGroupKind>, kind: TowerGroupKind): Array<TowerGroupKind> {
            return if (DEBUG) {
                kinds + kind
            } else {
                EMPTY_KIND_ARRAY
            }
        }

        private fun debugKindArrayOf(kind: TowerGroupKind): Array<TowerGroupKind> {
            return if (DEBUG) {
                arrayOf(kind)
            } else {
                EMPTY_KIND_ARRAY
            }
        }

        /*
            K - bits of index
            D - bits of depth
            U - bits of used bits count
            TowerGroupKind(K): KKKK.....UUUUUU
            WithDepth(K, D):   KKKKDDDDDDDDDD.....UUUUUU

            Subscript operation:
            KKKK....000100
            KKKKKKKK....001000

            Start.Start > Start
            00000000...001000 > 0000...000100
         */
        private fun subscript(code: Long, kind: TowerGroupKind): Long {
            val usedBits = (code and USED_BITS_MASK).toInt()
            return when (kind) {
                is TowerGroupKind.WithDepth -> {
                    val kindUsedBits = usedBits + KIND_SIZE_BITS
                    val depthUsedBits = kindUsedBits + DEPTH_SIZE_BITS
                    require(kind.depth <= DEPTH_MASK) {
                        "Depth overflow: requested: ${kind.depth}, allowed: $DEPTH_MASK"
                    }
                    require(depthUsedBits <= USABLE_BITS) {
                        "BitGroup overflow: newUsedBits: $depthUsedBits, original: ${toBinaryString(code)}, usedBits: $usedBits"
                    }

                    (code or kind.index.toLong().shl(TOTAL_BITS - kindUsedBits)
                            or kind.depth.toLong().shl(TOTAL_BITS - depthUsedBits)
                            or depthUsedBits.toLong())
                }
                else -> {
                    val newUsedBits = usedBits + KIND_SIZE_BITS

                    require(newUsedBits <= USABLE_BITS)
                    code or kind.index.toLong().shl(TOTAL_BITS - newUsedBits) or newUsedBits.toLong()
                }
            }
        }

        private fun compareDebugKinds(aDebugKinds: Array<TowerGroupKind>, bDebugKinds: Array<TowerGroupKind>): Int {
            var index = 0
            while (index < aDebugKinds.size) {
                if (index >= bDebugKinds.size) return 1
                when {
                    aDebugKinds[index] < bDebugKinds[index] -> return -1
                    aDebugKinds[index] > bDebugKinds[index] -> return 1
                }
                index++
            }
            if (index < bDebugKinds.size) return -1

            return 0
        }

        val DEBUG_KINDS_COMPARATOR: Comparator<Array<TowerGroupKind>> = Comparator(::compareDebugKinds)

        private fun kindOf(kind: TowerGroupKind): TowerGroup {
            return TowerGroup(subscript(0, kind), debugKindArrayOf(kind))
        }

        val EmptyRoot = TowerGroup(0, EMPTY_KIND_ARRAY)

        val Start = kindOf(TowerGroupKind.Start)

        val Qualifier = kindOf(TowerGroupKind.Qualifier)

        val Classifier = kindOf(TowerGroupKind.Classifier)

        val QualifierValue = kindOf(TowerGroupKind.QualifierValue)

        val Member = kindOf(TowerGroupKind.Member)

        fun UnqualifiedEnum(depth: Int) = kindOf(TowerGroupKind.UnqualifiedEnum(depth))

        fun Local(depth: Int) = kindOf(TowerGroupKind.Local(depth))

        fun Implicit(depth: Int) = kindOf(TowerGroupKind.Implicit(depth))
        fun NonLocal(depth: Int) = kindOf(TowerGroupKind.NonLocal(depth))

        fun ContextReceiverGroup(depth: Int) = kindOf(TowerGroupKind.ContextReceiverGroup(depth))

        fun TopPrioritized(depth: Int) = kindOf(TowerGroupKind.TopPrioritized(depth))

        val Last = kindOf(TowerGroupKind.Last)
    }

    private fun kindOf(kind: TowerGroupKind): TowerGroup = TowerGroup(subscript(code, kind), appendDebugKind(debugKinds, kind))

    val Member get() = kindOf(TowerGroupKind.Member)

    fun Local(depth: Int) = kindOf(TowerGroupKind.Local(depth))

    fun Implicit(depth: Int) = kindOf(TowerGroupKind.Implicit(depth))
    fun NonLocal(depth: Int) = kindOf(TowerGroupKind.NonLocal(depth))

    fun ContextReceiverGroup(depth: Int) = kindOf(TowerGroupKind.ContextReceiverGroup(depth))

    val InvokeExtensionWithImplicitReceiver get() = kindOf(TowerGroupKind.InvokeExtensionWithImplicitReceiver)

    fun TopPrioritized(depth: Int) = kindOf(TowerGroupKind.TopPrioritized(depth))

    fun InvokeReceiver(
        receiverGroup: TowerGroup,
        invokeResolvePriority: InvokeResolvePriority
    ): TowerGroup {
        require(invokeResolvePriority != InvokeResolvePriority.NONE) {
            "invokeResolvePriority should be non-trivial when receiverGroup is specified"
        }

        require(receiverGroup.receiverGroup == null) {
            "receiverGroup should be trivial, but ${receiverGroup.receiverGroup} was found"
        }

        return TowerGroup(code, debugKinds, invokeResolvePriority, receiverGroup)
    }

    // Treating `a.foo()` common calls as more prioritized than `a.foo.invoke()`
    // It's not the same as TowerGroupKind because it's not about tower levels, but rather a different dimension semantically.
    // It could be implemented via another TowerGroupKind, but it's not clear what priority should be assigned to the new TowerGroupKind
    fun InvokeResolvePriority(invokeResolvePriority: InvokeResolvePriority): TowerGroup {
        if (invokeResolvePriority == InvokeResolvePriority.NONE) return this
        return TowerGroup(code, debugKinds, invokeResolvePriority)
    }

    private fun debugCompareTo(other: TowerGroup): Int {
        val receiverDebugKinds = receiverGroup?.debugKinds ?: emptyArray()
        val otherReceiverDebugKinds = other.receiverGroup?.debugKinds ?: emptyArray()

        val thisMax = maxOf(debugKinds, receiverDebugKinds, DEBUG_KINDS_COMPARATOR)
        val otherMax = maxOf(other.debugKinds, otherReceiverDebugKinds, DEBUG_KINDS_COMPARATOR)

        val maxResult = compareDebugKinds(thisMax, otherMax)
        if (maxResult != 0) return maxResult

        val invokeKindPriority = invokeResolvePriority.compareTo(other.invokeResolvePriority)
        if (invokeKindPriority != 0) return invokeKindPriority

        // thisMax == otherMax
        return if (compareDebugKinds(receiverDebugKinds, thisMax) == 0 && compareDebugKinds(otherReceiverDebugKinds, thisMax) == 0) {
            compareDebugKinds(debugKinds, other.debugKinds)
        } else {
            compareDebugKinds(receiverDebugKinds, otherReceiverDebugKinds)
        }
    }

    override fun compareTo(other: TowerGroup): Int = run {
        // Fast-path
        if (receiverGroup == null && other.receiverGroup == null) return@run compareUnsigned(code, other.code)
        val receiverCode = receiverGroup?.code ?: 0 // TowerGroup.Start.code
        val otherReceiverCode = other.receiverGroup?.code ?: 0

        val thisMax: Long = if (compareUnsigned(code, receiverCode) >= 0) code else receiverCode
        val otherMax: Long = if (compareUnsigned(other.code, otherReceiverCode) >= 0) other.code else otherReceiverCode

        val resultMax = compareUnsigned(thisMax, otherMax)
        if (resultMax != 0) return@run resultMax

        val invokeKindPriority = invokeResolvePriority.compareTo(other.invokeResolvePriority)
        if (invokeKindPriority != 0) return invokeKindPriority

        return if (compareUnsigned(receiverCode, thisMax) == 0 && compareUnsigned(otherReceiverCode, thisMax) == 0) {
            compareUnsigned(code, other.code)
        } else {
            compareUnsigned(receiverCode, otherReceiverCode)
        }
    }.also {
        if (DEBUG) {
            val debugResult = debugCompareTo(other)
            require(debugResult == it) {
                "Kind comparison incorrect: $this vs $other, expected: $it, $debugResult"
            }
            require((this == other) == (debugResult == 0)) {
                "Equality and compareTo should work consistently, but $debugResult found"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TowerGroup

        if (code != other.code) return false
        if (DEBUG) require(this.debugKinds.contentEquals(other.debugKinds)) { "Equals inconsistent: $this vs $other" }
        if (invokeResolvePriority != other.invokeResolvePriority) return false
        if (receiverGroup != null && other.receiverGroup != null) {
            if (receiverGroup != other.receiverGroup) return false
        }

        return true
    }

    override fun toString(): String {
        return "TowerGroup(code=${toBinaryString(code)}, debugKinds=${debugKinds.contentToString()}, invokeResolvePriority=$invokeResolvePriority)"
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + invokeResolvePriority.hashCode()
        return result
    }


}

enum class InvokeResolvePriority {
    NONE, COMMON_INVOKE, INVOKE_EXTENSION;
}
