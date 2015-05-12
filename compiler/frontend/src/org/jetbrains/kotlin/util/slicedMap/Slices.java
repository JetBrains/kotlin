/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.util.slicedMap;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class Slices {
    private static final Logger LOG = Logger.getInstance(Slices.class);

    public static final RewritePolicy ONLY_REWRITE_TO_EQUAL = new RewritePolicy() {
        @Override
        public <K> boolean rewriteProcessingNeeded(K key) {
            return true;
        }

        @Override
        public <K, V> boolean processRewrite(WritableSlice<K, V> slice, K key, V oldValue, V newValue) {
            if (!((oldValue == null && newValue == null) || (oldValue != null && oldValue.equals(newValue)))) {
                // NOTE: Use BindingTraceContext.TRACK_REWRITES to debug this exception
                LOG.error("Rewrite at slice " + slice +
                        " key: " + key +
                        " old value: " + oldValue + '@' + System.identityHashCode(oldValue) +
                        " new value: " + newValue + '@' + System.identityHashCode(newValue));
            }
            return true;
        }
    };

    private Slices() {
    }

    public interface KeyNormalizer<K> {
        KeyNormalizer DO_NOTHING = new KeyNormalizer<Object>() {
            @Override
            public Object normalize(Object key) {
                return key;
            }
        };

        K normalize(K key);
    }

    public static <K, V> SliceBuilder<K, V> sliceBuilder() {
        return new SliceBuilder<K, V>(ONLY_REWRITE_TO_EQUAL);
    }

    public static <K, V> WritableSlice<K, V> createSimpleSlice() {
        return new BasicWritableSlice<K, V>(ONLY_REWRITE_TO_EQUAL);
    }

    public static <K, V> WritableSlice<K, V> createCollectiveSlice() {
        return new BasicWritableSlice<K, V>(ONLY_REWRITE_TO_EQUAL, true);
    }

    public static <K> WritableSlice<K, Boolean> createSimpleSetSlice() {
        return createRemovableSetSlice();
    }

    public static <K> WritableSlice<K, Boolean> createCollectiveSetSlice() {
        return new SetSlice<K>(RewritePolicy.DO_NOTHING, true);
    }

    public static <K> RemovableSlice<K, Boolean> createRemovableSetSlice() {
        return new SetSlice<K>(RewritePolicy.DO_NOTHING, false);
    }

    public static class SliceBuilder<K, V> {
        private V defaultValue = null;
        private List<ReadOnlySlice<K, V>> furtherLookupSlices = null;
        private WritableSlice<? super V, ? super K> opposite = null;
        private KeyNormalizer<K> keyNormalizer = null;

        private final RewritePolicy rewritePolicy;

        private String debugName;

        private SliceBuilder(RewritePolicy rewritePolicy) {
            this.rewritePolicy = rewritePolicy;
        }

        public SliceBuilder<K, V> setDefaultValue(V defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public SliceBuilder<K, V> setFurtherLookupSlices(ReadOnlySlice<K, V>... furtherLookupSlices) {
            this.furtherLookupSlices = Arrays.asList(furtherLookupSlices);
            return this;
        }

        public SliceBuilder<K, V> setOpposite(WritableSlice<? super V, ? super K> opposite) {
            this.opposite = opposite;
            return this;
        }

        public SliceBuilder<K, V> setDebugName(@NotNull String debugName) {
            this.debugName = debugName;
            return this;
        }

        public SliceBuilder<K, V> setKeyNormalizer(KeyNormalizer<K> keyNormalizer) {
            this.keyNormalizer = keyNormalizer;
            return this;
        }

        public RemovableSlice<K, V>  build() {
            SliceWithOpposite<K, V> result = doBuild();
            if (debugName != null) {
                result.setDebugName(debugName);
            }
            return result;
        }

        private SliceWithOpposite<K, V> doBuild() {
            if (defaultValue != null) {
                return new SliceWithOpposite<K, V>(rewritePolicy, opposite, keyNormalizer) {
                    @Override
                    public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
                        if (valueNotFound) return defaultValue;
                        return super.computeValue(map, key, value, false);
                    }
                };
            }
            if (furtherLookupSlices != null) {
                return new SliceWithOpposite<K, V>(rewritePolicy, opposite, keyNormalizer) {
                    @Override
                    public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
                        if (valueNotFound) {
                            for (ReadOnlySlice<K, V> slice : furtherLookupSlices) {
                                V v = map.get(slice, key);
                                if (v != null) {
                                    return v;
                                }
                            }
                            return defaultValue;
                        }
                        return super.computeValue(map, key, value, false);
                    }
                };
            }
            return new SliceWithOpposite<K, V>(rewritePolicy, opposite, keyNormalizer);
        }
    }

    public static class BasicRemovableSlice<K, V> extends BasicWritableSlice<K, V> implements RemovableSlice<K, V> {
        protected BasicRemovableSlice(RewritePolicy rewritePolicy) {
            super(rewritePolicy);
        }

        protected BasicRemovableSlice(RewritePolicy rewritePolicy, boolean isCollective) {
            super(rewritePolicy, isCollective);
        }
    }

    public static class SliceWithOpposite<K, V> extends BasicRemovableSlice<K, V> {
        private final WritableSlice<? super V, ? super K> opposite;
        private final KeyNormalizer<K> keyNormalizer;

        public SliceWithOpposite(RewritePolicy rewritePolicy, WritableSlice<? super V, ? super K> opposite, KeyNormalizer<K> keyNormalizer) {
            super(rewritePolicy);
            this.opposite = opposite;
            this.keyNormalizer = keyNormalizer;
        }

        @Override
        public void afterPut(MutableSlicedMap map, K key, V value) {
            if (opposite != null) {
                map.put(opposite, value, key);
            }
        }
    }

    public static class SetSlice<K> extends BasicRemovableSlice<K, Boolean> {

        protected SetSlice(RewritePolicy rewritePolicy) {
            this(rewritePolicy, false);
        }

        protected SetSlice(RewritePolicy rewritePolicy, boolean collective) {
            super(rewritePolicy, collective);
        }

        @Override
        public Boolean computeValue(SlicedMap map, K key, Boolean value, boolean valueNotFound) {
            Boolean result = super.computeValue(map, key, value, valueNotFound);
            return result != null ? result : false;
        }
    }

}
