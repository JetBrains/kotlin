// FILE: X.java
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class X implements java.util.List<String> {
    @Override
    public int size() {
        return 0;
    }

    public int getSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    public boolean contains(String o) {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return null;
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @NotNull
    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(String s) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends String> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public String get(int index) {
        return null;
    }

    @Override
    public String set(int index, String element) {
        return null;
    }

    @Override
    public void add(int index, String element) {

    }

    @Override
    public String remove(int index) {
        return null;
    }

    public String removeAt(int index) {
        return null;
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }

    @NotNull
    @Override
    public ListIterator<String> listIterator() {
        return null;
    }

    @NotNull
    @Override
    public ListIterator<String> listIterator(int index) {
        return null;
    }

    @NotNull
    @Override
    public List<String> subList(int fromIndex, int toIndex) {
        return null;
    }
}

// FILE: main.kt

class Y : X()
class <!CONFLICTING_JVM_DECLARATIONS!>Y2<!> : X() {
    <!CONFLICTING_JVM_DECLARATIONS!>override fun removeAt(index: Int)<!> = ""
}

fun main() {
    X().remove("")
    X().removeAt(1)

    val y: MutableList<String> = Y()
    y.removeAt(1)

    Y().remove("")
    Y().removeAt(1)

    X().remove("")
    X().removeAt(1)
}
