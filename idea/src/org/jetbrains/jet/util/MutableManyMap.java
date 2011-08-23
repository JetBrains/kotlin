package org.jetbrains.jet.util;

/**
 * @author abreslav
 */
public interface MutableManyMap extends ManyMap {

    <K, V> void put(WritableSlice<K, V> slice, K key, V value);

    <K, V> V remove(RemovableSlice<K, V> slice, K key);
}
