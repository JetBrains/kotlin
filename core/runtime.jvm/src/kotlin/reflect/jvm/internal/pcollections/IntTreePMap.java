/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal.pcollections;

/**
 * An efficient persistent map from integer keys to non-null values.
 */
final class IntTreePMap<V> {
    private static final IntTreePMap<Object> EMPTY = new IntTreePMap<Object>(IntTree.EMPTYNODE);

    @SuppressWarnings("unchecked")
    public static <V> IntTreePMap<V> empty() {
        return (IntTreePMap<V>) EMPTY;
    }

    private final IntTree<V> root;

    private IntTreePMap(IntTree<V> root) {
        this.root = root;
    }

    private IntTreePMap<V> withRoot(IntTree<V> root) {
        if (root == this.root) return this;
        return new IntTreePMap<V>(root);
    }

    public V get(int key) {
        return root.get(key);
    }

    public IntTreePMap<V> plus(int key, V value) {
        return withRoot(root.plus(key, value));
    }

    public IntTreePMap<V> minus(int key) {
        return withRoot(root.minus(key));
    }
}
