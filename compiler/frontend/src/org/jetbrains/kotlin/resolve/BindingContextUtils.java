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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.JetTypeInfo;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice;

import java.util.Collection;

import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION;
import static org.jetbrains.kotlin.diagnostics.Errors.AMBIGUOUS_LABEL;
import static org.jetbrains.kotlin.resolve.BindingContext.*;

public class BindingContextUtils {
    private BindingContextUtils() {
    }

    @Nullable
    public static VariableDescriptor extractVariableDescriptorIfAny(@NotNull BindingContext bindingContext, @Nullable JetElement element, boolean onlyReference) {
        DeclarationDescriptor descriptor = null;
        if (!onlyReference && (element instanceof JetVariableDeclaration || element instanceof JetParameter)) {
            descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        }
        else if (element instanceof JetSimpleNameExpression) {
            descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) element);
        }
        else if (element instanceof JetQualifiedExpression) {
            descriptor = extractVariableDescriptorIfAny(bindingContext, ((JetQualifiedExpression) element).getSelectorExpression(), onlyReference);
        }
        if (descriptor instanceof VariableDescriptor) {
            return (VariableDescriptor) descriptor;
        }
        return null;
    }

    public static void recordFunctionDeclarationToDescriptor(@NotNull BindingTrace trace,
            @NotNull PsiElement psiElement, @NotNull SimpleFunctionDescriptor function) {

        if (function.getKind() != DECLARATION) {
            throw new IllegalArgumentException("function of kind " + function.getKind() + " cannot have declaration");
        }

        trace.record(BindingContext.FUNCTION, psiElement, function);
    }

    @NotNull
    public static <K, V> V getNotNull(
        @NotNull BindingContext bindingContext,
        @NotNull ReadOnlySlice<K, V> slice,
        @NotNull K key
    ) {
        return getNotNull(bindingContext, slice, key, "Value at " + slice + " must not be null for " + key);
    }

    @NotNull
    public static <K, V> V getNotNull(
            @NotNull BindingContext bindingContext,
            @NotNull ReadOnlySlice<K, V> slice,
            @NotNull K key,
            @NotNull String messageIfNull
    ) {
        V value = bindingContext.get(slice, key);
        if (value == null) {
            throw new IllegalStateException(messageIfNull);
        }
        return value;
    }

    @NotNull
    public static DeclarationDescriptor getEnclosingDescriptor(@NotNull BindingContext context, @NotNull JetElement element) {
        JetNamedDeclaration declaration = PsiTreeUtil.getParentOfType(element, JetNamedDeclaration.class);
        if (declaration instanceof JetFunctionLiteral) {
            return getEnclosingDescriptor(context, declaration);
        }
        DeclarationDescriptor descriptor = context.get(DECLARATION_TO_DESCRIPTOR, declaration);
        assert descriptor != null : "No descriptor for named declaration: " + declaration.getText() + "\n(of type " + declaration.getClass() + ")";
        return descriptor;
    }

    public static FunctionDescriptor getEnclosingFunctionDescriptor(@NotNull BindingContext context, @NotNull JetElement element) {
        JetFunction function = PsiTreeUtil.getParentOfType(element, JetFunction.class);
        return (FunctionDescriptor)context.get(DECLARATION_TO_DESCRIPTOR, function);
    }

    public static void reportAmbiguousLabel(
            @NotNull BindingTrace trace,
            @NotNull JetSimpleNameExpression targetLabel,
            @NotNull Collection<DeclarationDescriptor> declarationsByLabel
    ) {
        Collection<PsiElement> targets = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : declarationsByLabel) {
            PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
            assert element != null : "Label can only point to something in the same lexical scope";
            targets.add(element);
        }
        if (!targets.isEmpty()) {
            trace.record(AMBIGUOUS_LABEL_TARGET, targetLabel, targets);
        }
        trace.report(AMBIGUOUS_LABEL.on(targetLabel));
    }

    @Nullable
    public static JetType updateRecordedType(
            @Nullable JetType type,
            @NotNull JetExpression expression,
            @NotNull BindingTrace trace,
            boolean shouldBeMadeNullable
    ) {
        if (type == null) return null;
        if (shouldBeMadeNullable) {
            type = TypeUtils.makeNullable(type);
        }
        trace.record(BindingContext.EXPRESSION_TYPE, expression, type);
        return type;
    }

    @Nullable
    public static JetTypeInfo getRecordedTypeInfo(@NotNull JetExpression expression, @NotNull BindingContext context) {
        if (!context.get(BindingContext.PROCESSED, expression)) return null;
        DataFlowInfo dataFlowInfo = BindingContextUtilPackage.getDataFlowInfo(context, expression);
        JetType type = context.get(BindingContext.EXPRESSION_TYPE, expression);
        return JetTypeInfo.create(type, dataFlowInfo);
    }

    public static boolean isExpressionWithValidReference(
            @NotNull JetExpression expression,
            @NotNull BindingContext context
    ) {
        if (expression instanceof JetCallExpression) {
            ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(expression, context);
            return resolvedCall instanceof VariableAsFunctionResolvedCall;
        }
        return expression instanceof JetReferenceExpression;
    }

    public static boolean isVarCapturedInClosure(BindingContext bindingContext, DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof VariableDescriptor) || descriptor instanceof PropertyDescriptor) return false;
        VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
        return bindingContext.get(CAPTURED_IN_CLOSURE, variableDescriptor) != null && variableDescriptor.isVar();
    }

    @NotNull
    public static Pair<FunctionDescriptor, PsiElement> getContainingFunctionSkipFunctionLiterals(
            @Nullable DeclarationDescriptor startDescriptor,
            boolean strict
    ) {
        FunctionDescriptor containingFunctionDescriptor = DescriptorUtils.getParentOfType(startDescriptor, FunctionDescriptor.class, strict);
        PsiElement containingFunction =
                containingFunctionDescriptor != null ? DescriptorToSourceUtils.getSourceFromDescriptor(containingFunctionDescriptor) : null;
        while (containingFunction instanceof JetFunctionLiteral) {
            containingFunctionDescriptor = DescriptorUtils.getParentOfType(containingFunctionDescriptor, FunctionDescriptor.class);
            containingFunction = containingFunctionDescriptor != null ? DescriptorToSourceUtils
                    .getSourceFromDescriptor(containingFunctionDescriptor) : null;
        }

        return new Pair<FunctionDescriptor, PsiElement>(containingFunctionDescriptor, containingFunction);
    }

}
