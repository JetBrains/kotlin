package org.jetbrains.jet.util.slicedmap;

/**
 * @author abreslav
 */
public interface ReadOnlySlice<K, V> {
    SlicedMapKey<K, V> makeKey(K key);

    V computeValue(SlicedMap map, K key, V value, boolean valueNotFound);

}
