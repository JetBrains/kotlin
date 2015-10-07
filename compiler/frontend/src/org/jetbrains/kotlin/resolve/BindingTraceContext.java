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
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.resolve.diagnostics.MutableDiagnosticsWithSuppression;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.JetTypeInfo;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;
import org.jetbrains.kotlin.util.slicedMap.*;

import java.util.Collection;

public class BindingTraceContext implements BindingTrace {
    // These flags are used for debugging of "Rewrite at slice..." exceptions
    /* package */ final static boolean TRACK_REWRITES = false;
    /* package */ final static boolean TRACK_WITH_STACK_TRACES = true;

    private final MutableSlicedMap map;
    private final MutableDiagnosticsWithSuppression mutableDiagnostics;

    private final BindingContext bindingContext = new BindingContext() {

        @NotNull
        @Override
        public Diagnostics getDiagnostics() {
            return mutableDiagnostics;
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
        public JetType getType(@NotNull JetExpression expression) {
            return BindingTraceContext.this.getType(expression);
        }

        @Override
        public void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics) {
            BindingContextUtils.addOwnDataTo(trace, null, commitDiagnostics, map, mutableDiagnostics);
        }
    };

    public BindingTraceContext() {
        //noinspection ConstantConditions
        this(TRACK_REWRITES ? new TrackingSlicedMap(TRACK_WITH_STACK_TRACES) : SlicedMapImpl.create());
    }


    private BindingTraceContext(@NotNull MutableSlicedMap map) {
        this.map = map;
        this.mutableDiagnostics = new MutableDiagnosticsWithSuppression(bindingContext, Diagnostics.Companion.getEMPTY());
    }

    @TestOnly
    public static BindingTraceContext createTraceableBindingTrace() {
        return new BindingTraceContext(new TrackingSlicedMap(TRACK_WITH_STACK_TRACES));
    }

    @Override
    public void report(@NotNull Diagnostic diagnostic) {
        mutableDiagnostics.report(diagnostic);
    }

    public void clearDiagnostics() {
        mutableDiagnostics.clear();
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
    public JetType getType(@NotNull JetExpression expression) {
        JetTypeInfo typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression);
        return typeInfo != null ? typeInfo.getType() : null;
    }

    @Override
    public void recordType(@NotNull JetExpression expression, @Nullable JetType type) {
        JetTypeInfo typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression);
        typeInfo = typeInfo != null ? typeInfo.replaceType(type) : TypeInfoFactoryPackage.createTypeInfo(type);
        record(BindingContext.EXPRESSION_TYPE_INFO, expression, typeInfo);
    }
}
