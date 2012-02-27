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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.AbstractDiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;

import java.util.List;

/**
 * @author svtk
 */
public class FakeDiagnosticForQuickFixes implements Diagnostic {
    private final PsiElement element;

    public FakeDiagnosticForQuickFixes(PsiElement element) {
        this.element = element;
    }

    @NotNull
    @Override
    public AbstractDiagnosticFactory getFactory() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public String getMessage() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Severity getSeverity() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public PsiElement getPsiElement() {
        return element;
    }

    @NotNull
    @Override
    public List<TextRange> getTextRanges() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public PsiFile getPsiFile() {
        return element.getContainingFile();
    }
}
