// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// FILE: IntOpenHashSet.java

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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

abstract class AbstractIntSet extends AbstractIntCollection implements IntSet {
	@Override
	public boolean remove(int k) { return false; }
}

abstract class AbstractIntCollection extends AbstractCollection<Integer> implements IntCollection {}

interface IntCollection extends Collection<Integer> {}

interface IntSet extends IntCollection, Set<Integer> {
	boolean remove(int k);
}

// FILE: box.kt

fun box(): String {
    val s = IntOpenHashSet()
    s.remove(0)
    return "OK"
}
