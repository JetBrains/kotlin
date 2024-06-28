// TARGET_BACKEND: JVM

// FILE: J.java

import java.util.*;
public class J {
    abstract static public class AImpl<E> {
        public int size() {
            return 56;
        }

        public boolean isEmpty() {
            return false;
        }

        public final boolean contains(Object o) {
            return true;
        }

        public Iterator<E> iterator() {
            return null;
        }

        public Object[] toArray() {
            return new Object[0];
        }

        public <T> T[] toArray(T[] a) {
            return null;
        }

        public boolean add(E s) {
            return false;
        }

        public boolean remove(Object o) {
            return false;
        }

        public boolean containsAll(Collection<?> c) {
            return false;
        }

        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        public boolean addAll(int index, Collection<? extends E> c) {
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

        public E get(int index) {
            return null;
        }

        public E set(int index, E element) {
            return null;
        }

        public void add(int index, E element) {

        }

        public E remove(int index) {
            return null;
        }

        public int indexOf(Object o) {
            return 0;
        }

        public int lastIndexOf(Object o) {
            return 0;
        }

        public ListIterator<E> listIterator() {
            return null;
        }

        public ListIterator<E> listIterator(int index) {
            return null;
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return null;
        }
    }

    public static class A<E> extends AImpl<E> implements List<E> {
    }
}

// FILE: test.kt

class X : J.A<Any?>()

fun box(): String {
    val x = X()
    if (x.size != 56) return "fail 1"
    if (!x.contains(null)) return "fail 2"

    return "OK"
}
