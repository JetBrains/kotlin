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

open class KList<E> : MutableList<E> {
    override fun add(e: E): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(o: E): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(c: Collection<E>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(index: Int, c: Collection<E>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(c: Collection<E>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(c: Collection<E>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun set(index: Int, element: E): E {
        throw UnsupportedOperationException()
    }

    override fun add(index: Int, element: E) {
        throw UnsupportedOperationException()
    }

    override fun removeAt(index: Int): E {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): MutableListIterator<E> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<E> {
        throw UnsupportedOperationException()
    }

    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun contains(o: E) = true
    override fun containsAll(c: Collection<E>) = true

    override fun get(index: Int): E {
        throw UnsupportedOperationException()
    }

    override fun indexOf(o: E): Int {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(o: E): Int {
        throw UnsupportedOperationException()
    }
}

fun box() = J.foo()
