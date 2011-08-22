package org.jetbrains.jet.util;

/**
 * @author abreslav
 */
public interface ReadOnlySlice<K, V> {
    ManyMapKey<K, V> makeKey(K key);

    V computeValue(ManyMap map, K key, V value, boolean valueNotFound);

}
