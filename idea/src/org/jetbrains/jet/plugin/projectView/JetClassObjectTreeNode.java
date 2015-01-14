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

package org.jetbrains.jet.plugin.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.JetClassObject;
import org.jetbrains.kotlin.psi.JetPsiUtil;

import java.util.Collection;

import static org.jetbrains.jet.plugin.projectView.JetProjectViewUtil.canRepresentPsiElement;
import static org.jetbrains.jet.plugin.projectView.JetProjectViewUtil.getClassOrObjectChildren;

public class JetClassObjectTreeNode extends AbstractPsiBasedNode<JetClassObject> {
    protected JetClassObjectTreeNode(Project project, JetClassObject classObject, ViewSettings viewSettings) {
        super(project, classObject, viewSettings);
    }

    @Override
    protected PsiElement extractPsiFromValue() {
        return getValue();
    }

    @Override
    protected Collection<AbstractTreeNode> getChildrenImpl() {
        return getClassOrObjectChildren(getValue().getObjectDeclaration(), getProject(), getSettings());
    }

    @Override
    protected void updateImpl(PresentationData data) {
        data.setPresentableText("<class object>");
    }

    @Override
    public boolean canRepresent(Object element) {
        if (!isValid()) {
            return false;
        }

        return super.canRepresent(element) || canRepresentPsiElement(getValue(), element, getSettings());
    }

    @Override
    protected boolean isDeprecated() {
        return JetPsiUtil.isDeprecated(getValue());
    }
}
