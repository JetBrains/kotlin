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

package org.jetbrains.kotlin.resolve

import com.google.common.collect.ImmutableMap
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.diagnostics.MutableDiagnosticsWithSuppression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import org.jetbrains.kotlin.util.slicedMap.*

open class DelegatingBindingTrace(
        private val parentContext: BindingContext,
        private val name: String,
        withParentDiagnostics: Boolean = true,
        private val filter: BindingTraceFilter = BindingTraceFilter.ACCEPT_ALL,
        allowSliceRewrite: Boolean = false
) : BindingTrace {
    
    private val map = if (BindingTraceContext.TRACK_REWRITES && !allowSliceRewrite)
        TrackingSlicedMap(BindingTraceContext.TRACK_WITH_STACK_TRACES)
    else
        SlicedMapImpl(allowSliceRewrite)

    private val mutableDiagnostics: MutableDiagnosticsWithSuppression?

    private inner class MyBindingContext : BindingContext {
        override fun getDiagnostics(): Diagnostics = mutableDiagnostics ?: Diagnostics.EMPTY

        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
            return this@DelegatingBindingTrace.get(slice, key)
        }

        override fun getType(expression: KtExpression): KotlinType? {
            return this@DelegatingBindingTrace.getType(expression)
        }

        override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
            return this@DelegatingBindingTrace.getKeys(slice)
        }

        override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
            BindingContextUtils.addOwnDataTo(trace, null, commitDiagnostics, map, mutableDiagnostics)
        }

        @TestOnly
        override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
            return ImmutableMap.copyOf(parentContext.getSliceContents(slice) + map.getSliceContents(slice))
        }
    }

    private val bindingContext = MyBindingContext()

    init {
        this.mutableDiagnostics = if (filter.ignoreDiagnostics)
            null
        else if (withParentDiagnostics)
            MutableDiagnosticsWithSuppression(bindingContext, parentContext.diagnostics)
        else
            MutableDiagnosticsWithSuppression(bindingContext)
    }

    constructor(parentContext: BindingContext,
                debugName: String,
                resolutionSubjectForMessage: Any?,
                filter: BindingTraceFilter = BindingTraceFilter.ACCEPT_ALL,
                allowSliceRewrite: Boolean = false
    ) : this(parentContext,
             AnalyzingUtils.formDebugNameForBindingTrace(debugName, resolutionSubjectForMessage),
             filter = filter,
             allowSliceRewrite = allowSliceRewrite)

    override fun getBindingContext(): BindingContext = bindingContext

    override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
        map.put(slice, key, value)
    }

    override fun <K> record(slice: WritableSlice<K, Boolean>, key: K) {
        record(slice, key, true)
    }

    override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        val value = map.get(slice, key)
        if (slice is SetSlice<*>) {
            assert(value != null)
            if (value != SetSlice.DEFAULT) return value
        }
        else if (value != null) {
            return value
        }

        return parentContext.get(slice, key)
    }

    override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        val keys = map.getKeys(slice)
        val fromParent = parentContext.getKeys(slice)
        if (keys.isEmpty()) return fromParent
        if (fromParent.isEmpty()) return keys

        return keys + fromParent
    }

    override fun getType(expression: KtExpression): KotlinType? {
        val typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression)
        return typeInfo?.type
    }

    override fun recordType(expression: KtExpression, type: KotlinType?) {
        var typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression)
        if (typeInfo == null) {
            typeInfo = createTypeInfo(type)
        }
        else {
            typeInfo = typeInfo.replaceType(type)
        }
        record(BindingContext.EXPRESSION_TYPE_INFO, expression, typeInfo)
    }

    fun moveAllMyDataTo(trace: BindingTrace) {
        addOwnDataTo(trace, null, true)
        clear()
    }

    @JvmOverloads fun addOwnDataTo(trace: BindingTrace, filter: TraceEntryFilter? = null, commitDiagnostics: Boolean = true) {
        BindingContextUtils.addOwnDataTo(trace, filter, commitDiagnostics, map, mutableDiagnostics)
    }

    fun clear() {
        map.clear()
        mutableDiagnostics?.clear()
    }

    override fun report(diagnostic: Diagnostic) {
        if (mutableDiagnostics == null) {
            return
        }
        mutableDiagnostics.report(diagnostic)
    }

    override fun wantsDiagnostics(): Boolean = mutableDiagnostics != null

    override fun toString(): String = name
}
