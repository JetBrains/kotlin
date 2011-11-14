package org.jetbrains.jet.util.slicedmap;

import java.util.Collection;
import java.util.Map;

/**
 * @author abreslav
 */
public interface SlicedMap extends Iterable<Map.Entry<SlicedMapKey<?, ?>, ?>> {
    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    <K, V> boolean containsKey(ReadOnlySlice<K, V> slice, K key);

    // slice.isCollective() must return true
    <K, V> Collection<K> getKeys(WritableSlice<K, V> slice);
}
