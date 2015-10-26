// FILE: AImpl.java

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class AImpl {
    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean contains(Object o) {
        return false;
    }

    public Iterator<String> iterator() {
        return null;
    }

    public Object[] toArray() {
        return new Object[0];
    }

    public <T> T[] toArray(T[] a) {
        return null;
    }

    public boolean add(String s) {
        return false;
    }

    public boolean remove(Object o) {
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        return false;
    }

    public boolean addAll(Collection<? extends String> c) {
        return false;
    }

    public boolean addAll(int index, Collection<? extends String> c) {
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        return false;
    }

    public boolean retainAll(Collection<?> c) {
        return false;
    }

    public void clear() {

    }

    public String get(int index) {
        return null;
    }

    public String set(int index, String element) {
        return null;
    }

    public void add(int index, String element) {

    }

    public String remove(int index) {
        return null;
    }

    public int indexOf(Object o) {
        return 0;
    }

    public int lastIndexOf(Object o) {
        return 0;
    }

    public ListIterator<String> listIterator() {
        return null;
    }

    public ListIterator<String> listIterator(int index) {
        return null;
    }

    public List<String> subList(int fromIndex, int toIndex) {
        return null;
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
