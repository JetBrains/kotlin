/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.Sets;
import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.presentation.JetClassPresenter;
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class QuickFixUtil {
    private QuickFixUtil() {
    }

    public static boolean removePossiblyWhiteSpace(ASTDelegatePsiElement element, PsiElement possiblyWhiteSpace) {
        if (possiblyWhiteSpace instanceof PsiWhiteSpace) {
            element.deleteChildInternal(possiblyWhiteSpace.getNode());
            return true;
        }
        return false;
    }

    @Nullable
    public static <T extends PsiElement> T getParentElementOfType(Diagnostic diagnostic, Class<T> aClass) {
        return PsiTreeUtil.getParentOfType(diagnostic.getPsiElement(), aClass, false);
    }

    @Nullable
    public static JetType getDeclarationReturnType(JetNamedDeclaration declaration) {
        PsiFile file = declaration.getContainingFile();
        if (!(file instanceof JetFile)) return null;
        BindingContext bindingContext = ResolvePackage.getBindingContext((JetFile) file);
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
        if (!(descriptor instanceof CallableDescriptor)) return null;
        JetType type = ((CallableDescriptor) descriptor).getReturnType();
        if (type instanceof DeferredType) {
            type = ((DeferredType) type).getDelegate();
        }
        return type;
    }

    @Nullable
    public static JetType findLowerBoundOfOverriddenCallablesReturnTypes(BindingContext context, JetDeclaration callable) {
        DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, callable);
        if (!(descriptor instanceof CallableDescriptor)) {
            return null;
        }

        JetType matchingReturnType = null;
        for (CallableDescriptor overriddenDescriptor : ((CallableDescriptor) descriptor).getOverriddenDescriptors()) {
            JetType overriddenReturnType = overriddenDescriptor.getReturnType();
            if (overriddenReturnType == null) {
                return null;
            }
            if (matchingReturnType == null || JetTypeChecker.DEFAULT.isSubtypeOf(overriddenReturnType, matchingReturnType)) {
                matchingReturnType = overriddenReturnType;
            }
            else if (!JetTypeChecker.DEFAULT.isSubtypeOf(matchingReturnType, overriddenReturnType)) {
                return null;
            }
        }
        return matchingReturnType;
    }

    public static boolean canModifyElement(@NotNull PsiElement element) {
        return element.isWritable() && !BuiltInsReferenceResolver.isFromBuiltIns(element);
    }

    @Nullable
    public static JetParameterList getParameterListOfCallee(@NotNull JetCallExpression callExpression) {
        BindingContext context = ResolvePackage.getBindingContext(callExpression.getContainingJetFile());
        ResolvedCall<?> resolvedCall = BindingContextUtilPackage.getResolvedCall(callExpression, context);
        if (resolvedCall == null) return null;
        PsiElement declaration = safeGetDeclaration(resolvedCall);
        if (declaration instanceof JetFunction) {
            return ((JetFunction) declaration).getValueParameterList();
        }
        if (declaration instanceof JetClass) {
            return ((JetClass) declaration).getPrimaryConstructorParameterList();
        }
        return null;
    }

    @Nullable
    public static PsiElement safeGetDeclaration(@NotNull ResolvedCall<?> resolvedCall) {
        List<PsiElement> declarations = DescriptorToSourceUtils.descriptorToDeclarations(resolvedCall.getResultingDescriptor());
        //do not create fix if descriptor has more than one overridden declaration
        if (declarations.size() == 1) {
            return declarations.iterator().next();
        }
        return null;
    }

    @Nullable
    public static JetParameter getParameterCorrespondingToFunctionLiteralPassedOutsideArgumentList(@NotNull JetFunctionLiteralExpression functionLiteralExpression) {
        if (!(functionLiteralExpression.getParent() instanceof JetCallExpression)) {
            return null;
        }
        JetCallExpression callExpression = (JetCallExpression) functionLiteralExpression.getParent();
        JetParameterList parameterList = getParameterListOfCallee(callExpression);
        if (parameterList == null) return null;
        return parameterList.getParameters().get(parameterList.getParameters().size() - 1);
    }

    @Nullable
    public static JetParameter getParameterCorrespondingToValueArgumentPassedInCall(@NotNull JetValueArgument valueArgument) {
        if (!(valueArgument.getParent() instanceof JetValueArgumentList)) {
            return null;
        }
        JetValueArgumentList valueArgumentList = (JetValueArgumentList) valueArgument.getParent();
        if (!(valueArgumentList.getParent() instanceof JetCallExpression)) {
            return null;
        }
        JetCallExpression callExpression = (JetCallExpression) valueArgumentList.getParent();
        JetParameterList parameterList = getParameterListOfCallee(callExpression);
        if (parameterList == null) return null;
        List<JetParameter> parameters = parameterList.getParameters();
        int position = valueArgumentList.getArguments().indexOf(valueArgument);
        if (position == -1) return null;

        if (valueArgument.isNamed()) {
            JetValueArgumentName valueArgumentName = valueArgument.getArgumentName();
            JetSimpleNameExpression referenceExpression = valueArgumentName == null ? null : valueArgumentName.getReferenceExpression();
            String valueArgumentNameAsString = referenceExpression == null ? null : referenceExpression.getReferencedName();
            if (valueArgumentNameAsString == null) return null;

            for (JetParameter parameter: parameters) {
                if (valueArgumentNameAsString.equals(parameter.getName())) {
                    return parameter;
                }
            }
            return null;
        }
        else {
            if (position >= parameters.size()) return null;
            return parameters.get(position);
        }
    }

    private static boolean equalOrLastInThenOrElse(JetExpression thenOrElse, JetExpression expression) {
        if (thenOrElse == expression) return true;
        return thenOrElse instanceof JetBlockExpression && expression.getParent() == thenOrElse &&
               PsiTreeUtil.getNextSiblingOfType(expression, JetExpression.class) == null;
    }

    public static boolean canEvaluateTo(JetExpression parent, JetExpression child) {
        if (parent == null || child == null) {
            return false;
        }
        while (parent != child) {
            if (child.getParent() instanceof JetParenthesizedExpression) {
                child = (JetExpression) child.getParent();
                continue;
            }
            JetIfExpression jetIfExpression = PsiTreeUtil.getParentOfType(child, JetIfExpression.class, true);
            if (jetIfExpression == null) return false;
            if (!equalOrLastInThenOrElse(jetIfExpression.getThen(), child) && !equalOrLastInThenOrElse(jetIfExpression.getElse(), child)) {
                return false;
            }
            child = jetIfExpression;
        }
        return true;
    }

    public static boolean canFunctionOrGetterReturnExpression(@NotNull JetDeclaration functionOrGetter, @NotNull JetExpression expression) {
        if (functionOrGetter instanceof JetFunctionLiteral) {
            JetBlockExpression functionLiteralBody = ((JetFunctionLiteral) functionOrGetter).getBodyExpression();
            PsiElement returnedElement = functionLiteralBody == null ? null : functionLiteralBody.getLastChild();
            return returnedElement instanceof JetExpression && canEvaluateTo((JetExpression) returnedElement, expression);
        }
        else {
            if (functionOrGetter instanceof JetWithExpressionInitializer && canEvaluateTo(((JetWithExpressionInitializer) functionOrGetter).getInitializer(), expression)) {
                return true;
            }
            JetReturnExpression returnExpression = PsiTreeUtil.getParentOfType(expression, JetReturnExpression.class);
            return returnExpression != null && canEvaluateTo(returnExpression.getReturnedExpression(), expression);
        }
    }

    @ReadOnly
    @NotNull
    public static Set<String> getUsedParameters(
            @NotNull JetCallElement callElement,
            @Nullable JetValueArgument ignoreArgument,
            @NotNull CallableDescriptor callableDescriptor
    ) {
        Set<String> usedParameters = Sets.newHashSet();
        boolean isPositionalArgument = true;
        int idx = 0;
        for (ValueArgument argument : callElement.getValueArguments()) {
            if (argument.isNamed()) {
                JetValueArgumentName name = argument.getArgumentName();
                assert name != null : "Named argument's name cannot be null";
                if (argument != ignoreArgument) {
                    usedParameters.add(name.getText());
                }
                isPositionalArgument = false;
            }
            else if (isPositionalArgument) {
                if (callableDescriptor.getValueParameters().size() > idx) {
                    ValueParameterDescriptor parameter = callableDescriptor.getValueParameters().get(idx);
                    if (argument != ignoreArgument) {
                        usedParameters.add(parameter.getName().asString());
                    }
                    idx++;
                }
            }
        }

        return usedParameters;
    }

    static class ClassCandidateListCellRenderer extends PsiElementListCellRenderer<JetClass> {
        private final JetClassPresenter presenter = new JetClassPresenter();

        @Override
        public String getElementText(JetClass element) {
            return presenter.getPresentation(element).getPresentableText();
        }

        @Nullable
        @Override
        protected String getContainerText(JetClass element, String name) {
            return presenter.getPresentation(element).getLocationString();
        }

        @Override
        protected int getIconFlags() {
            return 0;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            return super.getListCellRendererComponent(list, ((ClassCandidate) value).getJetClass(), index, isSelected, cellHasFocus);
        }
    }
}
