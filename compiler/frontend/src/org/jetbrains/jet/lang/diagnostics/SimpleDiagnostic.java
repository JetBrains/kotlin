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

package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class SimpleDiagnostic<E extends PsiElement> extends AbstractDiagnostic<E> {
    public SimpleDiagnostic(@NotNull E psiElement,
            @NotNull DiagnosticFactory0<E> factory,
            @NotNull Severity severity) {
        super(psiElement, factory, severity);
    }

    @NotNull
    @Override
    public DiagnosticFactory0<E> getFactory() {
        return (DiagnosticFactory0<E>)super.getFactory();
    }
}
