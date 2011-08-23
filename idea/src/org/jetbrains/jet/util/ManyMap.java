package org.jetbrains.jet.util;

import java.util.Map;

/**
 * @author abreslav
 */
public interface ManyMap extends Iterable<Map.Entry<ManyMapKey<?, ?>, ?>> {
    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    <K, V> boolean containsKey(ReadOnlySlice<K, V> slice, K key);
    
    
}
