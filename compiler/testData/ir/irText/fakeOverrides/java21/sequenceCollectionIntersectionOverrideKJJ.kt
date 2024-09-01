// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK_21
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-63914, KT-65219

// FILE: 1.kt
import java.util.*

class A : LinkedList<Int>(), SequencedCollection<Int>

class B : LinkedList<Int>(), SequencedCollection<Int> {
    override fun addFirst(e: Int?) { }
    override fun reversed(): LinkedList<Int> {
        return null!!
    }
}

fun test(a: A, b: B){
    a.size
    a.remove(element = 1)
    a.addFirst(3)
    a.removeFirst()
    a.removeLast()
    a.reversed()

    b.reversed()
    b.addFirst(1)
}
