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

package org.jetbrains.kotlin.idea.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetDeclaration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class JetProjectViewUtil {

    private JetProjectViewUtil() {
    }

    public static Collection<AbstractTreeNode> getClassOrObjectChildren(JetClassOrObject classOrObject, Project project,
                                                                        ViewSettings settings) {
        if (classOrObject != null && settings.isShowMembers()) {
            Collection<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
            List<JetDeclaration> declarations = classOrObject.getDeclarations();
            for (JetDeclaration declaration : declarations) {
                if (declaration instanceof JetClassOrObject) {
                    result.add(new JetClassOrObjectTreeNode(project, (JetClassOrObject) declaration, settings));
                }
                else {
                    result.add(new JetDeclarationTreeNode(project, declaration, settings));
                }
            }

            return result;
        }
        else {
            return Collections.emptyList();
        }
    }

    public static boolean canRepresentPsiElement(PsiElement value, Object element, ViewSettings settings) {
        if (value == null || !value.isValid()) {
            return false;
        }

        PsiFile file = value.getContainingFile();
        if (file != null && (file == element || file.getVirtualFile() == element)) {
            return true;
        }

        if (value == element) {
            return true;
        }

        if (!settings.isShowMembers()) {
            if (element instanceof PsiElement && ((PsiElement) element).getContainingFile() != null) {
                PsiFile elementFile = ((PsiElement) element).getContainingFile();
                if (elementFile != null && file != null) {
                    return elementFile.equals(file);
                }
            }
        }

        return false;
    }
}
