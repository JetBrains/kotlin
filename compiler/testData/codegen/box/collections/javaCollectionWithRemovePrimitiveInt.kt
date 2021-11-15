// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: javaCollectionWithRemovePrimitiveInt.kt

fun box(): String {
    val j = JIntCollection(arrayListOf(1, 2, 3))
    j.remove(1) // remove(int)
    if (j.removed != 1) throw AssertionError("${j.removed}")
    return "OK"
}

// FILE: JIntCollection.java

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class JIntCollection implements Collection<Integer> {
    private final Collection<Integer> collection;
    public int removed = 0;

    public JIntCollection(Collection<Integer> collection) {
        this.collection = collection;
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return collection.contains(o);
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return collection.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return collection.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return collection.toArray(a);
    }

    @Override
    public boolean add(Integer integer) {
        return collection.add(integer);
    }

    @Override
    public boolean remove(Object o) {
        return collection.remove(o);
    }

    public boolean remove(int x) {
        removed = x;
        return true;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return collection.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Integer> c) {
        return collection.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return collection.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return collection.retainAll(c);
    }

    @Override
    public void clear() {
        collection.clear();
    }
}
