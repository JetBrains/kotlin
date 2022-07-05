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
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactoryKt;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.kotlin.types.TypeUtils;

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
                logErrorAboutRewritingNonEqualObjects(slice, key, oldValue, newValue);
            }
            return true;
        }
    };

    // Rewrite is allowed for equal objects and for signed constant values that were converted to unsigned ones
    // This is needed to avoid making `CompileTimeConstant` mutable
    public static final RewritePolicy COMPILE_TIME_VALUE_REWRITE_POLICY = new RewritePolicy() {
        @Override
        public <K> boolean rewriteProcessingNeeded(K key) {
            return true;
        }

        @Override
        public <K, V> boolean processRewrite(WritableSlice<K, V> slice, K key, V oldValue, V newValue) {
            if ((oldValue == null && newValue == null) || (oldValue != null && oldValue.equals(newValue))) return true;

            if (oldValue instanceof IntegerValueTypeConstant && newValue instanceof IntegerValueTypeConstant) {
                IntegerValueTypeConstant oldConstant = (IntegerValueTypeConstant) oldValue;
                IntegerValueTypeConstant newConstant = (IntegerValueTypeConstant) newValue;

                if (oldConstant.getParameters().isPure() && newConstant.getParameters().isUnsignedNumberLiteral()) {
                    long oldConstantValue = oldConstant.getValue(TypeUtils.NO_EXPECTED_TYPE).longValue();
                    Number newConstantValue = newConstant.getValue(TypeUtils.NO_EXPECTED_TYPE);
                    if (oldConstantValue == newConstantValue.longValue() ||
                        oldConstantValue == ConstantValueFactoryKt.fromUIntToLong(newConstantValue.intValue()) ||
                        oldConstantValue == ConstantValueFactoryKt.fromUByteToLong(newConstantValue.byteValue()) ||
                        oldConstantValue == ConstantValueFactoryKt.fromUShortToLong(newConstantValue.shortValue())
                    ) {
                        return true;
                    }
                }
            }

            logErrorAboutRewritingNonEqualObjects(slice, key, oldValue, newValue);

            return true;
        }
    };

    private Slices() {
    }

    public static <K, V> SliceBuilder<K, V> sliceBuilder() {
        return new SliceBuilder<>(ONLY_REWRITE_TO_EQUAL);
    }

    public static <K, V> WritableSlice<K, V> createSimpleSlice() {
        return new BasicWritableSlice<>(ONLY_REWRITE_TO_EQUAL);
    }

    public static <K, V> WritableSlice<K, V> createCollectiveSlice() {
        return new BasicWritableSlice<>(ONLY_REWRITE_TO_EQUAL, true);
    }

    public static <K> WritableSlice<K, Boolean> createSimpleSetSlice() {
        return new SetSlice<>(RewritePolicy.DO_NOTHING);
    }

    public static <K> WritableSlice<K, Boolean> createCollectiveSetSlice() {
        return new SetSlice<>(RewritePolicy.DO_NOTHING, true);
    }

    public static class SliceBuilder<K, V> {
        private List<ReadOnlySlice<K, V>> furtherLookupSlices;
        private final RewritePolicy rewritePolicy;
        private String debugName;

        private SliceBuilder(RewritePolicy rewritePolicy) {
            this.rewritePolicy = rewritePolicy;
        }

        @SafeVarargs
        public final SliceBuilder<K, V> setFurtherLookupSlices(ReadOnlySlice<K, V>... furtherLookupSlices) {
            this.furtherLookupSlices = Arrays.asList(furtherLookupSlices);
            return this;
        }

        public SliceBuilder<K, V> setDebugName(@NotNull String debugName) {
            this.debugName = debugName;
            return this;
        }

        public WritableSlice<K, V> build() {
            BasicWritableSlice<K, V> result = doBuild();
            if (debugName != null) {
                result.setDebugName(debugName);
            }
            return result;
        }

        private BasicWritableSlice<K, V> doBuild() {
            if (furtherLookupSlices != null) {
                return new BasicWritableSlice<K, V>(rewritePolicy) {
                    @Override
                    public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
                        if (valueNotFound) {
                            for (ReadOnlySlice<K, V> slice : furtherLookupSlices) {
                                V v = map.get(slice, key);
                                if (v != null) {
                                    return v;
                                }
                            }
                            return null;
                        }
                        return super.computeValue(map, key, value, false);
                    }
                };
            }
            return new BasicWritableSlice<>(rewritePolicy);
        }
    }

    private static <K, V> void logErrorAboutRewritingNonEqualObjects(WritableSlice<K, V> slice, K key, V oldValue, V newValue) {
        // NOTE: Use BindingTraceContext.TRACK_REWRITES to debug this exception
        LOG.error("Rewrite at slice " + slice +
                  " key: " + key +
                  " old value: " + oldValue + '@' + System.identityHashCode(oldValue) +
                  " new value: " + newValue + '@' + System.identityHashCode(newValue) +
                  (key instanceof KtElement ? "\n" + PsiUtilsKt.getElementTextWithContext((KtElement) key) : ""));
    }
}
