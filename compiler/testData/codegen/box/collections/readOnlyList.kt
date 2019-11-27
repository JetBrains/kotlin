// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

import java.util.*;

public class J {

    private static class MyList<E> extends KList<E> {}

    public static String foo() {
        Collection<String> collection = new MyList<String>();
        if (!collection.contains("ABCDE")) return "fail 1";
        if (!collection.containsAll(Arrays.asList(1, 2, 3))) return "fail 2";
        return "OK";
    }
}

// FILE: test.kt

open class KList<E> : List<E> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }
    override fun contains(o: E) = true
    override fun containsAll(c: Collection<E>) = true

    override fun iterator(): Iterator<E> {
        throw UnsupportedOperationException()
    }

    override fun get(index: Int): E {
        throw UnsupportedOperationException()
    }

    override fun indexOf(element: E): Int {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(element: E): Int {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): ListIterator<E> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): ListIterator<E> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> {
        throw UnsupportedOperationException()
    }
}

fun box() = J.foo()
