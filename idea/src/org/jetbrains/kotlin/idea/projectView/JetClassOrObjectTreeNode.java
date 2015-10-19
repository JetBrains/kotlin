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

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.idea.JetIconProvider;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtPsiUtil;

import java.util.Collection;

import static org.jetbrains.kotlin.idea.projectView.JetProjectViewUtil.canRepresentPsiElement;
import static org.jetbrains.kotlin.idea.projectView.JetProjectViewUtil.getClassOrObjectChildren;

public class JetClassOrObjectTreeNode extends AbstractPsiBasedNode<KtClassOrObject> {
    protected JetClassOrObjectTreeNode(Project project, KtClassOrObject ktClassOrObject, ViewSettings viewSettings) {
        super(project, ktClassOrObject, viewSettings);
    }

    @Override
    protected PsiElement extractPsiFromValue() {
        return getValue();
    }

    @Override
    protected Collection<AbstractTreeNode> getChildrenImpl() {
        return getClassOrObjectChildren(getValue(), getProject(), getSettings());
    }

    private void update(AbstractTreeNode node) {
        Project project = getProject();
        if (project != null) {
            ProjectView.getInstance(project).getCurrentProjectViewPane().getTreeBuilder().addSubtreeToUpdateByElement(node);
        }
    }

    @Override
    protected void updateImpl(PresentationData data) {
        KtClassOrObject classOrObject = getValue();
        if (classOrObject != null) {
            data.setPresentableText(classOrObject.getName());

            AbstractTreeNode parent = getParent();
            if (JetIconProvider.getMainClass(classOrObject.getContainingJetFile()) != null) {
                if (parent instanceof JetFileTreeNode) {
                    update(parent.getParent());
                }
            }
            else {
                if (!(parent instanceof JetClassOrObjectTreeNode) && !(parent instanceof JetFileTreeNode)) {
                    update(parent);
                }
            }
        }
    }

    @Override
    protected boolean isDeprecated() {
        return KtPsiUtil.isDeprecated(getValue());
    }

    @Override
    public boolean canRepresent(Object element) {
        if (!isValid()) {
            return false;
        }

        return super.canRepresent(element) || canRepresentPsiElement(getValue(), element, getSettings());
    }

    @Override
    public int getWeight() {
        return 20;
    }
}
