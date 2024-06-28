// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK_21
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-63914, KT-65219

// FILE: 1.kt
import java.util.*

abstract class A : HashSet<Int>(), SequencedSet<Int> {
    override fun spliterator(): Spliterator<Int> {
        return null!!
    }
}

class B : HashSet<Int>(), SequencedSet<Int>{
    override fun reversed(): SequencedSet<Int> {
        return null!!
    }
    override fun spliterator(): Spliterator<Int> {
        return null!!
    }
}

fun test(a: A, b: B){
    a.size
    a.addFirst(null)
    a.reversed()

    b.reversed()
    b.addFirst(1)
}
