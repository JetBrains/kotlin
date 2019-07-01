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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.TreeTraversal;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.psi.UpdatePsiFileCopyright;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtDeclaration;

import java.util.List;

public class UpdateKotlinCopyright extends UpdatePsiFileCopyright {
    UpdateKotlinCopyright(Project project, Module module, VirtualFile root, CopyrightProfile copyrightProfile) {
        super(project, module, root, copyrightProfile);
    }

    @Override
    protected void scanFile() {
        List<PsiComment> comments = getExistentComments(getFile());
        checkComments(ContainerUtil.getLastItem(comments), true, comments);
    }

    @NotNull
    public static List<PsiComment> getExistentComments(@NotNull PsiFile psiFile) {
        return SyntaxTraverser.psiTraverser(psiFile)
                .withTraversal(TreeTraversal.LEAVES_DFS)
                .traverse()
                .takeWhile(
                        element ->
                                (element instanceof PsiComment && !(element.getParent() instanceof KtDeclaration)) ||
                                element instanceof PsiWhiteSpace ||
                                element.getText().isEmpty()
                )
                .filter(PsiComment.class)
                .toList();
    }
}
