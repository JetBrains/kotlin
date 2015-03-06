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

package org.jetbrains.kotlin.idea.intentions;

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
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class SpecifyTypeExplicitlyAction extends PsiElementBaseIntentionAction {
    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("specify.type.explicitly.action.family.name");
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        JetTypeReference typeRefParent = PsiTreeUtil.getTopmostParentOfType(element, JetTypeReference.class);
        if (typeRefParent != null) {
            element = typeRefParent;
        }
        JetCallableDeclaration declaration = (JetCallableDeclaration)element.getParent();
        JetType type = getTypeForDeclaration(declaration);
        if (declaration.getTypeReference() == null) {
            addTypeAnnotation(project, editor, declaration, type);
        }
        else {
            declaration.setTypeReference(null);
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (element.getContainingFile() instanceof JetCodeFragment) {
            return false;
        }

        JetTypeReference typeRefParent = PsiTreeUtil.getTopmostParentOfType(element, JetTypeReference.class);
        if (typeRefParent != null) {
            element = typeRefParent;
        }
        PsiElement parent = element.getParent();
        if (!(parent instanceof JetCallableDeclaration)) return false;
        JetCallableDeclaration declaration = (JetCallableDeclaration) parent;

        if (declaration instanceof JetProperty && !PsiTreeUtil.isAncestor(((JetProperty) declaration).getInitializer(), element, false)) {
            if (declaration.getTypeReference() != null) {
                setText(JetBundle.message("specify.type.explicitly.remove.action.name"));
                return true;
            }
            else {
                setText(JetBundle.message("specify.type.explicitly.add.action.name"));
            }
        }
        else if (declaration instanceof JetNamedFunction && declaration.getTypeReference() == null
                 && !((JetNamedFunction) declaration).hasBlockBody()) {
            setText(JetBundle.message("specify.type.explicitly.add.return.type.action.name"));
        }
        else if (declaration instanceof JetParameter && ((JetParameter) declaration).isLoopParameter()) {
            if (declaration.getTypeReference() != null) {
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
        BindingContext bindingContext = ResolvePackage.analyzeFully(declaration.getContainingJetFile());
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            //noinspection ConstantConditions
            if (Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE == diagnostic.getFactory() && declaration == diagnostic.getPsiElement()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static JetType getTypeForDeclaration(@NotNull JetCallableDeclaration declaration) {
        BindingContext bindingContext = ResolvePackage.analyzeFully(declaration.getContainingJetFile());
        CallableDescriptor descriptor = (CallableDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

        JetType type = descriptor != null ? descriptor.getReturnType() : null;
        return type == null ? ErrorUtils.createErrorType("null type") : type;
    }

    @NotNull
    public static Expression createTypeExpressionForTemplate(JetType exprType) {
        ClassifierDescriptor descriptor = exprType.getConstructor().getDeclarationDescriptor();
        boolean isAnonymous = descriptor != null && DescriptorUtils.isAnonymousObject(descriptor);

        Set<JetType> allSupertypes = TypeUtils.getAllSupertypes(exprType);
        List<JetType> types = isAnonymous ? new ArrayList<JetType>() : Lists.newArrayList(exprType);
        types.addAll(allSupertypes);

        return new JetTypeLookupExpression<JetType>(
                types,
                types.iterator().next(),
                JetBundle.message("specify.type.explicitly.add.action.name")
        ) {
            @Override
            protected String getLookupString(JetType element) {
                return IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(element);
            }

            @Override
            protected String getResult(JetType element) {
                return IdeDescriptorRenderers.SOURCE_CODE.renderType(element);
            }
        };
    }

    public static void addTypeAnnotation(Project project, @Nullable Editor editor, @NotNull JetCallableDeclaration declaration, @NotNull JetType exprType) {
        if (editor != null) {
            addTypeAnnotationWithTemplate(project, editor, declaration, exprType);
        }
        else {
            declaration.setTypeReference(anyTypeRef(project));
        }
    }

    public static TemplateEditingAdapter createTypeReferencePostprocessor(final JetCallableDeclaration declaration) {
        return new TemplateEditingAdapter() {
            @Override
            public void templateFinished(Template template, boolean brokenOff) {
                JetTypeReference typeRef = declaration.getTypeReference();
                if (typeRef != null && typeRef.isValid()) {
                    ShortenReferences.DEFAULT.process(typeRef);
                }
            }
        };
    }

    private static void addTypeAnnotationWithTemplate(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull JetCallableDeclaration declaration,
            @NotNull JetType exprType
    ) {
        assert !exprType.isError() : "Unexpected error type, should have been checked before: "
                                     + JetPsiUtil.getElementTextWithContext(declaration) + ", type = " + exprType;

        Expression expression = createTypeExpressionForTemplate(exprType);

        declaration.setTypeReference(anyTypeRef(project));

        PsiDocumentManager.getInstance(project).commitAllDocuments();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        JetTypeReference newTypeRef = declaration.getTypeReference();
        assert newTypeRef != null;
        TemplateBuilderImpl builder = new TemplateBuilderImpl(newTypeRef);
        builder.replaceElement(newTypeRef, expression);

        editor.getCaretModel().moveToOffset(newTypeRef.getNode().getStartOffset());

        TemplateManagerImpl manager = new TemplateManagerImpl(project);
        manager.startTemplate(editor, builder.buildInlineTemplate(), createTypeReferencePostprocessor(declaration));
    }

    private static JetTypeReference anyTypeRef(@NotNull Project project) {
        return JetPsiFactory(project).createType("Any");
    }
}
