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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PositioningStrategy<E extends PsiElement> {
    @NotNull
    public List<TextRange> markDiagnostic(@NotNull ParametrizedDiagnostic<? extends E> diagnostic) {
        return mark(diagnostic.getPsiElement());
    }

    @NotNull
    protected List<TextRange> mark(@NotNull E element) {
        return markElement(element);
    }

    public boolean isValid(@NotNull E element) {
        return !hasSyntaxErrors(element);
    }

    @NotNull
    protected static List<TextRange> markElement(@NotNull PsiElement element) {
        return Collections.singletonList(element.getTextRange());
    }

    @NotNull
    protected static List<TextRange> markNode(@NotNull ASTNode node) {
        return Collections.singletonList(node.getTextRange());
    }

    @NotNull
    protected static List<TextRange> markRange(@NotNull TextRange range) {
        return Collections.singletonList(range);
    }

    protected static boolean hasSyntaxErrors(@NotNull PsiElement psiElement) {
        if (psiElement instanceof PsiErrorElement) return true;

        PsiElement[] children = psiElement.getChildren();
        if (children.length > 0 && hasSyntaxErrors(children[children.length - 1])) return true;

        return false;
    }
}
