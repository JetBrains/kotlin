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

package org.jetbrains.kotlin.idea.copyright;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.psi.UpdatePsiFileCopyright;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UpdateKotlinCopyright extends UpdatePsiFileCopyright {
    UpdateKotlinCopyright(Project project, Module module, VirtualFile root, CopyrightProfile copyrightProfile) {
        super(project, module, root, copyrightProfile);
    }

    @Override
    protected void scanFile() {
        PsiRange commentSearchRange = getCommentSearchRange(getFile());
        PsiElement first = commentSearchRange.first;
        PsiElement last = commentSearchRange.last;

        if (first != null) {
            checkComments(first, last, true);
        }
    }

    public static class PsiRange {
        public final @Nullable PsiElement first;
        public final @Nullable PsiElement last;

        public PsiRange(@Nullable PsiElement first, @Nullable PsiElement second) {
            this.first = first;
            this.last = second;
        }
    }

    public static @NotNull PsiRange getCommentSearchRange(@NotNull PsiFile psiFile) {
        PsiElement first = psiFile.getFirstChild();
        PsiElement last = first;
        PsiElement next = first;
        while (next != null) {
            if (next instanceof PsiComment || next instanceof PsiWhiteSpace || next.getText().isEmpty()) {
                next = next.getNextSibling();
            }
            else {
                break;
            }
            last = next;
        }

        return new PsiRange(first, last);
    }
}
