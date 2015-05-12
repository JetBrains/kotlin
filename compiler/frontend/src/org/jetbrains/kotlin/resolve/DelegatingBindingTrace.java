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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import kotlin.Function3;
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
import java.util.List;
import java.util.Map;

public class DelegatingBindingTrace implements BindingTrace {
    @SuppressWarnings("ConstantConditions")
    private final MutableSlicedMap map = BindingTraceContext.TRACK_REWRITES ? new TrackingSlicedMap(BindingTraceContext.TRACK_WITH_STACK_TRACES) : SlicedMapImpl.create();

    private final BindingContext parentContext;
    private final String name;
    private final MutableDiagnosticsWithSuppression mutableDiagnostics;

    private final BindingContext bindingContext = new BindingContext() {
        @NotNull
        @Override
        public Diagnostics getDiagnostics() {
            return mutableDiagnostics;
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return DelegatingBindingTrace.this.get(slice, key);
        }


        @Nullable
        @Override
        public JetType getType(@NotNull JetExpression expression) {
            return DelegatingBindingTrace.this.getType(expression);
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            return DelegatingBindingTrace.this.getKeys(slice);

        }

        @NotNull
        @TestOnly
        @Override
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            Map<K, V> result = Maps.newHashMap();
            result.putAll(parentContext.getSliceContents(slice));
            result.putAll(map.getSliceContents(slice));
            return ImmutableMap.copyOf(result);
        }
    };

    public DelegatingBindingTrace(BindingContext parentContext, String debugName) {
        this.parentContext = parentContext;
        this.name = debugName;
        this.mutableDiagnostics = new MutableDiagnosticsWithSuppression(bindingContext, parentContext.getDiagnostics());
    }

    public DelegatingBindingTrace(BindingContext parentContext, String debugName, @Nullable Object resolutionSubjectForMessage) {
        this(parentContext, AnalyzingUtils.formDebugNameForBindingTrace(debugName, resolutionSubjectForMessage));
    }

    @Override
    @NotNull
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
        V value = map.get(slice, key);
        if (slice instanceof Slices.SetSlice) {
            assert value != null;
            if (value.equals(true)) return value;
        }
        else if (value != null) {
            return value;
        }

        return parentContext.get(slice, key);
    }

    @NotNull
    @Override
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        Collection<K> keys = map.getKeys(slice);
        Collection<K> fromParent = parentContext.getKeys(slice);
        if (keys.isEmpty()) return fromParent;
        if (fromParent.isEmpty()) return keys;

        List<K> result = Lists.newArrayList(keys);
        result.addAll(fromParent);
        return result;
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
        if (typeInfo == null) {
            typeInfo = TypeInfoFactoryPackage.createTypeInfo(type);
        }
        else {
            typeInfo = typeInfo.replaceType(type);
        }
        record(BindingContext.EXPRESSION_TYPE_INFO, expression, typeInfo);
    }

    public void addAllMyDataTo(@NotNull BindingTrace trace) {
        addAllMyDataTo(trace, null, true);
    }

    public void moveAllMyDataTo(@NotNull BindingTrace trace) {
        addAllMyDataTo(trace, null, true);
        clear();
    }

    public void addAllMyDataTo(@NotNull final BindingTrace trace, @Nullable final TraceEntryFilter filter, boolean commitDiagnostics) {
        map.forEach(new Function3<WritableSlice, Object, Object, Void>() {
            @Override
            public Void invoke(WritableSlice slice, Object key, Object value) {
                if (filter == null || filter.accept(slice, key)) {
                    trace.record(slice, key, value);
                }

                return null;
            }
        });

        if (!commitDiagnostics) return;

        for (Diagnostic diagnostic : mutableDiagnostics.getOwnDiagnostics()) {
            if (filter == null || filter.accept(null, diagnostic.getPsiElement())) {
                trace.report(diagnostic);
            }
        }
    }

    public void clear() {
        map.clear();
        mutableDiagnostics.clear();
    }

    @Override
    public void report(@NotNull Diagnostic diagnostic) {
        mutableDiagnostics.report(diagnostic);
    }

    @Override
    public String toString() {
        return name;
    }
}
