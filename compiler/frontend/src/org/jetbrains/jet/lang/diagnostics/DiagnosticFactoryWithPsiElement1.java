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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public abstract class DiagnosticFactoryWithPsiElement1<T extends PsiElement, A> extends DiagnosticFactoryWithMessageFormat {

    protected DiagnosticFactoryWithPsiElement1(Severity severity, String message, Renderer renderer) {
        super(severity, message, renderer);
    }

    protected DiagnosticFactoryWithPsiElement1(Severity severity, String message) {
        super(severity, message);
    }

    protected String makeMessage(@NotNull A argument) {
        return messageFormat.format(new Object[]{makeMessageFor(argument)});
    }

    protected String makeMessageFor(A argument) {
        return renderer.render(argument);
    }

    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToMark, @NotNull A argument) {
        return on(elementToMark, elementToMark.getTextRange(), argument);
    }

    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull ASTNode nodeToMark, @NotNull A argument) {
        return on(elementToBlame, nodeToMark.getTextRange(), argument);
    }

    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull PsiElement elementToMark, @NotNull A argument) {
        return on(elementToBlame, elementToMark.getTextRange(), argument);
    }
    
    @NotNull
    protected DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull TextRange textRangeToMark, @NotNull A argument) {
        return new DiagnosticWithPsiElementImpl<T>(this, severity, makeMessage(argument), elementToBlame, textRangeToMark);
    }
}
