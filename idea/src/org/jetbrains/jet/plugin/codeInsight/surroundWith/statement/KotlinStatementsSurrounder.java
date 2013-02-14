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

package org.jetbrains.jet.plugin.codeInsight.surroundWith.statement;

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class KotlinStatementsSurrounder implements Surrounder {

    @Override
    public boolean isApplicable(@NotNull PsiElement[] elements) {
        return elements.length > 0;
    }

    @Override
    @Nullable
    public TextRange surroundElements(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull PsiElement[] elements
    ) throws IncorrectOperationException {
        PsiElement container = elements[0].getParent();
        if (container == null) return null;
        return surroundStatements(project, editor, container, elements);
    }

    @Nullable
    protected abstract TextRange surroundStatements(
            final Project project,
            final Editor editor,
            final PsiElement container,
            final PsiElement[] statements
    );
}
