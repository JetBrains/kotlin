// WITH_STDLIB

// FILE: A.java

public class A {
    public int size()
    { return 1; }
}

// FILE: JavaSet.java

import java.util.*;

public class JavaSet<T> extends A implements Set<T> {
    public Iterator < T > iterator () { return null; }

    public boolean isEmpty() { return false; }

    public boolean contains(Object o) { return false; }

    public Object [] toArray () { return new Object [1]; }

    public<T> T [] toArray (T[] a) { throw new RuntimeException (); }

    public boolean add(T e) { return false; }

    public boolean remove(Object o) { return false; }

    public boolean containsAll(Collection<?> c) { return false; }

    public boolean addAll(Collection < ? extends T > c) { return false; }

    public boolean retainAll(Collection<?> c) { return false; }

    public boolean removeAll(Collection<?> c) { return false; }

    public void clear() {}
}

// FILE: box.kt

open class KotlinSet : JavaSet<String>()

interface SizeInterface {
    fun size() = 42
}

class Diamond : KotlinSet(), SizeInterface

fun box(): String {
    Diamond()
    return "OK"
}