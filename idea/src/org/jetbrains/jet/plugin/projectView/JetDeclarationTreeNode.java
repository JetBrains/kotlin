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
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.structureView.JetStructureViewElement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * User: Alefas
 * Date: 15.02.12
 */
public class JetDeclarationTreeNode extends AbstractPsiBasedNode<JetDeclaration> {
    public static final String CLASS_INITIALIZER = "<class initializer>";

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
            String text = declaration.getName();
            if (text == null) return;
            JetCodeStyleSettings settings = CodeStyleSettingsManager.getInstance(getProject()).getCurrentSettings()
                    .getCustomSettings(JetCodeStyleSettings.class);
            if (declaration instanceof JetClassInitializer) {
                text = CLASS_INITIALIZER;
            } else if (declaration instanceof JetProperty) {
                JetProperty property = (JetProperty) declaration;
                JetTypeReference ref = property.getPropertyTypeRef();
                if (ref != null) {
                    if (settings.SPACE_BEFORE_TYPE_COLON) text += " ";
                    text += ":";
                    if (settings.SPACE_AFTER_TYPE_COLON) text += " ";
                    text += ref.getText();
                }
            } else if (declaration instanceof JetFunction) {
                JetFunction function = (JetFunction) declaration;
                JetTypeReference receiverTypeRef = function.getReceiverTypeRef();
                if (receiverTypeRef != null) {
                    text = receiverTypeRef.getText() + "." + text;
                }
                text += "(";
                List<JetParameter> parameters = function.getValueParameters();
                for (JetParameter parameter : parameters) {
                    if (parameter.getName() != null) {
                        text += parameter.getName();
                        if (settings.SPACE_BEFORE_TYPE_COLON) text += " ";
                        text += ":";
                        if (settings.SPACE_AFTER_TYPE_COLON) text += " ";
                    }
                    JetTypeReference typeReference = parameter.getTypeReference();
                    if (typeReference != null) {
                        text += typeReference.getText();
                    }
                    text += ", ";
                }
                if (parameters.size() > 0) text = text.substring(0, text.length() - 2);
                text += ")";
                JetTypeReference typeReference = function.getReturnTypeRef();
                if (typeReference != null) {
                    if (settings.SPACE_BEFORE_TYPE_COLON) text += " ";
                    text += ":";
                    if (settings.SPACE_AFTER_TYPE_COLON) text += " ";
                    text += typeReference.getText();
                }
            }

            data.setPresentableText(text);
        }
    }
}
