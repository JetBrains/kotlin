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
public abstract class DiagnosticFactoryWithPsiElement3<T extends PsiElement, A, B, C> extends DiagnosticFactoryWithMessageFormat {
    protected DiagnosticFactoryWithPsiElement3(Severity severity, String messageStub, Renderer renderer) {
        super(severity, messageStub, renderer);
    }

    protected DiagnosticFactoryWithPsiElement3(Severity severity, String message) {
        super(severity, message);
    }

    protected String makeMessage(@NotNull A a, @NotNull B b, @NotNull C c) {
        return messageFormat.format(new Object[]{makeMessageForA(a), makeMessageForB(b), makeMessageForC(c)});
    }

    protected String makeMessageForA(@NotNull A a) {
        return renderer.render(a);
    }

    protected String makeMessageForB(@NotNull B b) {
        return renderer.render(b);
    }

    protected String makeMessageForC(@NotNull C c) {
        return renderer.render(c);
    }

    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToMark, @NotNull A a, @NotNull B b, @NotNull C c) {
        return on(elementToMark, elementToMark, a, b, c);
    }
    
    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull PsiElement elementToMark, @NotNull A a, @NotNull B b, @NotNull C c) {
        return on(elementToBlame, elementToMark.getTextRange(), a, b, c);
    }

    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull ASTNode nodeToMark, @NotNull A a, @NotNull B b, @NotNull C c) {
        return on(elementToBlame, nodeToMark.getTextRange(), a, b, c);
    }

    @NotNull
    protected DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull TextRange textRangeToMark, @NotNull A a, @NotNull B b, @NotNull C c) {
        return new DiagnosticWithPsiElementImpl<T>(this, severity, makeMessage(a, b, c), elementToBlame, textRangeToMark);
    }
    
}
