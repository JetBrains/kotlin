/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;

public class JetPsiCheckerAndHighlightingUpdater extends JetPsiChecker {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        try {
            super.annotate(element, holder);
            if (element instanceof JetFile) {
                //noinspection StaticMethodReferencedViaSubclass
                HighlighterPackage.updateHighlightingResult((JetFile) element, false);
            }
        }
        catch (ProcessCanceledException e) {
            HighlighterPackage.updateHighlightingResult((JetFile)element.getContainingFile(), false);
            throw e;
        }
    }
}
