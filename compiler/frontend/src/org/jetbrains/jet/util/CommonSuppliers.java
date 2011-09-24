package org.jetbrains.jet.util;

import com.google.common.base.Supplier;
import com.google.common.collect.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class CommonSuppliers {
    private CommonSuppliers() {}

    private static final Supplier<?> ARRAY_LIST_SUPPLIER = new Supplier() {
        @Override
        public List get() {
            return Lists.newArrayList();
        }
    };

    private static final Supplier<?> LINKED_HASH_SET_SUPPLIER = new Supplier() {
        @Override
        public Set get() {
            return Sets.newLinkedHashSet();
        }
    };

    public static <T> Supplier<List<T>> getArrayListSupplier() {
        //noinspection unchecked
        return (Supplier<List<T>>) ARRAY_LIST_SUPPLIER;
    }

    public static <T> Supplier<Set<T>> getLinkedHashSetSupplier() {
        //noinspection unchecked
        return (Supplier<Set<T>>) LINKED_HASH_SET_SUPPLIER;
    }
    
    public static <K, V> SetMultimap<K, V> newLinkedHashSetHashSetMultimap() {
        return Multimaps.newSetMultimap(Maps.<K, Collection<V>>newHashMap(), CommonSuppliers.<V>getLinkedHashSetSupplier());
    }
}
