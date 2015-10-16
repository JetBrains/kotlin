// FILE: A.java
abstract public class A<F> extends B<F> {
    public F remove(int x) { }
    public boolean remove(Object x) { }
}

// FILE: main.kt
import java.util.*;

abstract class B<T> : MutableList<T>, AbstractList<T>() {
    override fun removeAt(index: Int): T = null!!
    override fun remove(element: T): Boolean = null!!
}

fun main(a: A<String>, b: B<String>, c: ArrayList<String>) {
    a.remove("")
    a.removeAt(0)
    b.remove("")
    b.removeAt(0)
    c.remove("")
    c.removeAt(0)
}
