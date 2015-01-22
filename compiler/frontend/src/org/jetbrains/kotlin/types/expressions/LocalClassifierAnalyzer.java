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

package org.jetbrains.kotlin.types.expressions;

import com.google.common.base.Predicates;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.kotlin.descriptors.impl.PackageLikeBuilder;
import org.jetbrains.kotlin.di.InjectorForTopDownAnalyzerBasic;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.TopDownAnalysisContext;
import org.jetbrains.kotlin.resolve.TopDownAnalysisParameters;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.types.DynamicTypesSettings;

import java.util.Collections;

public class LocalClassifierAnalyzer {
    public void processClassOrObject(
            @NotNull GlobalContext globalContext,
            @Nullable final WritableScope scope,
            @NotNull ExpressionTypingContext context,
            @NotNull final DeclarationDescriptor containingDeclaration,
            @NotNull JetClassOrObject object,
            @NotNull AdditionalCheckerProvider additionalCheckerProvider,
            @NotNull DynamicTypesSettings dynamicTypesSettings
    ) {
        TopDownAnalysisParameters topDownAnalysisParameters =
                TopDownAnalysisParameters.createForLocalDeclarations(
                        globalContext.getStorageManager(),
                        globalContext.getExceptionTracker(),
                        Predicates.equalTo(object.getContainingFile())
                );

        InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(
                object.getProject(),
                topDownAnalysisParameters,
                context.trace,
                DescriptorUtils.getContainingModule(containingDeclaration),
                additionalCheckerProvider,
                dynamicTypesSettings
        );

        TopDownAnalysisContext c = new TopDownAnalysisContext(topDownAnalysisParameters);
        c.setOuterDataFlowInfo(context.dataFlowInfo);

        injector.getTopDownAnalyzer().doProcess(
               c,
               context.scope,
               new PackageLikeBuilder() {

                   @NotNull
                   @Override
                   public DeclarationDescriptor getOwnerForChildren() {
                       return containingDeclaration;
                   }

                   @Override
                   public void addClassifierDescriptor(@NotNull MutableClassDescriptor classDescriptor) {
                       if (scope != null) {
                           scope.addClassifierDescriptor(classDescriptor);
                       }
                   }

                   @Override
                   public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
                       throw new UnsupportedOperationException();
                   }

                   @Override
                   public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {

                   }

                   @Override
                   public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
                       return null;
                   }
               },
               Collections.<PsiElement>singletonList(object)
        );
    }
}
