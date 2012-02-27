/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public abstract class AbstractDiagnostic<P extends PsiElement> implements Diagnostic<P> {
    private final P psiElement;
    private final String message;
    private final AbstractDiagnosticFactory factory;
    private final Severity severity;

    public AbstractDiagnostic(@NotNull P psiElement, @NotNull AbstractDiagnosticFactory factory, @NotNull Severity severity, @NotNull String message) {
        this.psiElement = psiElement;
        this.factory = factory;
        this.severity = severity;
        this.message = message;
    }

    @NotNull
    @Override
    public AbstractDiagnosticFactory getFactory() {
        return factory;
    }

    @NotNull
    @Override
    public PsiFile getPsiFile() {
        return psiElement.getContainingFile();
    }

    @NotNull
    @Override
    public String getMessage() {
        return message;
    }

    @NotNull
    @Override
    public Severity getSeverity() {
        return severity;
    }


    @Override
    @NotNull
    public P getPsiElement() {
        return psiElement;
    }
}
