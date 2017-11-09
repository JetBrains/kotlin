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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.resolve.diagnostics.MutableDiagnosticsWithSuppression;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.util.slicedMap.*;

import java.util.Collection;

public class BindingTraceContext implements BindingTrace {
    // These flags are used for debugging of "Rewrite at slice..." exceptions
    /* package */ final static boolean TRACK_REWRITES = false;
    /* package */ final static boolean TRACK_WITH_STACK_TRACES = true;

    private final MutableSlicedMap map;
    @Nullable private final MutableDiagnosticsWithSuppression mutableDiagnostics;
    @NotNull private final BindingTraceFilter filter;

    private final BindingContext bindingContext = new BindingContext() {

        @NotNull
        @Override
        public Diagnostics getDiagnostics() {
            return mutableDiagnostics != null ? mutableDiagnostics : Diagnostics.Companion.getEMPTY();
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return BindingTraceContext.this.get(slice, key);
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            return BindingTraceContext.this.getKeys(slice);
        }

        @NotNull
        @TestOnly
        @Override
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            return map.getSliceContents(slice);
        }

        @Nullable
        @Override
        public KotlinType getType(@NotNull KtExpression expression) {
            return BindingTraceContext.this.getType(expression);
        }

        @Override
        public void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics) {
            BindingContextUtils.addOwnDataTo(trace, null, commitDiagnostics, map, mutableDiagnostics);
        }
    };

    public BindingTraceContext() {
        this(false);
    }

    public BindingTraceContext(boolean allowSliceRewrite) {
        this(BindingTraceFilter.Companion.getACCEPT_ALL(), allowSliceRewrite);
    }

    public BindingTraceContext(BindingTraceFilter filter, boolean allowSliceRewrite) {
        //noinspection ConstantConditions
        this(TRACK_REWRITES && !allowSliceRewrite ? new TrackingSlicedMap(TRACK_WITH_STACK_TRACES) : new SlicedMapImpl(allowSliceRewrite), filter);
    }


    private BindingTraceContext(@NotNull MutableSlicedMap map, BindingTraceFilter filter) {
        this.map = map;
        this.mutableDiagnostics = !filter.getIgnoreDiagnostics()
                                  ? new MutableDiagnosticsWithSuppression(bindingContext, Diagnostics.Companion.getEMPTY())
                                  : null;
        this.filter = filter;
    }

    @TestOnly
    public static BindingTraceContext createTraceableBindingTrace() {
        return new BindingTraceContext(new TrackingSlicedMap(TRACK_WITH_STACK_TRACES), BindingTraceFilter.Companion.getACCEPT_ALL());
    }

    @Override
    public void report(@NotNull Diagnostic diagnostic) {
        if (mutableDiagnostics == null) {
            return;
        }
        mutableDiagnostics.report(diagnostic);
    }

    public void clearDiagnostics() {
        if (mutableDiagnostics != null) {
            mutableDiagnostics.clear();
        }
    }

    @Override
    public boolean wantsDiagnostics() {
        return mutableDiagnostics != null;
    }

    @NotNull
    @Override
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    @Override
    public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
        map.put(slice, key, value);
    }

    @Override
    public <K> void record(WritableSlice<K, Boolean> slice, K key) {
        record(slice, key, true);
    }

    @Override
    public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
        return map.get(slice, key);
    }

    @NotNull
    @Override
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        return map.getKeys(slice);
    }

    @Nullable
    @Override
    public KotlinType getType(@NotNull KtExpression expression) {
        KotlinTypeInfo typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression);
        return typeInfo != null ? typeInfo.getType() : null;
    }

    @Override
    public void recordType(@NotNull KtExpression expression, @Nullable KotlinType type) {
        KotlinTypeInfo typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression);
        typeInfo = typeInfo != null ? typeInfo.replaceType(type) : TypeInfoFactoryKt.createTypeInfo(type);
        record(BindingContext.EXPRESSION_TYPE_INFO, expression, typeInfo);
    }
}
