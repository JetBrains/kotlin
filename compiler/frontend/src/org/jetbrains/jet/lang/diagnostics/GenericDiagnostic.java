/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class GenericDiagnostic implements DiagnosticWithTextRange {

    private final TextRange textRange;
    private final String message;
    private final DiagnosticFactory factory;
    private final Severity severity;
    private final PsiFile psiFile;

    public GenericDiagnostic(DiagnosticFactory factory, Severity severity, String message, @NotNull PsiFile psiFile, @NotNull TextRange textRange) {
        this.factory = factory;
        this.textRange = textRange;
        this.severity = severity;
        this.message = message;
        this.psiFile = psiFile;
    }

    @NotNull
    @Override
    public DiagnosticFactory getFactory() {
        return factory;
    }

    @NotNull
    public TextRange getTextRange() {
        return textRange;
    }

    @NotNull
    @Override
    public PsiFile getPsiFile() {
        return psiFile;
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
}
