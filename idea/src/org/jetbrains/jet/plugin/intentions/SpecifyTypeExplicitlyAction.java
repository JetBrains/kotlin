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

package org.jetbrains.jet.plugin.intentions;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.ReferenceToClassesShortening;
import org.jetbrains.jet.plugin.project.AnalyzeSingleFileUtil;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Collections;

/**
 * @author Evgeny Gerashchenko
 * @since 4/20/12
 */
public class SpecifyTypeExplicitlyAction extends PsiElementBaseIntentionAction {
    private JetType targetType;

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("specify.type.explicitly.action.family.name");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof JetProperty) {
            JetProperty property = (JetProperty) parent;
            if (targetType == null) {
                removeTypeAnnotation(project, property);
            } else {
                addTypeAnnotation(project, property, targetType);
            }
        } else if (parent instanceof JetNamedFunction) {
            assert targetType != null;
            addTypeAnnotation(project, (JetFunction) parent, targetType);
        } else {
            assert false;
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (element.getParent() instanceof JetProperty && !PsiTreeUtil.isAncestor(((JetProperty) element.getParent()).getInitializer(), element, false)) {
            if (((JetProperty)element.getParent()).getPropertyTypeRef() != null) {
                targetType = null;
                setText(JetBundle.message("specify.type.explicitly.remove.action.name"));
                return true;
            }
            else {
                setText(JetBundle.message("specify.type.explicitly.add.action.name"));
            }
        }
        else if (element.getParent() instanceof JetNamedFunction && ((JetNamedFunction) element.getParent()).getReturnTypeRef() == null
                 && !((JetNamedFunction) element.getParent()).hasBlockBody()) {
            setText(JetBundle.message("specify.type.explicitly.add.return.type.action.name"));
        }
        else {
            return false;
        }

        BindingContext bindingContext = AnalyzeSingleFileUtil.getContextForSingleFile((JetFile)element.getContainingFile());
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element.getParent());

        if (descriptor instanceof VariableDescriptor) {
            targetType = ((VariableDescriptor) descriptor).getType();
        }
        else if (descriptor instanceof SimpleFunctionDescriptor) {
            targetType = ((SimpleFunctionDescriptor) descriptor).getReturnType();
        }
        else {
            assert false;
        }

        if (targetType == null || ErrorUtils.isErrorType(targetType)) {
            return false;
        }
        if (isDisabledForError()) {
            for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
                if (Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE == diagnostic.getFactory() && element.getParent() == diagnostic.getPsiElement()) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean isDisabledForError() {
        return true;
    }

    public static void addTypeAnnotation(Project project, JetProperty property, @NotNull JetType exprType) {
        if (property.getPropertyTypeRef() != null) return;
        PsiElement anchor = property.getNameIdentifier();
        if (anchor == null) return;
        anchor = anchor.getNextSibling();
        if (anchor != null) {
            if (!(anchor instanceof PsiWhiteSpace)) {
                return;
            }
        }
        JetTypeReference typeReference = JetPsiFactory.createType(project, DescriptorRenderer.TEXT.renderType(exprType));
        ASTNode colon = JetPsiFactory.createColonNode(project);
        ASTNode anchorNode = anchor == null ? null : anchor.getNode().getTreeNext();
        if (anchor == null) {
            property.getNode().addChild(JetPsiFactory.createWhiteSpace(project).getNode(), anchorNode);
        }
        property.getNode().addChild(colon, anchorNode);
        property.getNode().addChild(JetPsiFactory.createWhiteSpace(project).getNode(), anchorNode);
        property.getNode().addChild(typeReference.getNode(), anchorNode);
        property.getNode().addChild(JetPsiFactory.createWhiteSpace(project).getNode(), anchorNode);
        if (anchor != null) {
            anchor.delete();
        }
        ReferenceToClassesShortening.compactReferenceToClasses(Collections.singletonList(property));
    }

    public static void addTypeAnnotation(Project project, JetFunction function, @NotNull JetType exprType) {
        JetTypeReference typeReference = JetPsiFactory.createType(project, DescriptorRenderer.TEXT.renderType(exprType));
        Pair<PsiElement, PsiElement> colon = JetPsiFactory.createColon(project);
        JetParameterList valueParameterList = function.getValueParameterList();
        assert valueParameterList != null;
        function.addAfter(typeReference, valueParameterList);
        function.addRangeAfter(colon.getFirst(), colon.getSecond(), valueParameterList);
        ReferenceToClassesShortening.compactReferenceToClasses(Collections.singletonList(function));
    }

    public static void removeTypeAnnotation(Project project, JetProperty property) {
        JetTypeReference propertyTypeRef = property.getPropertyTypeRef();
        if (propertyTypeRef == null) return;
        PsiElement identifier = property.getNameIdentifier();
        if (identifier == null) return;
        PsiElement sibling = identifier.getNextSibling();
        if (sibling == null) return;
        PsiElement nextSibling = propertyTypeRef.getNextSibling();
        if (nextSibling == null) return;
        sibling.getParent().getNode().removeRange(sibling.getNode(), nextSibling.getNode());
    }
}
