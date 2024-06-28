// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65219, KT-63914

// FILE: 1.kt
import java.util.*

class B : LinkedList<Int>()

class C: LinkedList<Int>() {
    override val size: Int
        get() = super.size

    override fun remove(): Int {
        return 1
    }
    override fun removeAt(index: Int): Int {
        return 1
    }
}

class D :  LinkedList<Int>() {
    override fun remove(element: Int): Boolean {
        return true
    }
}

fun test(b: B, c: C, d: D){
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