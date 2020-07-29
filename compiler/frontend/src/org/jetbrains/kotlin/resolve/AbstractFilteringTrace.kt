/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

/**
 * Trace which allows to keep some slices hidden from the parent trace.
 *
 * Compared with TemporaryBindingTrace + TraceEntryFilter, FilteringTrace doesn't
 * make extra moves for slices that should be definitely recorded into parent
 * (like storing them in the local map, later re-committing into parent's, etc.)
 */
abstract class AbstractFilteringTrace(
    private val parentTrace: BindingTrace,
    name: String
) : DelegatingBindingTrace(parentTrace.bindingContext, name, true, BindingTraceFilter.ACCEPT_ALL, false) {
    abstract protected fun <K, V> shouldBeHiddenFromParent(slice: WritableSlice<K, V>, key: K): Boolean

    override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
        if (shouldBeHiddenFromParent(slice, key)) super.record(slice, key, value) else parentTrace.record(slice, key, value)
    }

    override fun report(diagnostic: Diagnostic) {
        parentTrace.report(diagnostic)
    }

    override fun wantsDiagnostics(): Boolean = parentTrace.wantsDiagnostics()
}