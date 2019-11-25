// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

import java.util.*;
public class J {
    abstract static public class AImpl {
        public final int size() {
            return 56;
        }

        public boolean isEmpty() {
            return false;
        }

        public final boolean contains(Object o) {
            return true;
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

    public static class A extends AImpl implements List<String> {
    }
}

// FILE: test.kt

class X : J.A()

fun box(): String {
    val x = X()
    if (x.size != 56) return "fail 1"
    if (!x.contains("")) return "fail 2"

    return "OK"
}
