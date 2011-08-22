package org.jetbrains.jet.util;

/**
 * @author abreslav
 */
public interface WritableSlice<K, V> extends ReadOnlySlice<K, V> {
    // True to put, false to skip
    boolean check(K key, V value);

    void afterPut(MutableManyMap manyMap, K key, V value);

    RewritePolicy getRewritePolicy();
}
