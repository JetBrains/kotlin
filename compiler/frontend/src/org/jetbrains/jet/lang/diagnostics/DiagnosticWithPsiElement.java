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

import java.util.List;

/**
 * @author svtk
 */
public class DiagnosticWithPsiElement<P extends PsiElement> extends AbstractDiagnostic<P> {
    public DiagnosticWithPsiElement(@NotNull P psiElement, @NotNull DiagnosticFactoryWithPsiElement<P> factory, @NotNull Severity severity, @NotNull String message) {
        super(psiElement, factory, severity, message);
    }

    @NotNull
    @Override
    public DiagnosticFactoryWithPsiElement<P> getFactory() {
        return (DiagnosticFactoryWithPsiElement<P>)super.getFactory();
    }

    @NotNull
    public List<TextRange> getTextRanges() {
        return getFactory().getTextRanges(this);
    }
}
