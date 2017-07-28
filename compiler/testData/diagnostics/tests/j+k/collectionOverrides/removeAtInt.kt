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
    override fun remove(element: Int): Boolean = null!!
}

fun main(a: A, b: B, c: ArrayList<Int>) {
    a.remove(1)
    a.removeAt(0)
    b.remove(1)
    b.removeAt(0)
    c.remove(1)
    c.removeAt(0)
}
