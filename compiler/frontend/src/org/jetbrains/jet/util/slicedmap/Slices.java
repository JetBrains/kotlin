package org.jetbrains.jet.util.slicedmap;

import java.util.Arrays;
import java.util.List;

/**
 * @author abreslav
 */
public class Slices {

    public static final RewritePolicy ONLY_REWRITE_TO_EQUAL = new RewritePolicy() {
        @Override
        public <K> boolean rewriteProcessingNeeded(K key) {
            return true;
        }

        @Override
        public <K, V> boolean processRewrite(WritableSlice<K, V> slice, K key, V oldValue, V newValue) {
            assert (oldValue == null && newValue == null) || (oldValue != null && oldValue.equals(newValue))
                    : "Rewrite at slice " + slice + " key: " + key + " old value: " + oldValue + " new value: " + newValue;
            return true;
        }
    };

    public interface KeyNormalizer<K> {

        KeyNormalizer DO_NOTHING = new KeyNormalizer<Object>() {
            @Override
            public Object normalize(Object key) {
                return key;
            }
        };
        K normalize(K key);

    }

    public static <K, V> SliceBuilder<K, V> sliceBuilder(String debugName) {
        return new SliceBuilder<K, V>(debugName, ONLY_REWRITE_TO_EQUAL);
    }

    public static <K, V> WritableSlice<K, V> createSimpleSlice(String debugName) {
        return new BasicWritableSlice<K, V>(debugName, ONLY_REWRITE_TO_EQUAL);
    }

    public static <K> WritableSlice<K, Boolean> createSimpleSetSlice(String debugName) {
        return createRemovableSetSlice(debugName);
    }
    public static <K> RemovableSlice<K, Boolean> createRemovableSetSlice(String debugName) {
        return new SetSlice<K>(debugName, RewritePolicy.DO_NOTHING);
    }

    public static class SliceBuilder<K, V> {
        private V defaultValue = null;
        private List<ReadOnlySlice<K, V>> furtherLookupSlices = null;
        private WritableSlice<? super V, ? super K> opposite = null;
        private KeyNormalizer<K> keyNormalizer = null;

        private RewritePolicy rewritePolicy;

        private String debugName;

        private SliceBuilder(String debugName, RewritePolicy rewritePolicy) {
            this.rewritePolicy = rewritePolicy;
            this.debugName = debugName;
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

        public SliceBuilder<K, V> setKeyNormalizer(KeyNormalizer<K> keyNormalizer) {
            this.keyNormalizer = keyNormalizer;
            return this;
        }

        public RemovableSlice<K, V>  build() {
            if (defaultValue != null) {
                return new SliceWithOpposite<K, V>(debugName, rewritePolicy, opposite, keyNormalizer) {
                    @Override
                    public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
                        if (valueNotFound) return defaultValue;
                        return super.computeValue(map, key, value, valueNotFound);
                    }
                };
            }
            if (furtherLookupSlices != null) {
                return new SliceWithOpposite<K, V>(debugName, rewritePolicy, opposite, keyNormalizer) {
                    @Override
                    public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
                        if (valueNotFound) {
                            for (ReadOnlySlice<K, V> slice : furtherLookupSlices) {
                                if (map.containsKey(slice, key)) {
                                    return map.get(slice, key);
                                }
                            }
                            return defaultValue;
                        }
                        return super.computeValue(map, key, value, valueNotFound);
                    }
                };
            }
            return new SliceWithOpposite<K, V>(debugName, rewritePolicy, opposite, keyNormalizer);
        }

    }

    public static class BasicRemovableSlice<K, V> extends BasicWritableSlice<K, V> implements RemovableSlice<K, V> {
        protected BasicRemovableSlice(String debugName, RewritePolicy rewritePolicy) {
            super(debugName, rewritePolicy);
        }

    }

    public static class SliceWithOpposite<K, V> extends BasicRemovableSlice<K, V> {
        private final WritableSlice<? super V, ? super K> opposite;


        private final KeyNormalizer<K> keyNormalizer;

        public SliceWithOpposite(String debugName, RewritePolicy rewritePolicy) {
            this(debugName, rewritePolicy, KeyNormalizer.DO_NOTHING);
        }

        public SliceWithOpposite(String debugName, RewritePolicy rewritePolicy, KeyNormalizer<K> keyNormalizer) {
            this(debugName, rewritePolicy, null, keyNormalizer);
        }

        public SliceWithOpposite(String debugName, RewritePolicy rewritePolicy, WritableSlice<? super V, ? super K> opposite, KeyNormalizer<K> keyNormalizer) {
            super(debugName, rewritePolicy);
            this.opposite = opposite;
            this.keyNormalizer = keyNormalizer;
        }

        @Override
        public void afterPut(MutableSlicedMap map, K key, V value) {
            if (opposite != null) {
                map.put(opposite, value, key);
            }
        }
        @Override
        public SlicedMapKey<K, V> makeKey(K key) {
            if (keyNormalizer == null) {
                return super.makeKey(key);
            }
            return super.makeKey(keyNormalizer.normalize(key));
        }

    }

    public static class SetSlice<K> extends BasicRemovableSlice<K, Boolean> {


        protected SetSlice(String debugName, RewritePolicy rewritePolicy) {
            super(debugName, rewritePolicy);
        }
        @Override
        public Boolean computeValue(SlicedMap map, K key, Boolean value, boolean valueNotFound) {
            if (valueNotFound) return false;
            return super.computeValue(map, key, value, valueNotFound);
        }

    }

}
