// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK_21
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65667, KT-63914

// FILE: 1.kt
import java.util.*
import kotlin.collections.HashMap

abstract class A : HashMap<Int, Int>(), SequencedMap<Int, Int>

class B : HashMap<Int, Int>(), SequencedMap<Int, Int> {
    override fun reversed(): SequencedMap<Int, Int> {
        return null!!
    }
}

fun test(a: A, b: B){
    a.size
    a.putLast(null, 1)
    a.putLast(2, null)
    a.firstEntry()
    a.lastEntry()
    a.reversed()

    b.reversed()
}