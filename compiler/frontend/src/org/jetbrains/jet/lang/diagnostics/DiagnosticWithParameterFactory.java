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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public class DiagnosticWithParameterFactory<T extends PsiElement, A> extends PsiElementOnlyDiagnosticFactory1<T, A> {
    public static <T extends PsiElement, A> DiagnosticWithParameterFactory<T, A> create(Severity severity, String messageStub, DiagnosticParameter<A> diagnosticParameter) {
        return new DiagnosticWithParameterFactory<T, A>(severity, messageStub, diagnosticParameter);
    }

    private final DiagnosticParameter<A> diagnosticParameter;

    protected DiagnosticWithParameterFactory(Severity severity, String message, DiagnosticParameter<A> diagnosticParameter) {
        super(severity, message);
        this.diagnosticParameter = diagnosticParameter;
    }

    @NotNull
    @Override
    protected DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull TextRange textRangeToMark, @NotNull A argument) {
        return super.on(elementToBlame, textRangeToMark, argument).add(diagnosticParameter, argument);
    }
}
