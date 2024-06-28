// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK_21
// WITH_STDLIB

// FILE: 1.kt
import java.util.*
abstract class A : SortedSet<Int>, Set<Int>

abstract class B(override val size: Int) :  SortedSet<Int>, Set<Int> {
    override fun reversed(): SortedSet<Int> {
        return null!!
    }

    override fun first(): Int {
        return 1
    }
}

abstract class C : SortedSet<Int?>, MutableSet<Int?>

abstract class D : SortedSet<Int?>, MutableSet<Int?> {
    override fun reversed(): SortedSet<Int?>? {
        return null!!
    }

    override fun addLast(e: Int?) { }
}

fun test(a: A, b: B, c: C, d: D){
    a.size
    a.add(1)
    a.remove(1)
    a.addFirst(1)
    a.addLast(null)
    a.removeFirst()
    a.removeLast()
    a.reversed()
    a.first

    b.reversed()
    b.first

    c.size
    c.add(1)
    c.remove(1)
    c.addFirst(1)
    c.addLast(null)
    c.removeFirst()
    c.removeLast()
    c.reversed()
    c.first

    d.reversed()
    d.addLast(null)
}