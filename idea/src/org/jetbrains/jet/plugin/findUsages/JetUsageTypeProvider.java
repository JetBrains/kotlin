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

package org.jetbrains.jet.plugin.findUsages;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProviderEx;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

public class JetUsageTypeProvider implements UsageTypeProviderEx {
    private static final Function1<PsiElement, Boolean> IS_ASSIGNMENT = new Function1<PsiElement, Boolean>() {
        @Override
        public Boolean invoke(@Nullable PsiElement input) {
            return input != null && JetPsiUtil.isAssignment(input);
        }
    };

    @Nullable
    @Override
    public UsageType getUsageType(PsiElement element) {
        return getUsageType(element, UsageTarget.EMPTY_ARRAY);
    }

    @Nullable
    @Override
    public UsageType getUsageType(PsiElement element, @NotNull UsageTarget[] targets) {
        if (element == null) return null;

        UsageType usageType = getCommonUsageType(element);
        if (usageType != null) return usageType;

        JetSimpleNameExpression reference = PsiTreeUtil.getParentOfType(element, JetSimpleNameExpression.class, false);
        if (reference == null) return null;

        BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) element.getContainingFile()).getBindingContext();
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, reference);

        if (descriptor instanceof ClassifierDescriptor) {
            return getClassUsageType(element);
        }
        if (descriptor instanceof VariableDescriptor) {
            return getVariableUsageType(element);
        }
        if (descriptor instanceof FunctionDescriptor) {
            return getFunctionUsageType(element, (FunctionDescriptor) descriptor);
        }

        return null;
    }

    private static UsageType getCommonUsageType(@NotNull PsiElement element) {
        JetImportDirective importDirective = PsiTreeUtil.getParentOfType(element, JetImportDirective.class, false);
        if (importDirective != null) return JetUsageTypes.IMPORT_DIRECTIVE;

        JetCallableReferenceExpression callableReference =
                PsiTreeUtil.getParentOfType(element, JetCallableReferenceExpression.class, false);
        if (callableReference != null && PsiTreeUtil.isAncestor(callableReference.getCallableReference(), element, false)) {
            return JetUsageTypes.CALLABLE_REFERENCE;
        }

        return null;
    }

    @Nullable
    private static UsageType getClassUsageType(@NotNull PsiElement element) {
        JetTypeParameter typeParameter = PsiTreeUtil.getParentOfType(element, JetTypeParameter.class, false);
        if (typeParameter != null && PsiTreeUtil.isAncestor(typeParameter.getExtendsBound(), element, false)) {
            return JetUsageTypes.TYPE_CONSTRAINT;
        }

        JetTypeConstraint typeConstraint = PsiTreeUtil.getParentOfType(element, JetTypeConstraint.class, false);
        if (typeConstraint != null && PsiTreeUtil.isAncestor(typeConstraint.getBoundTypeReference(), element, false)) {
            return JetUsageTypes.TYPE_CONSTRAINT;
        }

        JetDelegationSpecifier delegationSpecifier = PsiTreeUtil.getParentOfType(element, JetDelegationSpecifier.class, false);
        if (delegationSpecifier != null
            && (delegationSpecifier == element || PsiTreeUtil.isAncestor(delegationSpecifier.getTypeReference(), element, false))) {
            return JetUsageTypes.SUPER_TYPE;
        }

        JetTypedef typedef = PsiTreeUtil.getParentOfType(element, JetTypedef.class, false);
        if (typedef != null && PsiTreeUtil.isAncestor(typedef.getTypeReference(), element, false)) {
            return JetUsageTypes.TYPE_DEFINITION;
        }

        JetTypeProjection typeProjection = PsiTreeUtil.getParentOfType(element, JetTypeProjection.class, false);
        if (typeProjection != null) return JetUsageTypes.TYPE_ARGUMENT;

        JetParameter parameter = PsiTreeUtil.getParentOfType(element, JetParameter.class, false);
        if (parameter != null) {
            if (PsiTreeUtil.isAncestor(parameter.getTypeReference(), element, false)) return JetUsageTypes.VALUE_PARAMETER_TYPE;
        }

        JetProperty property = PsiTreeUtil.getParentOfType(element, JetProperty.class, false);
        if (property != null) {
            if (PsiTreeUtil.isAncestor(property.getTypeRef(), element, false)) {
                return property.isLocal() ? JetUsageTypes.LOCAL_VARIABLE_TYPE : JetUsageTypes.NON_LOCAL_PROPERTY_TYPE;
            }
            if (PsiTreeUtil.isAncestor(property.getReceiverTypeRef(), element, false)) {
                return JetUsageTypes.EXTENSION_RECEIVER_TYPE;
            }
        }

        JetFunction function = PsiTreeUtil.getParentOfType(element, JetFunction.class, false);
        if (function != null) {
            if (PsiTreeUtil.isAncestor(function.getReturnTypeRef(), element, false)) {
                return JetUsageTypes.FUNCTION_RETURN_TYPE;
            }
            if (PsiTreeUtil.isAncestor(function.getReceiverTypeRef(), element, false)) {
                return JetUsageTypes.EXTENSION_RECEIVER_TYPE;
            }
        }

        JetIsExpression isExpression = PsiTreeUtil.getParentOfType(element, JetIsExpression.class, false);
        if (isExpression != null && PsiTreeUtil.isAncestor(isExpression.getTypeRef(), element, false)) {
            return JetUsageTypes.IS;
        }

        JetBinaryExpressionWithTypeRHS typeRHSExpression =
                PsiTreeUtil.getParentOfType(element, JetBinaryExpressionWithTypeRHS.class, false);
        if (typeRHSExpression != null && PsiTreeUtil.isAncestor(typeRHSExpression.getRight(), element, false)) {
            IElementType opType = typeRHSExpression.getOperationReference().getReferencedNameElementType();
            if (opType == JetTokens.AS_KEYWORD || opType == JetTokens.AS_SAFE) return JetUsageTypes.AS;
        }

        JetDotQualifiedExpression dotQualifiedExpression = PsiTreeUtil.getParentOfType(element, JetDotQualifiedExpression.class, false);
        if (dotQualifiedExpression != null
            && dotQualifiedExpression.getReceiverExpression() instanceof JetSimpleNameExpression
            && PsiTreeUtil.isAncestor(dotQualifiedExpression.getReceiverExpression(), element, false)) {

            return JetUsageTypes.CLASS_OBJECT_ACCESS;
        }

        JetSuperExpression superExpression = PsiTreeUtil.getParentOfType(element, JetSuperExpression.class, false);
        if (superExpression != null && PsiTreeUtil.isAncestor(superExpression.getSuperTypeQualifier(), element, false)) {
            return JetUsageTypes.SUPER_TYPE_QUALIFIER;
        }

        return null;
    }

    @Nullable
    private static UsageType getFunctionUsageType(@NotNull PsiElement element, @NotNull FunctionDescriptor descriptor) {
        if (descriptor instanceof ConstructorDescriptor) {
            JetAnnotationEntry annotation = PsiTreeUtil.getParentOfType(element, JetAnnotationEntry.class, false);
            if (annotation != null && PsiTreeUtil.isAncestor(annotation.getTypeReference(), element, false)) {
                return JetUsageTypes.ANNOTATION_TYPE;
            }
        }

        JetCallExpression callExpression = PsiTreeUtil.getParentOfType(element, JetCallExpression.class, false);
        if (callExpression != null
            && callExpression.getCalleeExpression() instanceof JetSimpleNameExpression
            && PsiTreeUtil.isAncestor(callExpression.getCalleeExpression(), element, false)) {

            return (descriptor instanceof ConstructorDescriptor) ? JetUsageTypes.INSTANTIATION : JetUsageTypes.FUNCTION_CALL;
        }

        return null;
    }

    @Nullable
    private static UsageType getVariableUsageType(@NotNull PsiElement element) {
        JetDotQualifiedExpression dotQualifiedExpression = PsiTreeUtil.getParentOfType(element, JetDotQualifiedExpression.class, false);
        if (dotQualifiedExpression != null) {
            if (PsiTreeUtil.isAncestor(dotQualifiedExpression.getReceiverExpression(), element, false)) {
                return JetUsageTypes.RECEIVER;
            }

            PsiElement parent = dotQualifiedExpression.getParent();
            if (parent instanceof JetDotQualifiedExpression
                && PsiTreeUtil.isAncestor(((JetDotQualifiedExpression) parent).getReceiverExpression(), element, false)) {
                return JetUsageTypes.RECEIVER;
            }

            return JetUsageTypes.SELECTOR;
        }

        JetBinaryExpression binaryExpression =
                PsiUtilPackage.getParentByTypeAndPredicate(element, JetBinaryExpression.class, false, IS_ASSIGNMENT);
        if (binaryExpression != null && PsiTreeUtil.isAncestor(binaryExpression.getLeft(), element, false)) {
            return UsageType.WRITE;
        }

        JetSimpleNameExpression simpleNameExpression = PsiTreeUtil.getParentOfType(element, JetSimpleNameExpression.class, false);
        if (simpleNameExpression != null) {
            return UsageType.READ;
        }

        return null;
    }
}

