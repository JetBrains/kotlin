// TARGET_BACKEND: JVM

// FILE: IntOpenHashSet.java

import java.util.Iterator;

public class IntOpenHashSet extends AbstractIntSet {
	@Override
	public boolean remove(final int k) { return false; }

	@Override
	public void clear() {}

	@Override
	public int size() { return 0; }

	@Override
	public Iterator<Integer> iterator() { return null; }
}

// FILE: AbstractIntSet.java

abstract class AbstractIntSet extends AbstractIntCollection implements IntSet {
	@Override
	public boolean remove(int k) { return false; }
}

// FILE: IntSet.java

import java.util.Set;

interface IntSet extends IntCollection, Set<Integer> {
	boolean remove(int k);
}

// FILE: AbstractIntCollection.java

import java.util.AbstractCollection;

abstract class AbstractIntCollection extends AbstractCollection<Integer> implements IntCollection {}

// FILE: IntCollection.java

import java.util.Collection;

interface IntCollection extends Collection<Integer> {}

// FILE: box.kt

fun box(): String {
    val s = IntOpenHashSet()
    s.remove(0)
    return "OK"
}
