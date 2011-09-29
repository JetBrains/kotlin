package org.jetbrains.jet.util.slicedmap;

import com.google.common.base.Supplier;

import java.util.Collection;

/**
 * @author abreslav
 */
public class CollectionSliceWrapper<K, V> implements WritableSlice<K, V> {
    
    private final WritableSlice<K, Collection<V>> wrapped;
    private final SlicedMapKey<K, V> myKey;
    private final Supplier<? extends Collection<V>> supplier;

    public CollectionSliceWrapper(WritableSlice<K, Collection<V>> wrapped, Supplier<? extends Collection<V>> supplier) {
        this.wrapped = wrapped;
        this.supplier = supplier;
        this.myKey = new SlicedMapKey<K, V>(this, null);
    }

    @Override
    public SlicedMapKey<K, V> makeKey(K key) {
        return myKey;
    }

    @Override
    public boolean check(K key, V value) {
        assert value != null;
        return true;
    }

    @Override
    public void afterPut(MutableSlicedMap map, K key, V value) {
        Collection<V> collection = map.get(wrapped, key);
        if (collection == null) {
            collection = supplier.get();
            map.put(wrapped, key, collection);
        }
        collection.add(value);
    }

    @Override
    public RewritePolicy getRewritePolicy() {
        return RewritePolicy.DO_NOTHING;
    }

    @Override
    public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
        throw new UnsupportedOperationException("Don't read by this slice, use the wrapped one");
    }

    @Override
    public ReadOnlySlice<K, V> makeRawValueVersion() {
        return this;
    }
}