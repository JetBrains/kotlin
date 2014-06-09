package kotlin.reflect.jvm.internal.pcollections;

import java.util.Map;
import java.util.Map.Entry;




/**
 *
 * A static convenience class for creating efficient persistent maps.
 * <p>
 * This class simply creates HashPMaps backed by IntTreePMaps.
 * 
 * @author harold
 */
public final class HashTreePMap {
	// not instantiable (or subclassable):
	private HashTreePMap() {}
	
	private static final HashPMap<Object,Object> EMPTY
		= HashPMap.empty(IntTreePMap.<PSequence<Entry<Object,Object>>>empty());

	/**
	 * @param <K>
	 * @param <V>
	 * @return an empty map
	 */
	@SuppressWarnings("unchecked")
	public static <K,V> HashPMap<K,V> empty() {
		return (HashPMap<K,V>)EMPTY; }
	
	/**
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param value
	 * @return empty().plus(key, value)
	 */
	public static <K,V> HashPMap<K,V> singleton(final K key, final V value) {
		return HashTreePMap.<K,V>empty().plus(key, value); }
	
	/**
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @return empty().plusAll(map)
	 */
	public static <K,V> HashPMap<K,V> from(final Map<? extends K, ? extends V> map) {
		return HashTreePMap.<K,V>empty().plusAll(map); }
}
