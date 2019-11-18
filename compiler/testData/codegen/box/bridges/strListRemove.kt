// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR

// FILE: J.java

import java.util.*;

public class J {

    public static String foo() {
        List list = new KList();
        if (!list.remove(0).equals("OK")) return "fail";

        return "OK";
    }
}

// FILE: test.kt

class KList : MutableList<String> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun contains(o: String): Boolean {
        throw UnsupportedOperationException()
    }
    override fun containsAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(index: Int): String {
        throw UnsupportedOperationException()
    }

    override fun indexOf(o: String): Int {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(o: String): Int {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<String> {
        throw UnsupportedOperationException()
    }

    override fun add(e: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(o: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(index: Int, c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun set(index: Int, element: String): String {
        throw UnsupportedOperationException()
    }

    override fun add(index: Int, element: String) {
        throw UnsupportedOperationException()
    }

    override fun removeAt(index: Int): String {
        return "OK"
    }

    override fun listIterator(): MutableListIterator<String> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): MutableListIterator<String> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<String> {
        throw UnsupportedOperationException()
    }

}
fun box() = J.foo()
