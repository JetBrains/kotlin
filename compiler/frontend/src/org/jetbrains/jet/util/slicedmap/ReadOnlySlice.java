package org.jetbrains.jet.util.slicedmap;

/**
 * @author abreslav
 */
public interface ReadOnlySlice<K, V> {
    SlicedMapKey<K, V> makeKey(K key);

    V computeValue(SlicedMap map, K key, V value, boolean valueNotFound);

    /**
     * @return a slice that only retrieves the value from the storage and skips any computeValue() calls
     */
    ReadOnlySlice<K, V> makeRawValueVersion();
}
