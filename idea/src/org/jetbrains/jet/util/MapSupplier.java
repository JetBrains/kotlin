package org.jetbrains.jet.util;

import com.google.common.collect.Maps;

import java.util.Map;

/**
* @author abreslav
*/
public interface MapSupplier {

    MapSupplier HASH_MAP_SUPPLIER = new MapSupplier() {
        @Override
        public <K, V> Map<K, V> get() {
            return Maps.newHashMap();
        }
    };

    MapSupplier LINKED_HASH_MAP_SUPPLIER = new MapSupplier() {
        @Override
        public <K, V> Map<K, V> get() {
            return Maps.newLinkedHashMap();
        }
    };

    <K, V> Map<K, V> get();
}
