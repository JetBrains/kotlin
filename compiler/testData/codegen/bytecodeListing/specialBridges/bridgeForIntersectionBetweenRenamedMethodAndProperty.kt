// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// DUMP_IR
// DUMP_EXTERNAL_CLASS: JavaSet
// ISSUE: KT-62570

// FILE: A.java
public class A<T> {
    public int size() { return 1; }
}

// FILE: B.java
public class B extends A<String> {}

// FILE: C.java
public class C extends B {}

// FILE: JavaSet.java
import java.util.*;

public class JavaSet<T> extends C implements Set<T> {
    public Iterator <T> iterator () { return null; }
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

// FILE: kotlinSet.kt
open class KotlinSet : JavaSet<String>()
