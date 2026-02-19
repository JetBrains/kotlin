// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK_21
// WITH_STDLIB

// FILE: 1.kt
import java.util.*

abstract class A : SequencedMap<Any, Any>

abstract class B(override val size: Int) : SequencedMap<Any, Any> {
    override fun putFirst(k: Any?, v: Any?): Any {
        return ""
    }

    override fun reversed(): SequencedMap<Any, Any> {
        return null!!
    }

    override fun firstEntry(): MutableMap.MutableEntry<Any, Any> {
        return null!!
    }

    override fun pollFirstEntry(): MutableMap.MutableEntry<Any, Any> {
        return null!!
    }

    override fun sequencedKeySet(): SequencedSet<Any> {
        return null!!
    }
}

fun test(a: A, b: B) {
    a.putFirst(1, null)
    a.firstEntry()
    a.putLast(null, 2)
    a.lastEntry()
    a.pollFirstEntry()
    a.reversed()
    a.sequencedKeySet()

    b.putFirst(1, null)
    b.firstEntry()
    b.putLast(null, 2)
    b.lastEntry()
    b.pollFirstEntry()
    b.reversed()
    b.sequencedKeySet()
}