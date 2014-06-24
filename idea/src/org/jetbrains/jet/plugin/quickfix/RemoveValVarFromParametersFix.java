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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.PluginJetFilesProvider;

import java.util.Collection;

public class RemoveValVarFromParametersFix implements IntentionAction {
    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.val.var.from.parameter");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.val.var.from.parameter");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file.getManager().isInProject(file);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        // TODO after M6, this quick fix should remove val/var only for current parameter

        Collection<JetFile> files = PluginJetFilesProvider.allFilesInProject(project);
        for (JetFile jetFile : files) {
            jetFile.acceptChildren(new JetVisitorVoid() {
                @Override
                public void visitParameter(@NotNull JetParameter parameter) {
                    visitJetElement(parameter); // run recursively for children

                    PsiElement parent = parameter.getParent();
                    if (parent != null && parent.getParent() instanceof JetClass) {
                        return; // constructor parameter
                    }

                    ASTNode valOrVarNode = parameter.getValOrVarNode();
                    if (valOrVarNode != null) {
                        ASTNode whitespace = valOrVarNode.getTreeNext();
                        assert whitespace.getElementType() == TokenType.WHITE_SPACE;

                        parameter.getNode().removeRange(valOrVarNode, whitespace.getTreeNext());
                    }
                }

                @Override
                public void visitJetElement(@NotNull JetElement element) {
                    element.acceptChildren(this);
                }
            });
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
