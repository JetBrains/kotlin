/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.JetCodeFragment;
import org.jetbrains.kotlin.psi.JetFile;

public abstract class JetIntentionAction<T extends PsiElement> implements IntentionAction {
    protected @NotNull T element;

    public JetIntentionAction(@NotNull T element) {
        this.element = element;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiFile file) {
        return element.isValid() && (file.getManager().isInProject(file) || file instanceof JetCodeFragment) && (file instanceof JetFile);
    }

    //Don't override this method. Use the method with JetFile instead.
    @Deprecated
    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiFile file) throws IncorrectOperationException {
        if (file instanceof JetFile) {
            if (FileModificationService.getInstance().prepareFileForWrite(element.getContainingFile())) {
                invoke(project, editor, (JetFile) file);
            }
        }
    }

    protected abstract void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull JetFile file) throws IncorrectOperationException;

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
