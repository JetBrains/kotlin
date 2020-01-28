/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

sealed class TowerGroupKind(private val index: Int) : Comparable<TowerGroupKind> {
    abstract class WithDepth(index: Int, val depth: Int) : TowerGroupKind(index)

    object Start : TowerGroupKind(Integer.MIN_VALUE)

    class Weakened(depth: Int) : WithDepth(-10, depth)

    class Qualifier(depth: Int) : WithDepth(0, depth)

    class TopPrioritized(depth: Int) : WithDepth(1, depth)

    object Member : TowerGroupKind(2)

    class Local(depth: Int) : WithDepth(3, depth)

    class Implicit(depth: Int) : WithDepth(4, depth)

    object InvokeExtension : TowerGroupKind(5)

    class Top(depth: Int) : WithDepth(6, depth)

    class Static(depth: Int) : WithDepth(7, depth)

    object Last : TowerGroupKind(Integer.MAX_VALUE)

    override fun compareTo(other: TowerGroupKind): Int {
        val indexResult = index.compareTo(other.index)
        if (indexResult != 0) return indexResult
        if (this is WithDepth && other is WithDepth) {
            return depth.compareTo(other.depth)
        }
        return 0
    }
}

@Suppress("FunctionName", "unused", "PropertyName")
class TowerGroup private constructor(private val list: List<TowerGroupKind>) : Comparable<TowerGroup> {
    companion object {
        private fun kindOf(kind: TowerGroupKind): TowerGroup = TowerGroup(listOf(kind))

        val Start = kindOf(TowerGroupKind.Start)

        fun Qualifier(depth: Int) = kindOf(TowerGroupKind.Qualifier(depth))

        val Member = kindOf(TowerGroupKind.Member)

        fun Local(depth: Int) = kindOf(TowerGroupKind.Local(depth))

        fun Implicit(depth: Int) = kindOf(TowerGroupKind.Implicit(depth))

        fun Top(depth: Int) = kindOf(TowerGroupKind.Top(depth))

        fun TopPrioritized(depth: Int) = kindOf(TowerGroupKind.TopPrioritized(depth))

        fun Static(depth: Int) = kindOf(TowerGroupKind.Static(depth))

        val Last = kindOf(TowerGroupKind.Last)
    }

    private fun kindOf(kind: TowerGroupKind): TowerGroup = TowerGroup(list + kind)

    fun Weakened(depth: Int) = kindOf(TowerGroupKind.Weakened(depth))

    val Member get() = kindOf(TowerGroupKind.Member)

    fun Local(depth: Int) = kindOf(TowerGroupKind.Local(depth))

    fun Implicit(depth: Int) = kindOf(TowerGroupKind.Implicit(depth))

    val InvokeExtension get() = kindOf(TowerGroupKind.InvokeExtension)

    fun Top(depth: Int) = kindOf(TowerGroupKind.Top(depth))

    fun Static(depth: Int) = kindOf(TowerGroupKind.Static(depth))

    override fun compareTo(other: TowerGroup): Int {
        var index = 0
        while (index < list.size) {
            if (index >= other.list.size) return 1
            when {
                list[index] < other.list[index] -> return -1
                list[index] > other.list[index] -> return 1
            }
            index++
        }
        if (index < other.list.size) return -1
        return 0
    }
}

fun main() {
    println(TowerGroup.Member < TowerGroup.Last)
    println(TowerGroup.Member < TowerGroup.Member.Weakened(1))
    println(TowerGroup.Member.Weakened(1) < TowerGroup.Member)
}