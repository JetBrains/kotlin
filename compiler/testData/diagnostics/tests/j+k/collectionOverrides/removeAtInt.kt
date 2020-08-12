// FIR_IDENTICAL
// JAVAC_EXPECTED_FILE
// FILE: A.java
abstract public class A extends B {
    public Integer removeAt(int x) { }
    public boolean remove(Integer x) { }
}

// FILE: main.kt
import java.util.*;

abstract class B : MutableList<Int>, AbstractList<Int>() {
    override fun removeAt(index: Int): Int = null!!
    override fun remove(element: Int): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Boolean<!> = null!!
}

abstract class D : AbstractList<Int>() {
    // removeAt() doesn't exist in java/util/AbstractList, it's a
    // fake override of the method from kotlin/collections/MutableList
    override fun removeAt(index: Int): Int = null!!
    // AbstractList::remove() should return Int here. No fake overrides created.
    // This may be a bug because the old compiler doesn't report a diagnostic here.
    override fun remove(element: Int): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Boolean<!> = null!!
}

fun main(a: A, b: B, c: ArrayList<Int>) {
    a.remove(1)
    a.removeAt(0)
    b.remove(1)
    b.removeAt(0)
    c.remove(1)
    c.removeAt(0)
}
