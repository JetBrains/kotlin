/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.psi.JetClassOrObject;

import java.util.Collection;

import static org.jetbrains.jet.plugin.projectView.JetProjectViewUtil.canRepresentPsiElement;
import static org.jetbrains.jet.plugin.projectView.JetProjectViewUtil.getClassOrObjectChildren;

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
        return getClassOrObjectChildren(getValue(), getProject(), getSettings());
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
        if (!isValid()) {
            return false;
        }

        return super.canRepresent(element) || canRepresentPsiElement(getValue(), element, getSettings());
    }
}
