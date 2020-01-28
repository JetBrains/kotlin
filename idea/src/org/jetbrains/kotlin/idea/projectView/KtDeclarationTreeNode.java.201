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

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings;
import org.jetbrains.kotlin.psi.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class KtDeclarationTreeNode extends AbstractPsiBasedNode<KtDeclaration> {
    public static final String CLASS_INITIALIZER = "<class initializer>";

    protected KtDeclarationTreeNode(Project project, KtDeclaration ktDeclaration, ViewSettings viewSettings) {
        super(project, ktDeclaration, viewSettings);
    }

    @Override
    protected PsiElement extractPsiFromValue() {
        return getValue();
    }

    @Override
    protected Collection<AbstractTreeNode<?>> getChildrenImpl() {
        return Collections.emptyList();
    }

    @Override
    protected void updateImpl(PresentationData data) {
        KtDeclaration declaration = getValue();
        if (declaration != null) {
            String text = declaration instanceof KtAnonymousInitializer ? CLASS_INITIALIZER : declaration.getName();
            if (text == null) return;

            KotlinCodeStyleSettings settings = CodeStyle.getSettings(getProject())
                    .getCustomSettings(KotlinCodeStyleSettings.class);

            if (declaration instanceof KtProperty) {
                KtProperty property = (KtProperty) declaration;
                KtTypeReference ref = property.getTypeReference();
                if (ref != null) {
                    if (settings.SPACE_BEFORE_TYPE_COLON) text += " ";
                    text += ":";
                    if (settings.SPACE_AFTER_TYPE_COLON) text += " ";
                    text += ref.getText();
                }
            }
            else if (declaration instanceof KtFunction) {
                KtFunction function = (KtFunction) declaration;
                KtTypeReference receiverTypeRef = function.getReceiverTypeReference();
                if (receiverTypeRef != null) {
                    text = receiverTypeRef.getText() + "." + text;
                }
                text += "(";
                List<KtParameter> parameters = function.getValueParameters();
                for (KtParameter parameter : parameters) {
                    if (parameter.getName() != null) {
                        text += parameter.getName();
                        if (settings.SPACE_BEFORE_TYPE_COLON) text += " ";
                        text += ":";
                        if (settings.SPACE_AFTER_TYPE_COLON) text += " ";
                    }
                    KtTypeReference typeReference = parameter.getTypeReference();
                    if (typeReference != null) {
                        text += typeReference.getText();
                    }
                    text += ", ";
                }
                if (parameters.size() > 0) text = text.substring(0, text.length() - 2);
                text += ")";
                KtTypeReference typeReference = function.getTypeReference();
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

    @Override
    protected boolean isDeprecated() {
        return KtPsiUtil.isDeprecated(getValue());
    }
}
