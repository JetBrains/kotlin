/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve

import org.jetbrains.jet.util.slicedmap.ReadOnlySlice
import org.jetbrains.jet.util.slicedmap.WritableSlice
import com.google.common.collect.ImmutableMap
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.psi.PsiElement

public class CompositeBindingContext private (
        private val delegates: List<BindingContext>
) : BindingContext {

    class object {
        public fun create(delegates: List<BindingContext>): BindingContext {
            if (delegates.isEmpty()) return BindingContext.EMPTY
            if (delegates.size == 1) return delegates.first()
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
        val builder = ImmutableMap.builder<K, V>()!!
        for (delegate in delegates) {
            builder.putAll(delegate.getSliceContents(slice))
        }
        return builder.build()!!
    }

    override fun getDiagnostics(): Diagnostics {
        return CompositeDiagnostics(delegates.map { it.getDiagnostics() })
    }

    private class CompositeDiagnostics(
            private val delegates: List<Diagnostics>
    ) : Diagnostics {
        override fun iterator(): Iterator<Diagnostic> {
            val emptyStream = listOf<Diagnostic>().stream()
            return delegates.fold(emptyStream, { r, t -> r + t.stream()}).iterator()
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
