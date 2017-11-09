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

package org.jetbrains.kotlin.resolve.diagnostics;

import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.FilteringIterator;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.resolve.BindingContext;

import java.util.Collection;
import java.util.Iterator;

public class DiagnosticsWithSuppression implements Diagnostics {
    private final KotlinSuppressCache kotlinSuppressCache;
    private final Collection<Diagnostic> diagnostics;
    private final DiagnosticsElementsCache elementsCache;

    public DiagnosticsWithSuppression(@NotNull BindingContext context, @NotNull Collection<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
        this.kotlinSuppressCache = new BindingContextSuppressCache(context);
        this.elementsCache = new DiagnosticsElementsCache(this, kotlinSuppressCache.getFilter());
    }

    @NotNull
    @Override
    public Diagnostics noSuppression() {
        return new SimpleDiagnostics(diagnostics);
    }

    @NotNull
    @Override
    public Iterator<Diagnostic> iterator() {
        return new FilteringIterator<>(diagnostics.iterator(), kotlinSuppressCache.getFilter()::invoke);
    }

    @NotNull
    @Override
    public Collection<Diagnostic> all() {
        return CollectionsKt.filter(diagnostics, kotlinSuppressCache.getFilter());
    }

    @NotNull
    @Override
    public Collection<Diagnostic> forElement(@NotNull PsiElement psiElement) {
        return elementsCache.getDiagnostics(psiElement);
    }

    @Override
    public boolean isEmpty() {
        return all().isEmpty();
    }

    @NotNull
    @Override
    public ModificationTracker getModificationTracker() {
        throw new IllegalStateException("Trying to obtain modification tracker for readonly DiagnosticsWithSuppression.");
    }

    @TestOnly
    @NotNull
    public Collection<Diagnostic> getDiagnostics() {
        return diagnostics;
    }
}
