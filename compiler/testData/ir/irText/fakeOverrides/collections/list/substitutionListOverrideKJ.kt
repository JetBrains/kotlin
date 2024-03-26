// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65219, KT-63914

// FILE: 1.kt
import java.util.*

class B<T> : LinkedList<T>()

class C<T>: LinkedList<T>() {
    override val size: Int
        get() = super.size

    override fun remove(): T {
        return null!!
    }
    override fun removeAt(index: Int): T {
        return null!!
    }
}

class D<T> :  LinkedList<T>() {
    override fun remove(): T {
        return null!!
    }
}

fun test(b: B<Int>, c: C<Int?>, d: D<Int>){
    b.get(1)
    b.size
    b.add(1)
    b.remove()
    b.remove(1)
    b.removeAt(1)

    c.get(1)
    c.size
    c.add(1)
    c.remove()
    c.remove(1)
    c.removeAt(1)

    d.remove(1)
}