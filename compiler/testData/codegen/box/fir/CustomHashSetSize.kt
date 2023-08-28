// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61370

// MODULE: m1
// FILE: m1/THash.java

package m1;

public class THash {

    public int size() { return 1; }
}

// FILE: m1/TObjectHash.java

package m1;

public class TObjectHash<T> extends THash {}

// FILE: m1/THashSet.java

package m1;
import java.util.*;

public class THashSet<T> extends TObjectHash<T> implements Set<T> {
    public Iterator<T> iterator() { return null; }

    public boolean isEmpty() { return false; }

    public boolean contains(Object o) { return false; }

    public Object[] toArray() { return new Object[1]; }

    public <T> T[] toArray(T[] a) { throw new RuntimeException(); }

    public boolean add(T e) { return false; }

    public boolean remove(Object o) { return false; }

    public boolean containsAll(Collection<?> c) { return false; }

    public boolean addAll(Collection<? extends T> c) { return false; }

    public boolean retainAll(Collection<?> c) { return false; }

    public boolean removeAll(Collection<?> c) { return false; }

    public void clear() {}
}

// MODULE: m2(m1)
// FILE: box.kt

package m2
import m1.THashSet

interface HeaderSet : Set<String>

class MutableHeaderSet : HeaderSet, MutableSet<String>, THashSet<String>()

fun box(): String {
    val size1 = THashSet<String>().size
    val size2 = MutableHeaderSet().size
    return if (size1 == 1 && size2 == 1) "OK" else "$size1/$size2"
}
