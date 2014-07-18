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

package org.jetbrains.jet.plugin.completion;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetDescriptorIconProvider;
import org.jetbrains.jet.plugin.completion.handlers.CaretPosition;
import org.jetbrains.jet.plugin.completion.handlers.GenerateLambdaInfo;
import org.jetbrains.jet.plugin.completion.handlers.JetClassInsertHandler;
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.List;

public final class DescriptorLookupConverter {
    private DescriptorLookupConverter() {}

    @NotNull
    public static LookupElement createLookupElement(@NotNull KotlinCodeAnalyzer analyzer,
            @NotNull DeclarationDescriptor descriptor, @Nullable PsiElement declaration) {
        LookupElementBuilder element = LookupElementBuilder.create(
                new JetLookupObject(descriptor, analyzer, declaration), descriptor.getName().asString());

        String presentableText = descriptor.getName().asString();
        String typeText = "";
        String tailText = "";

        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            JetType returnType = functionDescriptor.getReturnType();
            typeText = returnType != null ? DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(returnType) : "";
            presentableText += DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderFunctionParameters(functionDescriptor);

            boolean extensionFunction = functionDescriptor.getReceiverParameter() != null;
            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            if (containingDeclaration != null && extensionFunction) {
                tailText += " for " + DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(functionDescriptor.getReceiverParameter().getType());
                tailText += " in " + DescriptorUtils.getFqName(containingDeclaration);
            }
        }
        else if (descriptor instanceof VariableDescriptor) {
            JetType outType = ((VariableDescriptor) descriptor).getType();
            typeText = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(outType);
        }
        else if (descriptor instanceof ClassDescriptor) {
            DeclarationDescriptor declaredIn = descriptor.getContainingDeclaration();
            assert declaredIn != null;
            tailText = " (" + DescriptorUtils.getFqName(declaredIn) + ")";
        }
        else {
            typeText = DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor);
        }

        InsertHandler<LookupElement> insertHandler = getDefaultInsertHandler(descriptor);
        element = element.withInsertHandler(insertHandler);
        if (insertHandler instanceof JetFunctionInsertHandler && ((JetFunctionInsertHandler)insertHandler).getLambdaInfo() != null) {
            element.putUserData(JetCompletionCharFilter.ACCEPT_OPENING_BRACE, true);
        }
        element = element.withTailText(tailText, true).withTypeText(typeText).withPresentableText(presentableText);
        element = element.withIcon(JetDescriptorIconProvider.getIcon(descriptor, declaration, Iconable.ICON_FLAG_VISIBILITY));
        element = element.withStrikeoutness(KotlinBuiltIns.getInstance().isDeprecated(descriptor));

        return element;
    }

    @Nullable
    public static InsertHandler<LookupElement> getDefaultInsertHandler(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();

            if (parameters.isEmpty()) {
                return JetFunctionInsertHandler.NO_PARAMETERS_HANDLER;
            }

            if (parameters.size() == 1) {
                JetType parameterType = parameters.get(0).getType();
                if (KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(parameterType)) {
                    int parameterCount = KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(parameterType).size();
                    if (parameterCount <= 1) { // otherwise additional item with lambda template is to be added
                        return new JetFunctionInsertHandler(CaretPosition.IN_BRACKETS, new GenerateLambdaInfo(parameterType, false));
                    }
                }
            }

            return JetFunctionInsertHandler.WITH_PARAMETERS_HANDLER;
        }

        if (descriptor instanceof ClassDescriptor) {
            return JetClassInsertHandler.INSTANCE$;
        }

        return null;
    }

    @NotNull
    public static LookupElement createLookupElement(@NotNull KotlinCodeAnalyzer analyzer, @NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            descriptor = DescriptorUtils.unwrapFakeOverride((CallableMemberDescriptor) descriptor);
        }
        return createLookupElement(analyzer, descriptor, DescriptorToSourceUtils.descriptorToDeclaration(descriptor));
    }

    public static LookupElement[] collectLookupElements(
            @NotNull KotlinCodeAnalyzer analyzer,
            @NotNull Iterable<DeclarationDescriptor> descriptors
    ) {
        List<LookupElement> result = Lists.newArrayList();

        for (DeclarationDescriptor descriptor : descriptors) {
            result.add(createLookupElement(analyzer, descriptor));
        }

        return result.toArray(new LookupElement[result.size()]);
    }
}
