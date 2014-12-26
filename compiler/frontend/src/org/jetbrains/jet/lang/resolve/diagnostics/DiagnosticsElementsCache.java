/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.diagnostics;

import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ConcurrentMultiMap;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;

import java.util.Collection;

public class DiagnosticsElementsCache {
    private final Diagnostics diagnostics;

    private final AtomicNotNullLazyValue<MultiMap<PsiElement, Diagnostic>> elementToDiagnostic = new AtomicNotNullLazyValue<MultiMap<PsiElement, Diagnostic>>() {
        @NotNull
        @Override
        protected MultiMap<PsiElement, Diagnostic> compute() {
            return buildElementToDiagnosticCache(diagnostics);
        }
    };

    public DiagnosticsElementsCache(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @NotNull
    public Collection<Diagnostic> getDiagnostics(@NotNull PsiElement psiElement) {
        return elementToDiagnostic.getValue().get(psiElement);
    }

    private static MultiMap<PsiElement, Diagnostic> buildElementToDiagnosticCache(Diagnostics diagnostics) {
        MultiMap<PsiElement, Diagnostic> elementToDiagnostic = new ConcurrentMultiMap<PsiElement, Diagnostic>();
        for (Diagnostic diagnostic : diagnostics) {
            elementToDiagnostic.putValue(diagnostic.getPsiElement(), diagnostic);
        }

        return elementToDiagnostic;
    }
}
