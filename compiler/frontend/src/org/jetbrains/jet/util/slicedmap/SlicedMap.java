package org.jetbrains.jet.util.slicedmap;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * @author abreslav
 */
public interface SlicedMap extends Iterable<Map.Entry<SlicedMapKey<?, ?>, ?>> {

    SlicedMap DO_NOTHING = new SlicedMap() {
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return slice.computeValue(this, key, null, true);
        }

        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            return Collections.emptySet();
        }

        @Override
        public Iterator<Map.Entry<SlicedMapKey<?, ?>, ?>> iterator() {
            return Collections.<Map.Entry<SlicedMapKey<?, ?>, ?>>emptySet().iterator();
        }
    };

    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    // slice.isCollective() must return true
    <K, V> Collection<K> getKeys(WritableSlice<K, V> slice);
}
