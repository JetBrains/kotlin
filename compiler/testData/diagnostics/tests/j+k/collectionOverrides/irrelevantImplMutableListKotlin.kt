// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: AImpl.kt

public abstract class AImpl {
    fun add(element: String): Boolean {
        throw UnsupportedOperationException()
    }

    fun remove(element: String): Boolean {
        throw UnsupportedOperationException()
    }

    fun addAll(elements: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    fun addAll(index: Int, elements: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    fun removeAll(elements: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    fun retainAll(elements: Collection<String>): Boolean {
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

    val size: Int
        get() = throw UnsupportedOperationException()

    fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    fun contains(element: Any?): Boolean {
        throw UnsupportedOperationException()
    }

    fun containsAll(elements: Collection<*>): Boolean {
        throw UnsupportedOperationException()
    }

    fun get(index: Int): String {
        throw UnsupportedOperationException()
    }

    fun indexOf(element: String): Int {
        throw UnsupportedOperationException()
    }

    fun lastIndexOf(element: String): Int {
        throw UnsupportedOperationException()
    }

    fun iterator(): MutableIterator<String> {
        throw UnsupportedOperationException()
    }
}


// FILE: A.java
import java.util.List;

public class A extends AImpl implements List<String> {

}

// FILE: X.kt
class X : A()

fun main() {
    val x = X()
    x[0]
    x.size
    x.remove("")
    x.remove(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
}
