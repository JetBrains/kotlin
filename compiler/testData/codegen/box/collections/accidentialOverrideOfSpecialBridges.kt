// IGNORE_BACKEND_K2: JVM_IR
// FIR status: wrong ABSTRACT_MEMBER_NOT_IMPLEMENTED, probably provoked by override mapping error
// TARGET_BACKEND: JVM_IR
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB

// FILE: AImpl.kt
public abstract class AImpl {
    fun add(element: String): Boolean {
        throw UnsupportedOperationException()
    }

    fun remove(element: String): Boolean {
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class A extends AImpl implements List<String> {
    @Override
    public int size() {
        return 0;
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return a;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends String> c) {
        return false;
    }
    @Override
    public boolean addAll(int index, @NotNull Collection<? extends String> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }
}

// FILE: X.kt

class X : A()

// FILE: test.kt

private fun assertUnsupported(action: () -> Unit) {
    assert(runCatching(action).exceptionOrNull() is UnsupportedOperationException)
}

fun box(): String {
    val x = X()
    assert(x.size == 0)
    assert(x.indexOf("") == 0)
    assert(x.lastIndexOf("") == 0)
    assert(!x.retainAll(emptyList()))
    assert(!x.removeAll(emptyList()))
    assert(!x.remove(""))
    assert(!x.addAll(emptyList()))
    assert(!x.addAll(0, emptyList()))
    assert(x.toArray().size == 0)
    assert(x.toTypedArray().size == 0)
    val y = Array<Any>(3) { Any() }
    assert(x.toArray(y) == y)
    
    assertUnsupported { x.add("") }
    assertUnsupported { x.removeAt(2) }
    assertUnsupported { x.clear() }
    assertUnsupported { x[2] }
    assertUnsupported { x[2] = "" }
    assertUnsupported { x.add(2, "") }
    assertUnsupported { x.iterator() }
    assertUnsupported { x.listIterator() }
    assertUnsupported { x.listIterator(23) }
    assertUnsupported { x.subList(23, 34) }
    assertUnsupported { x.isEmpty() }
    assertUnsupported { x.contains(Any()) }
    assertUnsupported { x.containsAll(listOf(Any())) }
    
    return "OK"
}
