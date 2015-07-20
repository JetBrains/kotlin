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

import com.intellij.ide.projectView.SelectableTreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.idea.JetIconProvider;
import org.jetbrains.kotlin.psi.JetClassBody;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JetProjectViewProvider implements SelectableTreeStructureProvider, DumbAware {
    private final Project myProject;

    public JetProjectViewProvider(Project project) {
        myProject = project;
    }

    @Override
    public Collection<AbstractTreeNode> modify(AbstractTreeNode parent, Collection<AbstractTreeNode> children, ViewSettings settings) {
        List<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();

        for (AbstractTreeNode child : children) {
            Object childValue = child.getValue();

            if (childValue instanceof JetFile) {
                JetFile file = (JetFile) childValue;
                List<JetDeclaration> declarations = file.getDeclarations();

                JetClassOrObject mainClass = JetIconProvider.getMainClass(file);
                if (mainClass != null && declarations.size() == 1) {
                    result.add(new JetClassOrObjectTreeNode(file.getProject(), mainClass, settings));
                }
                else {
                    result.add(new JetFileTreeNode(file.getProject(), file, settings));
                }
            }
            else {
                result.add(child);
            }

        }

        return result;
    }

    @Override
    public Object getData(Collection<AbstractTreeNode> selected, String dataName) {
        return null;
    }

    @Override
    public PsiElement getTopLevelElement(PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null || !(file instanceof JetFile)) return null;

        VirtualFile virtualFile = file.getVirtualFile();
        if (!fileInRoots(virtualFile)) return file;

        PsiElement current = element;
        while (current != null) {
            if (isSelectable(current)) break;
            current = current.getParent();
        }

        if (current instanceof JetFile) {
            List<JetDeclaration> declarations = ((JetFile) current).getDeclarations();
            String nameWithoutExtension = virtualFile != null ? virtualFile.getNameWithoutExtension() : file.getName();
            if (declarations.size() == 1 && declarations.get(0) instanceof JetClassOrObject &&
                nameWithoutExtension.equals(declarations.get(0).getName())) {
                current = declarations.get(0);
            }
        }

        return current != null ? current : file;
    }

    private static boolean isSelectable(PsiElement element) {
        if (element instanceof JetFile) return true;
        if (element instanceof JetDeclaration) {
            PsiElement parent = element.getParent();
            if (parent instanceof JetFile) {
                return true;
            }
            else if (parent instanceof JetClassBody) {
                parent = parent.getParent();
                if (parent instanceof JetClassOrObject) {
                    return isSelectable(parent);
                }
                else return false;
            }
            else return false;
        }
        else return false;
    }

    private boolean fileInRoots(VirtualFile file) {
        ProjectFileIndex index = ProjectRootManager.getInstance(myProject).getFileIndex();
        return file != null && (index.isInSourceContent(file) || index.isInLibraryClasses(file) || index.isInLibrarySource(file));
    }
}
