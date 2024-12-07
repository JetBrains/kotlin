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

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.MultiMap;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;

import java.util.Collection;

import static org.jetbrains.kotlin.utils.PlatformUtilsKt.createConcurrentMultiMap;

public class DiagnosticsElementsCache {
    private final Diagnostics diagnostics;
    private final Function1<Diagnostic, Boolean> filter;

    private final NotNullLazyValue<MultiMap<PsiElement, Diagnostic>> elementToDiagnostic;

    public DiagnosticsElementsCache(Diagnostics diagnostics, Function1<Diagnostic, Boolean> filter) {
        this.diagnostics = diagnostics;
        this.filter = filter;

        elementToDiagnostic = NotNullLazyValue.atomicLazy(() -> buildElementToDiagnosticCache(this.diagnostics, this.filter));
    }

    @NotNull
    public Collection<Diagnostic> getDiagnostics(@NotNull PsiElement psiElement) {
        return elementToDiagnostic.getValue().get(psiElement);
    }

    private static MultiMap<PsiElement, Diagnostic> buildElementToDiagnosticCache(Diagnostics diagnostics, Function1<Diagnostic, Boolean> filter) {
        MultiMap<PsiElement, Diagnostic> elementToDiagnostic = createConcurrentMultiMap();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic == null) {
                throw new IllegalStateException(
                        "There shouldn't be null diagnostics in the collection: " + CollectionsKt.toList(diagnostics));
            }
            if (filter.invoke(diagnostic)) {
                elementToDiagnostic.putValue(diagnostic.getPsiElement(), diagnostic);
            }
        }

        return elementToDiagnostic;
    }
}
