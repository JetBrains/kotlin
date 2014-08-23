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

package org.jetbrains.jet.plugin.codeInsight;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastUtils;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.Qualifier;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;

import java.util.*;

public final class TipsManager {

    private TipsManager() {
    }

    @NotNull
    public static Collection<DeclarationDescriptor> getReferenceVariants(
            @NotNull JetSimpleNameExpression expression,
            @NotNull BindingContext context
    ) {
        JetExpression receiverExpression = PsiUtilPackage.getReceiverExpression(expression);
        PsiElement parent = expression.getParent();
        boolean inPositionForCompletionWithReceiver = parent instanceof JetCallExpression || parent instanceof JetQualifiedExpression;
        if (receiverExpression != null && inPositionForCompletionWithReceiver) {
            Set<DeclarationDescriptor> descriptors = new HashSet<DeclarationDescriptor>();
            // Process as call expression
            JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);
            Qualifier qualifier = context.get(BindingContext.QUALIFIER, receiverExpression);
            if (qualifier != null && resolutionScope != null) {
                // It's impossible to add extension function for package or class (if it's class object, expression type is not null)
                descriptors.addAll(new HashSet<DeclarationDescriptor>(excludePrivateDescriptors(qualifier.getScope().getAllDescriptors())));
            }

            JetType expressionType = context.get(BindingContext.EXPRESSION_TYPE, receiverExpression);
            if (expressionType != null && resolutionScope != null && !expressionType.isError()) {
                ExpressionReceiver receiverValue = new ExpressionReceiver(receiverExpression, expressionType);

                DataFlowInfo info = context.get(BindingContext.EXPRESSION_DATA_FLOW_INFO, expression);
                if (info == null) {
                    info = DataFlowInfo.EMPTY;
                }

                List<JetType> variantsForExplicitReceiver = AutoCastUtils.getAutoCastVariants(receiverValue, context, info);

                for (JetType variant : variantsForExplicitReceiver) {
                    descriptors.addAll(includeExternalCallableExtensions(
                            excludePrivateDescriptors(variant.getMemberScope().getAllDescriptors()),
                            resolutionScope, receiverValue));
                }

            }
            return descriptors;
        }
        else {
            return getVariantsNoReceiver(expression, context);
        }
    }

    public static Collection<DeclarationDescriptor> getVariantsNoReceiver(JetExpression expression, BindingContext context) {
        JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);
        if (resolutionScope != null) {
            if (expression.getParent() instanceof JetImportDirective || expression.getParent() instanceof JetPackageDirective) {
                return excludeNonPackageDescriptors(resolutionScope.getAllDescriptors());
            }
            else {
                Collection<DeclarationDescriptor> descriptorsSet = Sets.newHashSet();

                List<ReceiverParameterDescriptor> result = resolutionScope.getImplicitReceiversHierarchy();

                for (ReceiverParameterDescriptor receiverDescriptor : result) {
                    JetType receiverType = receiverDescriptor.getType();
                    descriptorsSet.addAll(receiverType.getMemberScope().getAllDescriptors());
                }

                descriptorsSet.addAll(resolutionScope.getAllDescriptors());
                return excludeNotCallableExtensions(excludePrivateDescriptors(descriptorsSet), resolutionScope);
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    public static Collection<DeclarationDescriptor> getPackageReferenceVariants(
            JetSimpleNameExpression expression,
            BindingContext context
    ) {
        JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);
        if (resolutionScope != null) {
            return excludeNonPackageDescriptors(resolutionScope.getAllDescriptors());
        }

        return Collections.emptyList();
    }

    private static Collection<DeclarationDescriptor> excludePrivateDescriptors(
            @NotNull Collection<DeclarationDescriptor> descriptors
    ) {

        return Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                if (descriptor == null) {
                    return false;
                }

                return true;
            }
        });
    }

    public static Collection<DeclarationDescriptor> excludeNotCallableExtensions(
            @NotNull Collection<? extends DeclarationDescriptor> descriptors, @NotNull JetScope scope
    ) {
        Set<DeclarationDescriptor> descriptorsSet = Sets.newHashSet(descriptors);

        final List<ReceiverParameterDescriptor> result = scope.getImplicitReceiversHierarchy();

        descriptorsSet.removeAll(
                Collections2.filter(JetScopeUtils.getAllExtensions(scope), new Predicate<CallableDescriptor>() {
                    @Override
                    public boolean apply(CallableDescriptor callableDescriptor) {
                        if (callableDescriptor.getReceiverParameter() == null) {
                            return false;
                        }
                        for (ReceiverParameterDescriptor receiverDescriptor : result) {
                            if (ExpressionTypingUtils.checkIsExtensionCallable(receiverDescriptor.getValue(), callableDescriptor)) {
                                return false;
                            }
                        }
                        return true;
                    }
                }));

        return Lists.newArrayList(descriptorsSet);
    }

    private static Collection<DeclarationDescriptor> excludeNonPackageDescriptors(
            @NotNull Collection<DeclarationDescriptor> descriptors
    ) {
        return Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(DeclarationDescriptor declarationDescriptor) {
                if (declarationDescriptor instanceof PackageViewDescriptor) {
                    // Heuristic: we don't want to complete "System" in "package java.lang.Sys",
                    // so we find class of the same name as package, we exclude this package
                    PackageViewDescriptor parent = ((PackageViewDescriptor) declarationDescriptor).getContainingDeclaration();
                    if (parent != null) {
                        JetScope parentScope = parent.getMemberScope();
                        return parentScope.getClassifier(declarationDescriptor.getName()) == null;
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private static Set<DeclarationDescriptor> includeExternalCallableExtensions(
            @NotNull Collection<DeclarationDescriptor> descriptors,
            @NotNull JetScope externalScope,
            @NotNull final ReceiverValue receiverValue
    ) {
        Set<DeclarationDescriptor> descriptorsSet = Sets.newHashSet(descriptors);

        descriptorsSet.addAll(
                Collections2.filter(JetScopeUtils.getAllExtensions(externalScope),
                                    new Predicate<CallableDescriptor>() {
                                        @Override
                                        public boolean apply(CallableDescriptor callableDescriptor) {
                                            return ExpressionTypingUtils.checkIsExtensionCallable(receiverValue, callableDescriptor);
                                        }
                                    }));

        return descriptorsSet;
    }
}
