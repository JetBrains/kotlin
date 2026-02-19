// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: 1.kt
import java.util.*
import kotlin.collections.HashSet

abstract class A : SortedSet<Any>, HashSet<Any>() {
    override fun spliterator(): Spliterator<Any> {
        return null!!
    }
}

abstract class B : SortedSet<Any>, HashSet<Any>() {
    override fun spliterator(): Spliterator<Any> {
        return null!!
    }

    override val size: Int
        get() = 5

    override fun remove(element: Any): Boolean {
        return true
    }
}

fun test(a: A, b: B) {
    a.size
    a.add(1)
    a.add(null)
    a.first()
    a.remove(1)
    a.removeAll(listOf(null))

    b.size
    b.add(1)
    b.add(null)
    b.first()
    b.remove(1)
    b.removeAll(listOf(null))
}
