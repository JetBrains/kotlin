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

import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import com.google.common.collect.ImmutableMap
import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.psi.PsiElement
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

public class CompositeBindingContext private (
        private val delegates: List<BindingContext>
) : BindingContext {

    default object {
        public fun create(delegates: List<BindingContext>): BindingContext {
            if (delegates.isEmpty()) return BindingContext.EMPTY
            if (delegates.size() == 1) return delegates.first()
            return CompositeBindingContext(delegates)
        }
    }

    override fun <K, V> get(slice: ReadOnlySlice<K, V>?, key: K?): V? {
        return delegates.stream().map { it[slice, key] }.firstOrNull { it != null }
    }

    override fun <K, V> getKeys(slice: WritableSlice<K, V>?): Collection<K> {
        return delegates.flatMap { it.getKeys(slice) }
    }

    override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
        //we need intermediate map cause ImmutableMap doesn't support same entries obtained from different slices
        var map = hashMapOf<K, V>()
        delegates.forEach { map.putAll(it.getSliceContents(slice)) }
        return ImmutableMap.builder<K, V>().putAll(map).build()
    }

    override fun getDiagnostics(): Diagnostics {
        return CompositeDiagnostics(delegates.map { it.getDiagnostics() })
    }

    private class CompositeDiagnostics(
            private val delegates: List<Diagnostics>
    ) : Diagnostics {

        override fun iterator(): Iterator<Diagnostic> {
            val emptyStream = listOf<Diagnostic>().stream()
            return delegates.fold(emptyStream, { r, t -> r + t.stream() }).iterator()
        }

        override val modificationTracker = object : ModificationTracker {
            override fun getModificationCount() = delegates.fold(0L, { r, t -> r + t.modificationTracker.getModificationCount() })
        }

        override fun all(): Collection<Diagnostic> {
            return delegates.flatMap { it.all() }
        }

        override fun forElement(psiElement: PsiElement): Collection<Diagnostic> {
            return delegates.flatMap { it.forElement(psiElement) }
        }

        override fun isEmpty(): Boolean {
            return delegates.all { it.isEmpty() }
        }

        override fun noSuppression(): Diagnostics {
            return CompositeDiagnostics(delegates.map { it.noSuppression() })
        }
    }
}
