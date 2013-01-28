/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.util;

import com.google.common.base.Supplier;
import com.google.common.collect.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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

    private static final Supplier<?> HASH_SET_SUPPLIER = new Supplier() {
        @Override
        public Set get() {
            return Sets.newHashSet();
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

    public static <T> Supplier<Set<T>> getHashSetSupplier() {
        //noinspection unchecked
        return (Supplier<Set<T>>) HASH_SET_SUPPLIER;
    }

    public static <K, V> SetMultimap<K, V> newLinkedHashSetHashSetMultimap() {
        return Multimaps.newSetMultimap(Maps.<K, Collection<V>>newHashMap(), CommonSuppliers.<V>getLinkedHashSetSupplier());
    }
}
