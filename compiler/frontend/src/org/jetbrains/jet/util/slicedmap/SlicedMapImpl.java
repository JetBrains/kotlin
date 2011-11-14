package org.jetbrains.jet.util.slicedmap;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
* @author abreslav
*/
public class SlicedMapImpl implements MutableSlicedMap {

    public static SlicedMapImpl create() {
        return new SlicedMapImpl(Maps.<SlicedMapKey<?, ?>, Object>newLinkedHashMap());
    }

    public static SlicedMapImpl create(Map<SlicedMapKey<?, ?>, Object> map) {
        return new SlicedMapImpl(map);
    }

    public static SlicedMapImpl create(MapSupplier mapSupplier) {
        return new SlicedMapImpl(mapSupplier.<SlicedMapKey<?, ?>, Object>get());
    }
    
    private final Map<SlicedMapKey<?, ?>, Object> map;
    private final Multimap<WritableSlice<?, ?>, Object> collectiveSliceKeys = Multimaps.newListMultimap(new HashMap<WritableSlice<?, ?>, Collection<Object>>(), CommonSuppliers.getArrayListSupplier());

    private SlicedMapImpl(Map<SlicedMapKey<?, ?>, Object> map) {
        this.map = map;
    }

    @Override
    public <K, V> void put(WritableSlice<K, V> slice, K key, V value) {
        if (!slice.check(key, value)) {
            return;
        }

        SlicedMapKey<K, V> slicedMapKey = slice.makeKey(key);
        RewritePolicy rewritePolicy = slice.getRewritePolicy();
        if (rewritePolicy.rewriteProcessingNeeded(key)) {
            if (map.containsKey(slicedMapKey)) {
                //noinspection unchecked
                if (!rewritePolicy.processRewrite(slice, key, (V) map.get(slicedMapKey), value)) {
                    return;
                }
            }
        }

        if (slice.isCollective()) {
            collectiveSliceKeys.put(slice, key);
        }

        map.put(slicedMapKey, value);
        slice.afterPut(this, key, value);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
        SlicedMapKey<K, V> slicedMapKey = slice.makeKey(key);
        //noinspection unchecked
        V value = (V) map.get(slicedMapKey);
        return slice.computeValue(this, key, value, value == null && !map.containsKey(slicedMapKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        assert slice.isCollective() : "Keys are not collected for slice " + slice;
        return (Collection<K>) collectiveSliceKeys.get(slice);
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
    public Iterator<Map.Entry<SlicedMapKey<?, ?>, ?>> iterator() {
        //noinspection unchecked
        return (Iterator) map.entrySet().iterator();
    }
}