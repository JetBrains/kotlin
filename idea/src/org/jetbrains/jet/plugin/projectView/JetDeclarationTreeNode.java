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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClassInitializer;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.structureView.JetStructureViewElement;

import java.util.Collection;
import java.util.Collections;

/**
 * User: Alefas
 * Date: 15.02.12
 */
public class JetDeclarationTreeNode extends AbstractPsiBasedNode<JetDeclaration> {
    protected JetDeclarationTreeNode(Project project, JetDeclaration jetDeclaration, ViewSettings viewSettings) {
        super(project, jetDeclaration, viewSettings);
    }

    @Override
    protected PsiElement extractPsiFromValue() {
        return getValue();
    }

    @Override
    protected Collection<AbstractTreeNode> getChildrenImpl() {
        return Collections.emptyList();
    }

    @Override
    protected void updateImpl(PresentationData data) {
        JetDeclaration declaration = getValue();
        if (declaration != null) {
            BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(
                    (JetFile) declaration.getContainingFile());

            final DeclarationDescriptor descriptor =
                    context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
            if (descriptor != null) {
                String text = JetStructureViewElement.getDescriptorTreeText(descriptor);
                if (declaration instanceof JetClassInitializer) {
                    text = "<class initializer>";
                }
                data.setPresentableText(text);
            } else {
                String text = declaration.getName();
                if (declaration instanceof JetClassInitializer) {
                    text = "<class initializer>";
                }
                data.setPresentableText(text);
            }
        }
    }
}
