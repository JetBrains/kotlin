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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public abstract class DiagnosticFactoryWithPsiElement2<T extends PsiElement, A, B> extends DiagnosticFactoryWithMessageFormat {

    protected DiagnosticFactoryWithPsiElement2(Severity severity, String message, Renderer renderer) {
        super(severity, message, renderer);
    }

    protected DiagnosticFactoryWithPsiElement2(Severity severity, String message) {
        super(severity, message);
    }

    protected String makeMessage(@NotNull A a, @NotNull B b) {
        return messageFormat.format(new Object[] {makeMessageForA(a), makeMessageForB(b)});
    }

    protected String makeMessageForA(@NotNull A a) {
        return renderer.render(a);
    }

    protected String makeMessageForB(@NotNull B b) {
        return renderer.render(b);
    }

    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToMark, @NotNull A a, @NotNull B b) {
        return on(elementToMark, elementToMark.getNode(), a, b);
    }
    
    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull ASTNode nodeToMark, @NotNull A a, @NotNull B b) {
        return makeDiagnostic(new DiagnosticWithPsiElementImpl<T>(this, severity, makeMessage(a, b), elementToBlame, nodeToMark.getTextRange()));
    }

    public DiagnosticWithPsiElement<T> makeDiagnostic(DiagnosticWithPsiElement<T> diagnostic) {
        return diagnostic;
    }
}
