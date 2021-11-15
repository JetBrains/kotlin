// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_STDLIB
// FILE: A.java

public class A extends AImpl implements java.util.List<String> {
    public <T> T[] toArray(T[] a) {return null;}
    public Object[] toArray() {return null;}
}

// FILE: test.kt

public abstract class AImpl {
    fun add(element: String): Boolean {
        throw UnsupportedOperationException()
    }

    fun remove(element: Any?): Boolean {
        throw UnsupportedOperationException()
    }

    @JvmSuppressWildcards(suppress = false)
    fun addAll(elements: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    fun addAll(index: Int, elements: Collection<@JvmWildcard String>): Boolean {
        throw UnsupportedOperationException()
    }

    fun removeAll(elements: Collection<*>): Boolean {
        throw UnsupportedOperationException()
    }

    fun retainAll(elements: Collection<*>): Boolean {
        throw UnsupportedOperationException()
    }

    fun clear() {
        throw UnsupportedOperationException()
    }

    fun set(index: Int, element: String): String {
        throw UnsupportedOperationException()
    }

    fun add(index: Int, element: String) {
        throw UnsupportedOperationException()
    }

    fun remove(index: Int): String {
        throw UnsupportedOperationException()
    }

    fun listIterator(): MutableListIterator<String> {
        throw UnsupportedOperationException()
    }

    fun listIterator(index: Int): MutableListIterator<String> {
        throw UnsupportedOperationException()
    }

    fun subList(fromIndex: Int, toIndex: Int): MutableList<String> {
        throw UnsupportedOperationException()
    }

    fun size(): Int = 56

    fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    fun contains(element: Any?) = true

    fun containsAll(elements: Collection<*>): Boolean {
        throw UnsupportedOperationException()
    }

    fun get(index: Int): String {
        throw UnsupportedOperationException()
    }

    fun indexOf(element: Any?): Int {
        throw UnsupportedOperationException()
    }

    fun lastIndexOf(element: Any?): Int {
        throw UnsupportedOperationException()
    }

    fun iterator(): MutableIterator<String> {
        throw UnsupportedOperationException()
    }
}


class X : A()

fun box(): String {
    val x = X()
    if (x.size != 56) return "fail 1"
    if (!x.contains("")) return "fail 2"
    return "OK"
}
