package kotlin.reflect.jvm.internal.pcollections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;




/**
 * 
 * An efficient persistent map from integer keys to non-null values.
 * <p>
 * Iteration occurs in the integer order of the keys.
 * <p>
 * This implementation is thread-safe (assuming Java's AbstractMap and AbstractSet are thread-safe),
 * although its iterators may not be.
 * <p>
 * The balanced tree is based on the Glasgow Haskell Compiler's Data.Map implementation,
 * which in turn is based on "size balanced binary trees" as described by:
 * <p>
 * Stephen Adams, "Efficient sets: a balancing act",
 * Journal of Functional Programming 3(4):553-562, October 1993,
 * http://www.swiss.ai.mit.edu/~adams/BB/.
 * <p>
 * J. Nievergelt and E.M. Reingold, "Binary search trees of bounded balance",
 * SIAM journal of computing 2(1), March 1973.
 * 
 * @author harold
 *
 * @param <V>
 */
public final class IntTreePMap<V> extends AbstractMap<Integer,V> implements PMap<Integer,V> {
//// STATIC FACTORY METHODS ////
	private static final IntTreePMap<Object> EMPTY = new IntTreePMap<Object>(IntTree.EMPTYNODE);

	/**
	 * @param <V>
	 * @return an empty map
	 */
	@SuppressWarnings("unchecked")
	public static <V> IntTreePMap<V> empty() {
		return (IntTreePMap<V>)EMPTY; }
	
	/**
	 * @param <V>
	 * @param key
	 * @param value
	 * @return empty().plus(key, value)
	 */
	public static <V> IntTreePMap<V> singleton(final Integer key, final V value) {
		return IntTreePMap.<V>empty().plus(key, value); }
	
	/**
	 * @param <V>
	 * @param map
	 * @return empty().plusAll(map)
	 */
	@SuppressWarnings("unchecked")
	public static <V> IntTreePMap<V> from(final Map<? extends Integer, ? extends V> map) {
		if(map instanceof IntTreePMap)
			return (IntTreePMap<V>)map; //(actually we only know it's IntTreePMap<? extends V>)
									// but that's good enough for an immutable
									// (i.e. we can't mess someone else up by adding the wrong type to it)
		return IntTreePMap.<V>empty().plusAll(map); }
	

//// PRIVATE CONSTRUCTORS ////
	private final IntTree<V> root;
	// not externally instantiable (or subclassable):
	private IntTreePMap(final IntTree<V> root) {
		this.root = root; }
	private IntTreePMap<V> withRoot(final IntTree<V> root) {
		if(root==this.root) return this;
		return new IntTreePMap<V>(root); }
	

//// UNINHERITED METHODS OF IntTreePMap ////
	IntTreePMap<V> withKeysChangedAbove(final int key, final int delta) {
		// TODO check preconditions of changeKeysAbove()
		// TODO make public?
		return withRoot( root.changeKeysAbove(key, delta) );
	}
	
	IntTreePMap<V> withKeysChangedBelow(final int key, final int delta) {
		// TODO check preconditions of changeKeysAbove()
		// TODO make public?
		return withRoot( root.changeKeysBelow(key, delta) );
	}
	
//// REQUIRED METHODS FROM AbstractMap ////
	// this cache variable is thread-safe, since assignment in Java is atomic:
	private Set<Entry<Integer,V>> entrySet = null;
	@Override
	public Set<Entry<Integer,V>> entrySet() {
		if(entrySet==null)
			entrySet = new AbstractSet<Entry<Integer,V>>() {
				// REQUIRED METHODS OF AbstractSet // 
				@Override
				public int size() { // same as Map
					return IntTreePMap.this.size(); }
				@Override
				public Iterator<Entry<Integer,V>> iterator() {
					return root.iterator(); }
				// OVERRIDDEN METHODS OF AbstractSet //
				@Override
				public boolean contains(final Object e) {
					if(!(e instanceof Entry))
						return false;
					V value = get(((Entry<?,?>)e).getKey());
					return value!=null && value.equals(((Entry<?,?>)e).getValue());
				}
			};
		return entrySet;
	}

	
//// OVERRIDDEN METHODS FROM AbstractMap ////
	@Override
	public int size() {
		return root.size(); }

	@Override
	public boolean containsKey(final Object key) {
		if(!(key instanceof Integer))
			return false;
		return root.containsKey((Integer)key);
	}
	
	@Override
	public V get(final Object key) {
		if(!(key instanceof Integer))
			return null;
		return root.get((Integer)key);
	}

	
//// IMPLEMENTED METHODS OF PMap////
	public IntTreePMap<V> plus(final Integer key, final V value) {
		return withRoot( root.plus(key, value) ); }
	
	public IntTreePMap<V> minus(final Object key) {
		if(!(key instanceof Integer)) return this;
		return withRoot( root.minus((Integer)key) ); }
	
	public IntTreePMap<V> plusAll(final Map<? extends Integer, ? extends V> map) {
		IntTree<V> root = this.root;
		for(Entry<? extends Integer,? extends V> entry : map.entrySet())
			root = root.plus(entry.getKey(), entry.getValue());
		return withRoot(root);
	}

	public IntTreePMap<V> minusAll(final Collection<?> keys) {
		IntTree<V> root = this.root;
		for(Object key : keys)
			if(key instanceof Integer)
				root = root.minus((Integer)key);
		return withRoot(root);
	}
}
