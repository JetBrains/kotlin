// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-63914

// FILE: 1.kt
import java.util.*

abstract class A : SortedSet<Any>, HashSet<Any>() {  //Kotlin ← Java1, Java2
    override fun spliterator(): Spliterator<Any> {
        return null!!
    }
}

abstract class B : SortedSet<Any>, HashSet<Any>() {
    override fun spliterator(): Spliterator<Any> {
        return null!!
    }
    override fun first(): Any {
        return 1
    }
    override fun remove(element: Any): Boolean {
        return true
    }
}

abstract class C : SortedSet<Any>, ArrayList<Any>() {
    override fun spliterator(): Spliterator<Any> {
        TODO("Not yet implemented")
    }
}

fun test(a: A, b: B, c: C) {
    a.size
    a.first()
    a.last()
    a.add(1)
    a.add(null)
    a.remove(1)
    a.remove(null)
    a.spliterator()

    b.size
    b.first()
    b.last()
    b.add(1)
    b.add(null)
    b.remove(1)
    b.remove<Any?>(null)
    b.spliterator()

    c.size
    c.first()
    c.last()
    c.add(1)
    c.add(null)
    c.remove(1)
    c.remove(null)
    c.spliterator()
    c.removeAt(1)
}