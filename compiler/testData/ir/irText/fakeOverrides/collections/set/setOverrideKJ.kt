// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: 1.kt
import java.util.SortedSet

abstract class A : SortedSet<Any>

abstract class B(override val size: Int) : SortedSet<Any> {
    override fun remove(element: Any?): Boolean {
        return true
    }

    override fun removeAll(elements: Collection<Any>): Boolean {
        return false
    }

    override fun first(): Any {
        return 1
    }

    override fun last(): Any {
        return 10
    }
}

fun test(a: A, b: B) {
    a.size
    a.first()
    a.last()
    a.add(1)
    a.add(null)
    a.remove(1)
    a.remove(null)

    b.size
    b.first()
    b.last()
    b.add(1)
    b.add(null)
    b.remove(null)
}