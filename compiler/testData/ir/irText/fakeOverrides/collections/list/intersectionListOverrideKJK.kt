// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65219, KT-63914

// FILE: 1.kt
import java.util.*

class A : LinkedList<Int>(), List<Int>

class B :  LinkedList<Int>(), List<Int> {
    override fun remove(): Int {
        return 1
    }
    override fun removeAt(index: Int): Int {
        return 1
    }
    override fun remove(element: Int): Boolean {
        return true
    }
    override val size: Int
        get() = 2
}

class C : LinkedList<Int?>(), MutableList<Int?>

class D : LinkedList<Int?>(), MutableList<Int?>{
    override fun get(index: Int): Int? {
        return 2
    }
    override fun remove(): Int? {
        return 2
    }
    override fun removeAt(index: Int): Int? {
        return 2
    }
    override fun remove(element: Int?): Boolean {
        return true
    }
}

fun test(a: A, b: B, c: C, d: D){
    a.size
    a.add(1)
    a.get(0)
    a.remove()
    a.removeAt(1)
    a.remove(1)

    b.size
    b.add(1)
    b.get(0)
    b.remove()
    b.removeAt(1)
    b.remove(1)

    c.size
    c.add(1)
    c.get(0)
    c.remove()
    c.removeAt(1)
    c.remove(1)

    d.size
    d.add(1)
    d.get(0)
    d.remove()
    d.removeAt(1)
    d.remove(1)
}
