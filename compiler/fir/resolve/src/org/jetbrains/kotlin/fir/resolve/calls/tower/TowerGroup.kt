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

    // If a variable of extension function type belong to some scope X, and there's an implicit receiver Y, then its invoke candidate
    // should be less prioritized than the member scope of Y (see diagnostics/tests/resolve/priority/invokeExtensionVsOther2.kt),
    // but more prioritized than extensions in X with bound receiver of Y (see analysis-tests/testData/resolveWithStdlib/problems/invokePriority.kt).
    // That's why it's been places between Member and Local/ImplicitOrNonLocal.
    data object InvokeExtensionWithImplicitReceiver : TowerGroupKind(5)

    class Local(depth: Int) : WithDepth(6, depth)

    class ImplicitOrNonLocal(depth: Int, @Suppress("unused") val kindForDebugSake: String) : WithDepth(7, depth)

    class ContextReceiverGroup(depth: Int) : WithDepth(8, depth)

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
        val EmptyRootForInvokeReceiver = TowerGroup(0, EMPTY_KIND_ARRAY, InvokeResolvePriority.INVOKE_RECEIVER)

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

    val InvokeExtensionWithImplicitReceiver get() = kindOf(TowerGroupKind.InvokeExtensionWithImplicitReceiver)

    fun Local(depth: Int) = kindOf(TowerGroupKind.Local(depth))

    fun Implicit(depth: Int) = kindOf(TowerGroupKind.Implicit(depth))
    fun NonLocal(depth: Int) = kindOf(TowerGroupKind.NonLocal(depth))

    fun ContextReceiverGroup(depth: Int) = kindOf(TowerGroupKind.ContextReceiverGroup(depth))

    fun TopPrioritized(depth: Int) = kindOf(TowerGroupKind.TopPrioritized(depth))

    fun InvokeReceiver(
        receiverGroup: TowerGroup,
        invokeResolvePriority: InvokeResolvePriority
    ): TowerGroup {
        require(receiverGroup.invokeResolvePriority == InvokeResolvePriority.INVOKE_RECEIVER) {
            "Receivers for invoke should be resolved with INVOKE_RECEIVER, but ${receiverGroup.invokeResolvePriority} found"
        }

        require(invokeResolvePriority != InvokeResolvePriority.NONE && invokeResolvePriority != InvokeResolvePriority.INVOKE_RECEIVER) {
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
        // In case of `receiverGroup` presence, it means that candidate is invoke+variable or invokeExtension+variable.
        // In both cases `receiverGroup` means the tower group of a receiver variable.
        // For regular invoke+variable, this@debugKinds mean the tower level where the "invoke" function has been found.
        // For invokeExtension, this@debugKinds is either TowerGroup.Member in case of explicit extension receiver value,
        // like `explicitReceiver.myPropertyOfExtensionFunctionType()`
        // or TowerGroup.Implicit(depth).InvokeExtensionWithImplicitReceiver in case of implicit extension receiver value
        // like `with(myReceiver) { myPropertyOfExtensionFunctionType() }`
        val receiverDebugKinds = receiverGroup?.debugKinds ?: emptyArray()
        val otherReceiverDebugKinds = other.receiverGroup?.debugKinds ?: emptyArray()

        // Maximums define how far resolution algorithm should go to find the candidate
        val thisMax = maxOf(debugKinds, receiverDebugKinds, DEBUG_KINDS_COMPARATOR)
        val otherMax = maxOf(other.debugKinds, otherReceiverDebugKinds, DEBUG_KINDS_COMPARATOR)

        // If maximum tower groups are different, then just use more prioritized of maximums.
        // It seems more or less obvious, because if one candidate uses X group and other uses Y and X > Y,
        // then for the second candidate we wouldn't even need to continue resolution to the level of X, like we would already found
        // complete and probably successful candidate on Y.
        val maxResult = compareDebugKinds(thisMax, otherMax)
        if (maxResult != 0) return maxResult

        // If any of the candidate is not invoke/invokeExtension, choose it
        // Otherwise, prefer invoke and then invokeExtension ones
        val invokeKindPriority = invokeResolvePriority.compareTo(other.invokeResolvePriority)
        if (invokeKindPriority != 0) return invokeKindPriority

        // NB: thisMax == otherMax and kinds of invokes are the same
        return if (compareDebugKinds(receiverDebugKinds, thisMax) == 0 && compareDebugKinds(otherReceiverDebugKinds, thisMax) == 0) {
            // If both variables/receivers are obtained from the maximum current level, compare invoke/invokeExtension levels
            // Also, here it might be viewed as comparing minimums of both candidates
            compareDebugKinds(debugKinds, other.debugKinds)
        } else {
            // Otherwise prefer, the one with the closest variable/receiver
            // See how groups are prioritized at org.jetbrains.kotlin.resolve.calls.tower.AbstractInvokeTowerProcessor.process
            // Also, see the test testData/diagnostics/tests/resolve/invoke/closerVariableMatterMore.kt
            // There, we have two candidates A, B for which maxTowerGroup(A) == maxTowerGroup(B) && minTowerGroup(A) == minTowerGroup(B)
            // But we don't assume them as equally placed preferring one with the closest receiver.
            //
            // NB: receiverDebugKinds == otherReceiverDebugKinds => this == other
            // Proof:
            // - Let's assume receiverDebugKinds == otherReceiverDebugKinds
            // - We know that thisMax == otherMax
            // - If receiverDebugKinds == thisMax then otherReceiverDebugKinds == thisMax, so we wouldn't come to the `else` section
            // - That means that receiverDebugKinds != thisMax && otherReceiverDebugKinds != thisMax
            // - Thus we have thisMax == debugKinds (because thisMax = maxOf(debugKinds, receiverDebugKinds)
            // - And the same for otherMax == otherDebugKinds
            //
            // Considering thisMax == otherMax we have debugKinds == otherDebugKinds
            // And with initial condition of receiverDebugKinds == otherReceiverDebugKinds we've got candidates from completely the same level
            compareDebugKinds(receiverDebugKinds, otherReceiverDebugKinds)
        }
    }

    override fun compareTo(other: TowerGroup): Int = run {
        // See detailed algorithm description at `debugCompareTo`
        // Fast-path
        if (receiverGroup == null && other.receiverGroup == null &&
            invokeResolvePriority == InvokeResolvePriority.NONE && other.invokeResolvePriority == InvokeResolvePriority.NONE
        ) {
            return@run compareUnsigned(code, other.code)
        }

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
    /**
     * Looking for regular functions (or variables when the callee is a variable access)
     */
    NONE,

    /**
     * When resolving a function call, that kind of priority signifies looking for a variable candidates that might serve as a receivers
     * for "invoke" calls.
     *
     * Semantically, that kind of priority is redundant and using NONE works just fine, but having it, helps not to trigger computation of
     * property return type when we've already found some successful regular candidate.
     *
     * See testData/diagnostics/tests/resolve/invoke/errors/typeCheckerRanRecursive.kt as an example which would fail if we use NONE instead
     * of INVOKE_RECEIVER
     */
    INVOKE_RECEIVER,

    /**
     * Looking for "invoke()" function member or extension that would match for already found variable that would be used as a receiver
     */
    COMMON_INVOKE,

    /**
     * Resolving "invoke()" call when the found variable candidate has a type of extension function type, and we've got a matching extension
     * receiver (whether explicit or implicit one).
     *
     * It should be de-prioritized comparing with regular "invoke()" (COMMON_INVOKE).
     */
    INVOKE_EXTENSION
}
