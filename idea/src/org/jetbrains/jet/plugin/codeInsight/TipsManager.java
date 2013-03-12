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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastServiceImpl;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;

import java.util.*;

public final class TipsManager {

    private TipsManager() {
    }

    @NotNull
    public static Collection<DeclarationDescriptor> getReferenceVariants(
            @NotNull final JetSimpleNameExpression expression,
            @NotNull final BindingContext context
    ) {
        JetExpression receiverExpression = expression.getReceiverExpression();
        if (receiverExpression != null) {
            // Process as call expression
            JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);
            JetType expressionType = context.get(BindingContext.EXPRESSION_TYPE, receiverExpression);

            if (expressionType != null && resolutionScope != null && !ErrorUtils.isErrorType(expressionType)) {
                if (!(expressionType instanceof NamespaceType)) {
                    ExpressionReceiver receiverValue = new ExpressionReceiver(receiverExpression, expressionType);
                    Set<DeclarationDescriptor> descriptors = new HashSet<DeclarationDescriptor>();

                    DataFlowInfo info = context.get(BindingContext.NON_DEFAULT_EXPRESSION_DATA_FLOW, expression);
                    if (info == null) {
                        info = DataFlowInfo.EMPTY;
                    }

                    AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(info, context);
                    List<ReceiverValue> variantsForExplicitReceiver = autoCastService.getVariantsForReceiver(receiverValue);

                    for (ReceiverValue descriptor : variantsForExplicitReceiver) {
                        descriptors.addAll(includeExternalCallableExtensions(
                                excludePrivateDescriptors(descriptor.getType().getMemberScope().getAllDescriptors()),
                                resolutionScope, descriptor));
                    }

                    return descriptors;
                }

                return includeExternalCallableExtensions(
                        excludePrivateDescriptors(expressionType.getMemberScope().getAllDescriptors()),
                        resolutionScope, new ExpressionReceiver(receiverExpression, expressionType));
            }
            return Collections.emptyList();
        }
        else {
            return getVariantsNoReceiver(expression, context);
        }
    }

    public static Collection<DeclarationDescriptor> getVariantsNoReceiver(JetExpression expression, BindingContext context) {
        JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);
        if (resolutionScope != null) {
            if (expression.getParent() instanceof JetImportDirective || expression.getParent() instanceof JetNamespaceHeader) {
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
            @NotNull Collection<? extends DeclarationDescriptor> descriptors, @NotNull final JetScope scope
    ) {
        final Set<DeclarationDescriptor> descriptorsSet = Sets.newHashSet(descriptors);

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
                if (declarationDescriptor instanceof NamespaceDescriptor) {
                    // Heuristic: we don't want to complete "System" in "package java.lang.Sys",
                    // so we find class of the same name as namespace, we exclude this namespace
                    DeclarationDescriptor parent = declarationDescriptor.getContainingDeclaration();
                    if (parent instanceof NamespaceDescriptor) {
                        JetScope parentScope = ((NamespaceDescriptor) parent).getMemberScope();
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
            @NotNull final JetScope externalScope,
            @NotNull final ReceiverValue receiverValue
    ) {
        // It's impossible to add extension function for namespace
        JetType receiverType = receiverValue.getType();
        if (receiverType instanceof NamespaceType) {
            return new HashSet<DeclarationDescriptor>(descriptors);
        }

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
