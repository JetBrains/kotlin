// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SKIP_JDK6
// FULL_JDK

// JVM_ABI_K1_K2_DIFF: KT-63857

// FILE: superCallToFakeRemoveAt.kt
class Test : IntArrayList() {
    override fun removeAt(index: Int): Int {
        return super.removeAt(index)
    }
}

fun box(): String {
    val t = Test()
    t.add(1)
    t.removeAt(0)
    // Just check that there's no exception (e.g., NSME)
    return "OK"
}

// FILE: IntArrayListBase.java
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class IntArrayListBase implements List<Integer> {
    private final ArrayList<Integer> data = new ArrayList<>();

    public IntArrayListBase() {
        super();
    }

    @Override
    public Integer remove(int index) {
        return data.remove(index);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return data.contains(o);
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return data.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return data.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return data.toArray(a);
    }

    @Override
    public boolean add(Integer integer) {
        return data.add(integer);
    }

    @Override
    public boolean remove(Object o) {
        return data.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return data.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Integer> c) {
        return data.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends Integer> c) {
        return data.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return data.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return data.retainAll(c);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Integer get(int index) {
        return data.get(index);
    }

    @Override
    public Integer set(int index, Integer element) {
        return data.set(index, element);
    }

    @Override
    public void add(int index, Integer element) {
        data.add(index, element);
    }

    @Override
    public int indexOf(Object o) {
        return data.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return data.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<Integer> listIterator() {
        return data.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<Integer> listIterator(int index) {
        return data.listIterator(index);
    }

    @NotNull
    @Override
    public List<Integer> subList(int fromIndex, int toIndex) {
        return data.subList(fromIndex, toIndex);
    }
}

// FILE: IntArrayList.java
public class IntArrayList extends IntArrayListBase {}
