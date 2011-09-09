package org.jetbrains.jet.util;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

import java.util.List;

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

    public static <T> Supplier<List<T>> getArrayListSupplier() {
        //noinspection unchecked
        return (Supplier<List<T>>) ARRAY_LIST_SUPPLIER;
    }
}
