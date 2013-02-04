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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.ReferenceToClassesShortening;
import org.jetbrains.jet.plugin.project.AnalyzeSingleFileUtil;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collections;

public class SpecifyTypeExplicitlyAction extends PsiElementBaseIntentionAction {
    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("specify.type.explicitly.action.family.name");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        JetTypeReference typeRefParent = PsiTreeUtil.getTopmostParentOfType(element, JetTypeReference.class);
        if (typeRefParent != null) {
            element = typeRefParent;
        }
        PsiElement parent = element.getParent();
        JetType type = getTypeForDeclaration((JetNamedDeclaration) parent);
        assert !ErrorUtils.isErrorType(type) : "Unexpected error type: " + element.getText();
        if (parent instanceof JetProperty) {
            JetProperty property = (JetProperty) parent;
            if (property.getTypeRef() == null) {
                addTypeAnnotation(project, property, type);
            }
            else {
                removeTypeAnnotation(property);
            }
        }
        else if (parent instanceof JetParameter) {
            JetParameter parameter = (JetParameter) parent;
            if (parameter.getTypeReference() == null) {
                addTypeAnnotation(project, parameter, type);
            }
            else {
                removeTypeAnnotation(parameter);
            }
        }
        else if (parent instanceof JetNamedFunction) {
            JetNamedFunction function = (JetNamedFunction) parent;
            assert function.getReturnTypeRef() == null;
            addTypeAnnotation(project, function, type);
        }
        else {
            throw new IllegalStateException("Unexpected parent: " + parent);
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        JetTypeReference typeRefParent = PsiTreeUtil.getTopmostParentOfType(element, JetTypeReference.class);
        if (typeRefParent != null) {
            element = typeRefParent;
        }
        PsiElement parent = element.getParent();
        if (!(parent instanceof JetNamedDeclaration)) {
            return false;
        }
        JetNamedDeclaration declaration = (JetNamedDeclaration) parent;
        if (declaration instanceof JetProperty && !PsiTreeUtil.isAncestor(((JetProperty) declaration).getInitializer(), element, false)) {
            if (((JetProperty) declaration).getTypeRef() != null) {
                setText(JetBundle.message("specify.type.explicitly.remove.action.name"));
                return true;
            }
            else {
                setText(JetBundle.message("specify.type.explicitly.add.action.name"));
            }
        }
        else if (declaration instanceof JetNamedFunction && ((JetNamedFunction) declaration).getReturnTypeRef() == null
                 && !((JetNamedFunction) declaration).hasBlockBody()) {
            setText(JetBundle.message("specify.type.explicitly.add.return.type.action.name"));
        }
        else if (declaration instanceof JetParameter && JetNodeTypes.LOOP_PARAMETER == declaration.getNode().getElementType()) {
            if (((JetParameter) declaration).getTypeReference() != null) {
                setText(JetBundle.message("specify.type.explicitly.remove.action.name"));
                return true;
            }
            else {
                setText(JetBundle.message("specify.type.explicitly.add.action.name"));
            }
        }
        else {
            return false;
        }

        if (ErrorUtils.isErrorType(getTypeForDeclaration(declaration))) {
            return false;
        }
        return !hasPublicMemberDiagnostic(declaration);
    }


    private static boolean hasPublicMemberDiagnostic(@NotNull JetNamedDeclaration declaration) {
        BindingContext bindingContext = AnalyzeSingleFileUtil.getContextForSingleFile((JetFile) declaration.getContainingFile());
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            //noinspection ConstantConditions
            if (Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE == diagnostic.getFactory() && declaration == diagnostic.getPsiElement()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    protected static JetType getTypeForDeclaration(@NotNull JetNamedDeclaration declaration) {
        BindingContext bindingContext = AnalyzeSingleFileUtil.getContextForSingleFile((JetFile) declaration.getContainingFile());
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

        JetType type;
        if (descriptor instanceof VariableDescriptor) {
            type = ((VariableDescriptor) descriptor).getType();
        }
        else if (descriptor instanceof SimpleFunctionDescriptor) {
            type = ((SimpleFunctionDescriptor) descriptor).getReturnType();
        }
        else {
            return ErrorUtils.createErrorType("unknown declaration type");
        }

        return type == null ? ErrorUtils.createErrorType("null type") : type;
    }

    public static void addTypeAnnotation(Project project, JetProperty property, @NotNull JetType exprType) {
        if (property.getTypeRef() != null) return;
        PsiElement anchor = property.getNameIdentifier();
        if (anchor == null) return;
        anchor = anchor.getNextSibling();
        if (anchor != null) {
            if (!(anchor instanceof PsiWhiteSpace)) {
                return;
            }
        }

        // For enum entry expression, we want type "MyEntry" instead of "MyEntry.<class-object>.ENTRY1"
        ClassifierDescriptor typeClassifier = exprType.getConstructor().getDeclarationDescriptor();
        assert typeClassifier != null;
        if (DescriptorUtils.isEnumEntry(typeClassifier)) {
            ClassDescriptor enumClass = (ClassDescriptor) typeClassifier.getContainingDeclaration().getContainingDeclaration();
            assert enumClass != null;
            exprType = enumClass.getDefaultType();
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

    public static void addTypeAnnotation(Project project, JetParameter parameter, @NotNull JetType exprType) {
        JetTypeReference typeReference = JetPsiFactory.createType(project, DescriptorRenderer.TEXT.renderType(exprType));
        Pair<PsiElement, PsiElement> colon = JetPsiFactory.createColon(project);
        parameter.addAfter(typeReference, parameter.getNameIdentifier());
        parameter.addRangeAfter(colon.getFirst(), colon.getSecond(), parameter.getNameIdentifier());
        ReferenceToClassesShortening.compactReferenceToClasses(Collections.singletonList(parameter));
    }

    private static void removeTypeAnnotation(@NotNull JetNamedDeclaration property, @Nullable JetTypeReference typeReference) {
        if (typeReference == null) return;
        PsiElement identifier = property.getNameIdentifier();
        if (identifier == null) return;
        PsiElement sibling = identifier.getNextSibling();
        if (sibling == null) return;
        PsiElement nextSibling = typeReference.getNextSibling();
        sibling.getParent().getNode().removeRange(sibling.getNode(), nextSibling == null ? null : nextSibling.getNode());
    }

    public static void removeTypeAnnotation(JetProperty property) {
        removeTypeAnnotation(property, property.getTypeRef());
    }

    public static void removeTypeAnnotation(JetParameter parameter) {
        removeTypeAnnotation(parameter, parameter.getTypeReference());
    }
}
