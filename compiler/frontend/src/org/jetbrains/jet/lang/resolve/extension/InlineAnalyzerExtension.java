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

package org.jetbrains.jet.lang.resolve.extension;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FunctionAnalyzerExtension;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.InlineUtil;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.List;

public class InlineAnalyzerExtension implements FunctionAnalyzerExtension.AnalyzerExtension {

    public static final InlineAnalyzerExtension INSTANCE = new InlineAnalyzerExtension();

    private InlineAnalyzerExtension() {

    }

    @Override
    public void process(
            @NotNull final FunctionDescriptor descriptor, @NotNull JetNamedFunction function, @NotNull final BindingTrace trace
    ) {
        assert descriptor instanceof SimpleFunctionDescriptor && ((SimpleFunctionDescriptor) descriptor).getInlineStrategy().isInline() :
                "This method should be invoced on inline function: " + descriptor;

        checkDefaults(descriptor, function, trace);
        checkNotVirtual(descriptor, function, trace);
        checkHasInlinableAndNullability(descriptor, function, trace);

        JetVisitorVoid visitor = new JetVisitorVoid() {

            @Override
            public void visitJetElement(@NotNull JetElement element) {
                super.visitJetElement(element);
                element.acceptChildren(this);
            }

            @Override
            public void visitClass(@NotNull JetClass klass) {
                trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(klass, klass, descriptor));
            }

            @Override
            public void visitNamedFunction(@NotNull JetNamedFunction function) {
                if (function.getParent().getParent() instanceof JetObjectDeclaration) {
                    super.visitNamedFunction(function);
                } else {
                    trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(function, function, descriptor));
                }
            }
        };

        function.acceptChildren(visitor);
    }

    private static void checkDefaults(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JetFunction function,
            @NotNull BindingTrace trace
    ) {
        int index = 0;
        List<JetParameter> jetParameters = function.getValueParameters();
        for (ValueParameterDescriptor parameter : functionDescriptor.getValueParameters()) {
            if (parameter.hasDefaultValue()) {
                JetParameter jetParameter = jetParameters.get(index);
                //report not supported default only on inlinable lambda and on parameter with inherited dafault (there is some problems to inline it)
                if (checkInlinableParameter(parameter, jetParameter, functionDescriptor, null) || !parameter.declaresDefaultValue()) {
                    trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(jetParameter, jetParameter, functionDescriptor));
                }
            }
            index++;
        }
    }

    private static void checkNotVirtual(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JetFunction function,
            @NotNull BindingTrace trace
    ) {
        if (Visibilities.isPrivate(functionDescriptor.getVisibility()) || functionDescriptor.getModality() == Modality.FINAL) {
            return;
        }

        if (functionDescriptor.getContainingDeclaration() instanceof PackageFragmentDescriptor) {
            return;
        }

        trace.report(Errors.DECLARATION_CANT_BE_INLINED.on(function));
    }


    private static void checkHasInlinableAndNullability(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JetFunction function,
            @NotNull BindingTrace trace
    ) {
        boolean hasInlinable = false;
        List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
        int index = 0;
        for (ValueParameterDescriptor parameter : parameters) {
            hasInlinable |= checkInlinableParameter(parameter, function.getValueParameters().get(index++), functionDescriptor, trace);
        }
        ReceiverParameterDescriptor receiverParameter = functionDescriptor.getExtensionReceiverParameter();
        if (receiverParameter != null) {
            JetTypeReference receiver = function.getReceiverTypeReference();
            assert receiver != null : "Descriptor has a receiver but psi doesn't " + function.getText();
            hasInlinable |= checkInlinableParameter(receiverParameter, receiver, functionDescriptor, trace);
        }

        hasInlinable |= DescriptorUtils.containsReifiedTypeParameters(functionDescriptor);

        if (!hasInlinable) {
            trace.report(Errors.NOTHING_TO_INLINE.on(function, functionDescriptor));
        }
    }

    public static boolean checkInlinableParameter(
            @NotNull CallableDescriptor parameter,
            @NotNull JetElement expression,
            @NotNull CallableDescriptor functionDescriptor,
            @Nullable BindingTrace trace
    ) {
        JetType type = parameter.getReturnType();
        if (type != null && KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type)) {
            if (!InlineUtil.hasNoinlineAnnotation(parameter)) {
                if (type.isMarkedNullable()) {
                    if (trace != null) {
                        trace.report(Errors.NULLABLE_INLINE_PARAMETER.on(expression, expression, functionDescriptor));
                    }
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }
}
