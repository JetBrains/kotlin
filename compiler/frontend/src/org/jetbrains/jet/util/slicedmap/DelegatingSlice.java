package org.jetbrains.jet.util.slicedmap;

import org.jetbrains.annotations.NotNull;

/**
 * Do nothing but dispatching all invokes to internal writable slice.
 *
 * @author abreslav
 */
public class DelegatingSlice<K, V> implements WritableSlice<K, V> {
    private final WritableSlice<K, V> delegate;

    public DelegatingSlice(@NotNull WritableSlice<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isCollective() {
        return delegate.isCollective();
    }

    @Override
    public boolean check(K key, V value) {
        return delegate.check(key, value);
    }

    @Override
    public void afterPut(MutableSlicedMap map, K key, V value) {
        delegate.afterPut(map, key, value);
    }

    @Override
    public RewritePolicy getRewritePolicy() {
        return delegate.getRewritePolicy();
    }

    @Override
    public SlicedMapKey<K, V> makeKey(K key) {
        return delegate.makeKey(key);
    }

    @Override
    public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
        return delegate.computeValue(map, key, value, valueNotFound);
    }

    @Override
    public ReadOnlySlice<K, V> makeRawValueVersion() {
        return delegate.makeRawValueVersion();
    }
}
