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

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;

import java.util.List;

public class RedeclarationDiagnosticFactory extends DiagnosticFactory1<PsiElement, String> {
    private static final PositioningStrategy<PsiElement> POSITION_REDECLARATION = new PositioningStrategy<PsiElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiElement element) {
            if (element instanceof JetNamedDeclaration) {
                PsiElement nameIdentifier = ((JetNamedDeclaration) element).getNameIdentifier();
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier);
                }
            }
            else if (element instanceof JetFile) {
                JetFile file = (JetFile) element;
                PsiElement nameIdentifier = file.getNamespaceHeader().getNameIdentifier();
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier);
                }
            }
            return markElement(element);
        }
    };

    public RedeclarationDiagnosticFactory(Severity severity) {
        super(severity, POSITION_REDECLARATION);
    }
}
