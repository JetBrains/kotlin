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
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class SimpleDiagnosticFactory extends SimpleDiagnosticFactoryWithPsiElement<PsiElement> {
    public static SimpleDiagnosticFactory create(Severity severity, String message) {
        return new SimpleDiagnosticFactory(severity, message);
    }

    protected SimpleDiagnosticFactory(Severity severity, String message) {
        super(severity, message);
    }

    @NotNull
    public Diagnostic on(@NotNull PsiFile psiFile, @NotNull TextRange range) {
        return new GenericDiagnostic(this, severity, message, psiFile, range);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode node) {
        return on(DiagnosticUtils.getContainingFile(node), node.getTextRange());
    }
}
