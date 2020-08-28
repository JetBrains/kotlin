/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

sealed class TowerGroupKind(private val index: Int) : Comparable<TowerGroupKind> {
    abstract class WithDepth(index: Int, val depth: Int) : TowerGroupKind(index)

    object Start : TowerGroupKind(Integer.MIN_VALUE)

    object ClassifierPrioritized : TowerGroupKind(-10)

    object Qualifier : TowerGroupKind(0)

    object Classifier : TowerGroupKind(10)

    class TopPrioritized(depth: Int) : WithDepth(20, depth)

    object Member : TowerGroupKind(30)

    class Local(depth: Int) : WithDepth(40, depth)

    class ImplicitOrNonLocal(depth: Int, val kindForDebugSake: String) : WithDepth(50, depth)

    object InvokeExtension : TowerGroupKind(60)

    object QualifierValue : TowerGroupKind(70)

    object Last : TowerGroupKind(Integer.MAX_VALUE)

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
    private val kinds: Array<TowerGroupKind>,
    private val invokeResolvePriority: InvokeResolvePriority = InvokeResolvePriority.NONE
) : Comparable<TowerGroup> {
    companion object {
        private fun kindOf(kind: TowerGroupKind): TowerGroup = TowerGroup(arrayOf(kind))

        val EmptyRoot = TowerGroup(emptyArray())

        val Start = kindOf(TowerGroupKind.Start)

        val ClassifierPrioritized = kindOf(TowerGroupKind.ClassifierPrioritized)

        val Qualifier = kindOf(TowerGroupKind.Qualifier)

        val Classifier = kindOf(TowerGroupKind.Classifier)

        val QualifierValue = kindOf(TowerGroupKind.QualifierValue)

        val Member = kindOf(TowerGroupKind.Member)

        fun Local(depth: Int) = kindOf(TowerGroupKind.Local(depth))

        fun Implicit(depth: Int) = kindOf(TowerGroupKind.Implicit(depth))
        fun NonLocal(depth: Int) = kindOf(TowerGroupKind.NonLocal(depth))

        fun TopPrioritized(depth: Int) = kindOf(TowerGroupKind.TopPrioritized(depth))

        val Last = kindOf(TowerGroupKind.Last)
    }

    private fun kindOf(kind: TowerGroupKind): TowerGroup = TowerGroup(kinds + kind)

    val Member get() = kindOf(TowerGroupKind.Member)

    fun Local(depth: Int) = kindOf(TowerGroupKind.Local(depth))

    fun Implicit(depth: Int) = kindOf(TowerGroupKind.Implicit(depth))
    fun NonLocal(depth: Int) = kindOf(TowerGroupKind.NonLocal(depth))

    val InvokeExtension get() = kindOf(TowerGroupKind.InvokeExtension)

    fun TopPrioritized(depth: Int) = kindOf(TowerGroupKind.TopPrioritized(depth))

    // Treating `a.foo()` common calls as more prioritized than `a.foo.invoke()`
    // It's not the same as TowerGroupKind because it's not about tower levels, but rather a different dimension semantically.
    // It could be implemented via another TowerGroupKind, but it's not clear what priority should be assigned to the new TowerGroupKind
    fun InvokeResolvePriority(invokeResolvePriority: InvokeResolvePriority): TowerGroup {
        if (invokeResolvePriority == InvokeResolvePriority.NONE) return this
        return TowerGroup(kinds, invokeResolvePriority)
    }

    override fun compareTo(other: TowerGroup): Int {
        var index = 0
        while (index < kinds.size) {
            if (index >= other.kinds.size) return 1
            when {
                kinds[index] < other.kinds[index] -> return -1
                kinds[index] > other.kinds[index] -> return 1
            }
            index++
        }
        if (index < other.kinds.size) return -1

        return invokeResolvePriority.compareTo(other.invokeResolvePriority)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TowerGroup

        if (!kinds.contentEquals(other.kinds)) return false
        if (invokeResolvePriority != other.invokeResolvePriority) return false

        return true
    }

    override fun hashCode(): Int {
        var result = kinds.contentHashCode()
        result = 31 * result + invokeResolvePriority.hashCode()
        return result
    }

    override fun toString(): String {
        return "TowerGroup(kinds=${kinds.contentToString()}, invokeResolvePriority=$invokeResolvePriority)"
    }


}

enum class InvokeResolvePriority {
    NONE, INVOKE_RECEIVER, COMMON_INVOKE, INVOKE_EXTENSION;
}
