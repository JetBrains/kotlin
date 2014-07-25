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

import com.google.common.collect.Lists;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateEditingAdapter;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

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
        if (parent instanceof JetProperty) {
            JetProperty property = (JetProperty) parent;
            if (property.getTypeRef() == null) {
                addTypeAnnotation(project, editor, property, type);
            }
            else {
                removeTypeAnnotation(property);
            }
        }
        else if (parent instanceof JetParameter) {
            JetParameter parameter = (JetParameter) parent;
            if (parameter.getTypeReference() == null) {
                addTypeAnnotation(project, editor, parameter, type);
            }
            else {
                removeTypeAnnotation(parameter);
            }
        }
        else if (parent instanceof JetNamedFunction) {
            JetNamedFunction function = (JetNamedFunction) parent;
            assert function.getReturnTypeRef() == null;
            addTypeAnnotation(project, editor, function, type);
        }
        else {
            throw new IllegalStateException("Unexpected parent: " + parent);
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (element.getContainingFile() instanceof JetCodeFragment) {
            return false;
        }

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
        else if (declaration instanceof JetParameter && ((JetParameter) declaration).isLoopParameter()) {
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

        if (getTypeForDeclaration(declaration).isError()) {
            return false;
        }
        return !hasPublicMemberDiagnostic(declaration);
    }


    private static boolean hasPublicMemberDiagnostic(@NotNull JetNamedDeclaration declaration) {
        BindingContext bindingContext = ResolvePackage.getBindingContext(declaration.getContainingJetFile());
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            //noinspection ConstantConditions
            if (Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE == diagnostic.getFactory() && declaration == diagnostic.getPsiElement()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static JetType getTypeForDeclaration(@NotNull JetNamedDeclaration declaration) {
        BindingContext bindingContext = ResolvePackage.getBindingContext(declaration.getContainingJetFile());
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

    public static void addTypeAnnotation(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull JetProperty property,
            @NotNull JetType exprType
    ) {
        if (property.getTypeRef() != null) {
            return;
        }

        PsiElement anchor = property.getNameIdentifier();
        if (anchor == null) {
            return;
        }

        addTypeAnnotation(project, editor, property, anchor, exprType);
    }

    public static void addTypeAnnotation(Project project, @Nullable Editor editor, JetFunction function, @NotNull JetType exprType) {
        JetParameterList valueParameterList = function.getValueParameterList();
        assert valueParameterList != null;
        if (editor != null) {
            addTypeAnnotation(project, editor, function, valueParameterList, exprType);
        }
        else {
            addTypeAnnotationSilently(project, function, valueParameterList);
        }
    }

    public static void addTypeAnnotation(Project project, @Nullable Editor editor, JetParameter parameter, @NotNull JetType exprType) {
        if (editor != null) {
            addTypeAnnotation(project, editor, parameter, parameter.getNameIdentifier(), exprType);
        }
        else {
            addTypeAnnotationSilently(project, parameter, parameter.getNameIdentifier());
        }
    }

    private static void addTypeAnnotation(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull final JetNamedDeclaration namedDeclaration,
            @NotNull PsiElement anchor,
            @NotNull JetType exprType
    ) {
        assert !exprType.isError() : "Unexpected error type: " + namedDeclaration.getText();

        ClassifierDescriptor descriptor = exprType.getConstructor().getDeclarationDescriptor();
        boolean isAnonymous = descriptor != null && DescriptorUtils.isAnonymousObject(descriptor);

        Set<JetType> allSupertypes = TypeUtils.getAllSupertypes(exprType);
        List<JetType> types = isAnonymous ? new ArrayList<JetType>() : Lists.newArrayList(exprType);
        types.addAll(allSupertypes);

        Expression expression = new JetTypeLookupExpression<JetType>(
                types,
                types.iterator().next(),
                JetBundle.message("specify.type.explicitly.add.action.name")
        ) {
            @Override
            protected String getLookupString(JetType element) {
                return DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(element);
            }

            @Override
            protected String getResult(JetType element) {
                return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(element);
            }
        };

        addTypeAnnotationSilently(project, namedDeclaration, anchor);

        PsiDocumentManager.getInstance(project).commitAllDocuments();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        JetTypeReference newTypeRef = getTypeRef(namedDeclaration);
        TemplateBuilderImpl builder = new TemplateBuilderImpl(newTypeRef);
        builder.replaceElement(newTypeRef, expression);

        editor.getCaretModel().moveToOffset(newTypeRef.getNode().getStartOffset());

        TemplateManagerImpl manager = new TemplateManagerImpl(project);
        manager.startTemplate(editor, builder.buildInlineTemplate(), new TemplateEditingAdapter() {
            @Override
            public void templateFinished(Template template, boolean brokenOff) {
                ShortenReferences.INSTANCE$.process(getTypeRef(namedDeclaration));
            }
        });
    }

    private static void addTypeAnnotationSilently(Project project, JetNamedDeclaration namedDeclaration, PsiElement anchor) {
        JetPsiFactory psiFactory = JetPsiFactory(namedDeclaration);
        namedDeclaration.addAfter(psiFactory.createType("Any"), anchor);
        namedDeclaration.addAfter(psiFactory.createColon(), anchor);
    }

    @Nullable
    private static JetTypeReference getTypeRef(@NotNull JetNamedDeclaration namedDeclaration) {
        if (namedDeclaration instanceof JetProperty) {
            return ((JetProperty) namedDeclaration).getTypeRef();
        }
        else if (namedDeclaration instanceof JetParameter) {
            return ((JetParameter) namedDeclaration).getTypeReference();
        }
        else if (namedDeclaration instanceof JetFunction) {
            return ((JetFunction) namedDeclaration).getReturnTypeRef();
        }
        assert false : "Wrong namedDeclaration: " + namedDeclaration.getText();
        return null;
    }

    private static void removeTypeAnnotation(@Nullable PsiElement removeAfter, @Nullable JetTypeReference typeReference) {
        if (removeAfter == null) return;
        if (typeReference == null) return;
        PsiElement sibling = removeAfter.getNextSibling();
        if (sibling == null) return;
        PsiElement nextSibling = typeReference.getNextSibling();
        sibling.getParent().getNode().removeRange(sibling.getNode(), nextSibling == null ? null : nextSibling.getNode());
    }

    public static void removeTypeAnnotation(JetVariableDeclaration property) {
        removeTypeAnnotation(property.getNameIdentifier(), property.getTypeRef());
    }

    public static void removeTypeAnnotation(JetParameter parameter) {
        removeTypeAnnotation(parameter.getNameIdentifier(), parameter.getTypeReference());
    }

    public static void removeTypeAnnotation(JetFunction function) {
        removeTypeAnnotation(function.getValueParameterList(), function.getReturnTypeRef());
    }
}
