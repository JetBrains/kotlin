package org.jetbrains.jet.util.slicedmap;

/**
 * @author abreslav
 */
public interface MutableSlicedMap extends SlicedMap {

    <K, V> void put(WritableSlice<K, V> slice, K key, V value);

    <K, V> V remove(RemovableSlice<K, V> slice, K key);
}
