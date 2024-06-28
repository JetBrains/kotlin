// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63857

// FILE: A.java
abstract public class A extends B {
    public Integer removeAt(int x) { return 0; }
    public boolean remove(Integer x) { return false; }
}

// FILE: main.kt
import java.util.*;

abstract class B : MutableList<Int>, AbstractList<Int>() {
    override fun removeAt(index: Int): Int = null!!
    override fun remove(element: Int): Boolean = null!!
}

abstract class D : AbstractList<Int>() {
    // removeAt() doesn't exist in java/util/AbstractList, it's a
    // fake override of the method from kotlin/collections/MutableList
    override fun removeAt(index: Int): Int = 0
    // AbstractList::remove() should return Int here. No fake overrides created.
    // This may be a bug because the old compiler doesn't report a diagnostic here.
    override fun remove(element: Int): Boolean = false
}

fun testABD(a: A, b: B, d: D) {
    a.remove(1)
    a.removeAt(0)
    b.remove(1)
    b.removeAt(0)
    d.remove(1)
    d.removeAt(0)
}

fun testArrayList(c: ArrayList<Int>) {
    c.remove(1)
    c.removeAt(0)
}

class AImpl : A() {
    override fun get(index: Int): Int = 0
    override val size: Int get() = 0
}

class DImpl : D() {
    override fun get(index: Int): Int = 0
    override val size: Int get() = 0
}

fun box(): String {
    testABD(AImpl(), AImpl(), DImpl())

    val c = ArrayList<Int>()
    c.add(1)
    c.add(1)
    testArrayList(c)

    return "OK"
}
