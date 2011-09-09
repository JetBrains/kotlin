package org.jetbrains.jet.util.slicedmap;

import java.util.Map;

/**
 * @author abreslav
 */
public interface SlicedMap extends Iterable<Map.Entry<SlicedMapKey<?, ?>, ?>> {
    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    <K, V> boolean containsKey(ReadOnlySlice<K, V> slice, K key);
    
    
}
