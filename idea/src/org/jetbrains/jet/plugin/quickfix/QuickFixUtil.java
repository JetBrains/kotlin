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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver;

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
        BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) file).getBindingContext();
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
        if (!(descriptor instanceof CallableDescriptor)) return null;
        JetType type = ((CallableDescriptor) descriptor).getReturnType();
        if (type instanceof DeferredType) {
            type = ((DeferredType) type).getActualType();
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
            if (matchingReturnType == null || JetTypeChecker.INSTANCE.isSubtypeOf(overriddenReturnType, matchingReturnType)) {
                matchingReturnType = overriddenReturnType;
            }
            else if (!JetTypeChecker.INSTANCE.isSubtypeOf(matchingReturnType, overriddenReturnType)) {
                return null;
            }
        }
        return matchingReturnType;
    }

    public static boolean canModifyElement(@NotNull PsiElement element) {
        return element.isWritable() && !BuiltInsReferenceResolver.isFromBuiltIns(element);
    }

    @Nullable
    public static JetParameterList getParameterListOfCalledFunction(@NotNull JetCallExpression callExpression) {
        BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) callExpression.getContainingFile()).getBindingContext();
        ResolvedCall<? extends CallableDescriptor> resolvedCall = context.get(BindingContext.RESOLVED_CALL, callExpression.getCalleeExpression());
        if (resolvedCall == null) return null;
        PsiElement functionDeclaration = BindingContextUtils.descriptorToDeclaration(context, resolvedCall.getCandidateDescriptor());
        if (functionDeclaration instanceof JetFunction) {
            return ((JetFunction) functionDeclaration).getValueParameterList();
        }
        return null;
    }

    @Nullable
    public static JetParameter getFunctionParameterCorrespondingToFunctionLiteralPassedOutsideArgumentList(@NotNull JetFunctionLiteralExpression functionLiteralExpression) {
        if (!(functionLiteralExpression.getParent() instanceof JetCallExpression)) {
            return null;
        }
        JetCallExpression callExpression = (JetCallExpression) functionLiteralExpression.getParent();
        JetParameterList parameterList = getParameterListOfCalledFunction(callExpression);
        if (parameterList == null) return null;
        return parameterList.getParameters().get(parameterList.getParameters().size() - 1);
    }

    @Nullable
    public static JetParameter getFunctionParameterCorrespondingToValueArgumentPassedInCall(@NotNull JetValueArgument valueArgument) {
        if (!(valueArgument.getParent() instanceof JetValueArgumentList)) {
            return null;
        }
        JetValueArgumentList valueArgumentList = (JetValueArgumentList) valueArgument.getParent();
        if (!(valueArgumentList.getParent() instanceof JetCallExpression)) {
            return null;
        }
        JetCallExpression callExpression = (JetCallExpression) valueArgumentList.getParent();
        JetParameterList parameterList = getParameterListOfCalledFunction(callExpression);
        if (parameterList == null) return null;
        int position = valueArgumentList.getArguments().indexOf(valueArgument);
        if (position == -1) return null;

        if (valueArgument.isNamed()) {
            JetValueArgumentName valueArgumentName = valueArgument.getArgumentName();
            JetSimpleNameExpression referenceExpression = valueArgumentName == null ? null : valueArgumentName.getReferenceExpression();
            String valueArgumentNameAsString = referenceExpression == null ? null : referenceExpression.getReferencedName();
            if (valueArgumentNameAsString == null) return null;

            for (JetParameter parameter: parameterList.getParameters()) {
                if (valueArgumentNameAsString.equals(parameter.getName())) {
                    return parameter;
                }
            }
            return null;
        }
        else {
            return parameterList.getParameters().get(position);
        }
    }
}
