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

package org.jetbrains.kotlin.resolve.inline;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.FunctionAnalyzerExtension;
import org.jetbrains.kotlin.resolve.annotations.AnnotationUtilKt;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;

import java.util.List;

public class InlineAnalyzerExtension implements FunctionAnalyzerExtension.AnalyzerExtension {

    public static final InlineAnalyzerExtension INSTANCE = new InlineAnalyzerExtension();

    private InlineAnalyzerExtension() {
    }

    @Override
    public void process(
            @NotNull final FunctionDescriptor descriptor, @NotNull KtNamedFunction function, @NotNull final BindingTrace trace
    ) {
        assert InlineUtil.isInline(descriptor) : "This method should be invoked on inline function: " + descriptor;

        checkDefaults(descriptor, function, trace);
        checkNotVirtual(descriptor, function, trace);
        checkHasInlinableAndNullability(descriptor, function, trace);

        KtVisitorVoid visitor = new KtVisitorVoid() {
            @Override
            public void visitKtElement(@NotNull KtElement element) {
                super.visitKtElement(element);
                element.acceptChildren(this);
            }

            @Override
            public void visitClass(@NotNull KtClass klass) {
                trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(klass, klass, descriptor));
            }

            @Override
            public void visitNamedFunction(@NotNull KtNamedFunction function) {
                if (function.getParent().getParent() instanceof KtObjectDeclaration) {
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
            @NotNull KtFunction function,
            @NotNull BindingTrace trace
    ) {
        List<KtParameter> jetParameters = function.getValueParameters();
        for (ValueParameterDescriptor parameter : functionDescriptor.getValueParameters()) {
            if (DescriptorUtilsKt.hasDefaultValue(parameter)) {
                KtParameter jetParameter = jetParameters.get(parameter.getIndex());
                //report not supported default only on inlinable lambda and on parameter with inherited default (there is some problems to inline it)
                if (checkInlinableParameter(parameter, jetParameter, functionDescriptor, null) || !parameter.declaresDefaultValue()) {
                    trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(jetParameter, jetParameter, functionDescriptor));
                }
            }
        }
    }

    private static void checkNotVirtual(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull KtFunction function,
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
            @NotNull KtFunction function,
            @NotNull BindingTrace trace
    ) {
        boolean hasInlinable = false;
        List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
        int index = 0;
        for (ValueParameterDescriptor parameter : parameters) {
            hasInlinable |= checkInlinableParameter(parameter, function.getValueParameters().get(index++), functionDescriptor, trace);
        }

        hasInlinable |= InlineUtil.containsReifiedTypeParameters(functionDescriptor);

        if (!hasInlinable && !AnnotationUtilKt.isInlineOnlyOrReified(functionDescriptor)) {
            KtModifierList modifierList = function.getModifierList();
            PsiElement inlineModifier = modifierList == null ? null : modifierList.getModifier(KtTokens.INLINE_KEYWORD);
            PsiElement reportOn = inlineModifier == null ? function : inlineModifier;
            trace.report(Errors.NOTHING_TO_INLINE.on(reportOn, functionDescriptor));
        }
    }

    public static boolean checkInlinableParameter(
            @NotNull ParameterDescriptor parameter,
            @NotNull KtElement expression,
            @NotNull CallableDescriptor functionDescriptor,
            @Nullable BindingTrace trace
    ) {
        if (InlineUtil.isInlineLambdaParameter(parameter)) {
            if (parameter.getType().isMarkedNullable()) {
                if (trace != null) {
                    trace.report(Errors.NULLABLE_INLINE_PARAMETER.on(expression, expression, functionDescriptor));
                }
            }
            else {
                return true;
            }
        }
        return false;
    }
}
