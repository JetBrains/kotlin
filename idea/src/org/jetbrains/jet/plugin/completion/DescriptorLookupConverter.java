/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.List;

/**
 * @author Nikolay Krasko
 */
public final class DescriptorLookupConverter {

    private final static JetFunctionInsertHandler EMPTY_FUNCTION_HANDLER = new JetFunctionInsertHandler(
            JetFunctionInsertHandler.CaretPosition.AFTER_BRACKETS);

    private final static JetFunctionInsertHandler PARAMS_FUNCTION_HANDLER = new JetFunctionInsertHandler(
            JetFunctionInsertHandler.CaretPosition.IN_BRACKETS);

    private DescriptorLookupConverter() {}

    @NotNull
    public static LookupElement createLookupElement(@NotNull DeclarationDescriptor descriptor, @Nullable PsiElement declaration) {

        LookupElementBuilder element = LookupElementBuilder.create(new JetLookupObject(descriptor, declaration), descriptor.getName());
        String typeText = "";
        String tailText = "";
        boolean tailTextGrayed = false;

        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            JetType returnType = functionDescriptor.getReturnType();
            typeText = DescriptorRenderer.TEXT.renderType(returnType);

            tailText = "(" + StringUtil.join(functionDescriptor.getValueParameters(), new Function<ValueParameterDescriptor, String>() {
                @Override
                public String fun(ValueParameterDescriptor valueParameterDescriptor) {
                    return valueParameterDescriptor.getName() + ":" +
                           DescriptorRenderer.TEXT.renderType(valueParameterDescriptor.getOutType());
                }
            }, ",") + ")";


            // TODO: A special case when it's impossible to resolve type parameters from arguments. Need '<' caret '>'
            // TODO: Support omitting brackets for one argument functions
            if (functionDescriptor.getValueParameters().isEmpty()) {
                element = element.setInsertHandler(EMPTY_FUNCTION_HANDLER);
            } else {
                element = element.setInsertHandler(PARAMS_FUNCTION_HANDLER);
            }
        }
        else if (descriptor instanceof VariableDescriptor) {
            JetType outType = ((VariableDescriptor) descriptor).getOutType();
            typeText = DescriptorRenderer.TEXT.renderType(outType);
        }
        else if (descriptor instanceof ClassDescriptor) {
            tailText = " (" + DescriptorUtils.getFQName(descriptor.getContainingDeclaration()) + ")";
            tailTextGrayed = true;
        }
        else {
            typeText = DescriptorRenderer.TEXT.render(descriptor);
        }

        element = element.setTailText(tailText, tailTextGrayed).setTypeText(typeText);

        if (declaration != null) {
            element = element.setIcon(declaration.getIcon(Iconable.ICON_FLAG_OPEN | Iconable.ICON_FLAG_VISIBILITY));
        }

        return element;
    }

    @NotNull
    public static LookupElement createLookupElement(@NotNull BindingContext bindingContext, @NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) descriptor;
            while (callableMemberDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                // TODO: need to know all of them
                callableMemberDescriptor = callableMemberDescriptor.getOverriddenDescriptors().iterator().next();
            }
            descriptor = callableMemberDescriptor;
        }
        return createLookupElement(descriptor, bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor));
    }

    public static LookupElement[] collectLookupElements(BindingContext bindingContext, Iterable<DeclarationDescriptor> descriptors) {
        List<LookupElement> result = Lists.newArrayList();

        for (final DeclarationDescriptor descriptor : descriptors) {
            result.add(createLookupElement(bindingContext, descriptor));
        }

        return result.toArray(new LookupElement[result.size()]);
    }
}
