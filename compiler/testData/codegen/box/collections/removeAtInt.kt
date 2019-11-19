// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

import java.util.*;

public class J {

    private static class MyList extends A {}

    public static String foo() {
        MyList myList = new MyList();
        List<Integer> list = (List<Integer>) myList;

        if (!list.remove((Integer) 1)) return "fail 1";
        if (list.remove((int) 1) != 123) return "fail 2";

        if (!myList.remove((Integer) 1)) return "fail 3";
        if (myList.remove((int) 1) != 123) return "fail 4";

        if (myList.removeAt(1) != 123) return "fail 5";
        return "OK";
    }
}

// FILE: test.kt

open class A : MutableList<Int> {
    override val size: Int
        get() = throw UnsupportedOperationException()
    override fun isEmpty(): Boolean = throw UnsupportedOperationException()

    override fun contains(o: Int): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<Int>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(index: Int): Int {
        throw UnsupportedOperationException()
    }

    override fun indexOf(o: Int): Int {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(o: Int): Int {
        throw UnsupportedOperationException()
    }

    override fun add(e: Int): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(o: Int) = true

    override fun removeAt(index: Int): Int = 123

    override fun addAll(c: Collection<Int>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(index: Int, c: Collection<Int>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(c: Collection<Int>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(c: Collection<Int>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun set(index: Int, element: Int): Int {
        throw UnsupportedOperationException()
    }

    override fun add(index: Int, element: Int) {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): MutableListIterator<Int> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): MutableListIterator<Int> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Int> {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<Int> {
        throw UnsupportedOperationException()
    }
}

fun box() = J.foo()
