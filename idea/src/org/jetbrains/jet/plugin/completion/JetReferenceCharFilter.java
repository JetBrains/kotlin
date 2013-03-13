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

package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;

public class JetReferenceCharFilter extends CharFilter {
    @Override
    @Nullable
    public Result acceptChar(char c, int prefixLength, Lookup lookup) {
        PsiFile psiFile = lookup.getPsiFile();
        if (!(psiFile instanceof JetFile)) {
            return null;
        }

        if (c == '.' && prefixLength == 0 && !lookup.isSelectionTouched()) {
            int caret = lookup.getEditor().getCaretModel().getOffset();
            if (caret > 0 && lookup.getEditor().getDocument().getCharsSequence().charAt(caret - 1) == '.') {
                return Result.HIDE_LOOKUP;
            }
        }

        return null;
    }
}