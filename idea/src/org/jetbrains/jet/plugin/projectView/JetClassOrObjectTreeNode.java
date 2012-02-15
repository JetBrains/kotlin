/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * User: Alefas
 * Date: 15.02.12
 */
public class JetClassOrObjectTreeNode extends AbstractPsiBasedNode<JetClassOrObject> {
    protected JetClassOrObjectTreeNode(Project project, JetClassOrObject jetClassOrObject, ViewSettings viewSettings) {
        super(project, jetClassOrObject, viewSettings);
    }

    @Override
    protected PsiElement extractPsiFromValue() {
        return getValue();
    }

    @Override
    protected Collection<AbstractTreeNode> getChildrenImpl() {
        if (getSettings().isShowMembers()) {
            ArrayList<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
            List<JetDeclaration> declarations = getValue().getDeclarations();
            for (JetDeclaration declaration : declarations) {
                if (declaration instanceof JetClassOrObject) {
                    result.add(new JetClassOrObjectTreeNode(getProject(), (JetClassOrObject) declaration, getSettings()));
                } else {
                    result.add(new JetDeclarationTreeNode(getProject(), declaration, getSettings()));
                }
            }
            return result;
        } else return Collections.emptyList();
    }

    @Override
    protected void updateImpl(PresentationData data) {
        JetClassOrObject value = getValue();
        if (value != null) {
            data.setPresentableText(value.getName());
        }
    }

    @Override
    public boolean canRepresent(Object element) {
        if (!isValid()) return false;
        return super.canRepresent(element) || canRepresent(getValue(), element);
    }

    private boolean canRepresent(JetClassOrObject value, Object element) {
        if (value == null || !value.isValid()) return false;
        PsiFile file = value.getContainingFile();
        if (file != null && (file == element || file.getVirtualFile() == element)) return true;

        if (value == element) return true;
        if (!getSettings().isShowMembers()) {
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
