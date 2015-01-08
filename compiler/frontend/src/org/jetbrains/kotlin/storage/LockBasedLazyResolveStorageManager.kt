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

package org.jetbrains.kotlin.storage

import com.intellij.util.containers.ConcurrentWeakValueHashMap
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice
import org.jetbrains.jet.util.slicedmap.WritableSlice
import org.jetbrains.jet.lang.resolve.diagnostics.Diagnostics

public class LockBasedLazyResolveStorageManager(private val storageManager: StorageManager): StorageManager by storageManager, LazyResolveStorageManager {
    override fun <K, V> createWeaklyRetainedMemoizedFunction(compute: Function1<K, V>) =
        storageManager.createMemoizedFunction<K, V>(compute, ConcurrentWeakValueHashMap<K, Any>())

    override fun <K, V> createWeaklyRetainedMemoizedFunctionWithNullableValues(compute: Function1<K, V>) =
        storageManager.createMemoizedFunctionWithNullableValues<K, V>(compute, ConcurrentWeakValueHashMap<K, Any>())

    // It seems safe to have a separate lock for traces:
    // no other locks will be acquired inside the trace operations
    override fun createSafeTrace(originalTrace: BindingTrace) =
            LockProtectedTrace(storageManager, originalTrace)

    private class LockProtectedContext(private val storageManager: StorageManager, private val context: BindingContext) : BindingContext {
        override fun getDiagnostics(): Diagnostics = storageManager.compute { context.getDiagnostics() }

        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K) = storageManager.compute { context.get<K, V>(slice, key) }

        override fun <K, V> getKeys(slice: WritableSlice<K, V>) = storageManager.compute { context.getKeys<K, V>(slice) }

        TestOnly
        override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>) = storageManager.compute { context.getSliceContents<K, V>(slice) }
    }

    private class LockProtectedTrace(private val storageManager: StorageManager, private val trace: BindingTrace) : BindingTrace {
        private val context: BindingContext = LockProtectedContext(storageManager, trace.getBindingContext())

        override fun getBindingContext() = context

        override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
            storageManager.compute { trace.record<K, V>(slice, key, value) }
        }

        override fun <K> record(slice: WritableSlice<K, Boolean>, key: K) {
            storageManager.compute { trace.record<K>(slice, key) }
        }

        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V = storageManager.compute { trace.get<K, V>(slice, key) }

        override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> = storageManager.compute { trace.getKeys<K, V>(slice) }

        override fun report(diagnostic: Diagnostic) {
            storageManager.compute { trace.report(diagnostic) }
        }
    }
}
