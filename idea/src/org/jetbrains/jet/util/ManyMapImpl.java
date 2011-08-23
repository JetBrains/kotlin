package org.jetbrains.jet.util;

import com.google.common.collect.Maps;

import java.util.Iterator;
import java.util.Map;

/**
* @author abreslav
*/
public class ManyMapImpl implements MutableManyMap {

    public static ManyMapImpl create() {
        return new ManyMapImpl(Maps.<ManyMapKey<?, ?>, Object>newLinkedHashMap());
    }

    public static ManyMapImpl create(Map<ManyMapKey<?, ?>, Object> map) {
        return new ManyMapImpl(map);
    }

    public static ManyMapImpl create(MapSupplier mapSupplier) {
        return new ManyMapImpl(mapSupplier.<ManyMapKey<?, ?>, Object>get());
    }
    
    private final Map<ManyMapKey<?, ?>, Object> map;

    private ManyMapImpl(Map<ManyMapKey<?, ?>, Object> map) {
        this.map = map;
    }

    @Override
    public <K, V> void put(WritableSlice<K, V> slice, K key, V value) {
        if (!slice.check(key, value)) {
            return;
        }
        ManyMapKey<K, V> manyMapKey = slice.makeKey(key);
        if (slice.getRewritePolicy().rewriteProcessingNeeded(key)) {
            if (map.containsKey(manyMapKey)) {
                //noinspection unchecked
                if (!slice.getRewritePolicy().processRewrite(slice, key, (V) map.get(manyMapKey), value)) {
                    return;
                }
            }
        }
        map.put(manyMapKey, value);
        slice.afterPut(this, key, value);
    }

    @Override
    public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
        ManyMapKey<K, V> manyMapKey = slice.makeKey(key);
        //noinspection unchecked
        V value = (V) map.get(manyMapKey);
        return slice.computeValue(this, key, value, value == null && !map.containsKey(manyMapKey));
    }

    @Override
    public <K, V> boolean containsKey(ReadOnlySlice<K, V> slice, K key) {
        return map.containsKey(slice.makeKey(key));
    }

    @Override
    public <K, V> V remove(RemovableSlice<K, V> slice, K key) {
        //noinspection unchecked
        return (V) map.remove(slice.makeKey(key));
    }

    @Override
    public Iterator<Map.Entry<ManyMapKey<?, ?>, ?>> iterator() {
        //noinspection unchecked
        return (Iterator) map.entrySet().iterator();
    }
}
