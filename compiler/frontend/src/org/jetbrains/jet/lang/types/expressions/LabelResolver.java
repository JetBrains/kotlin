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

package org.jetbrains.jet.lang.types.expressions;

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.LabelName;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.LABEL_NAME_CLASH;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.jetbrains.jet.lang.resolve.BindingContext.LABEL_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;

public class LabelResolver {

    private final Map<LabelName, Stack<JetElement>> labeledElements = new HashMap<LabelName, Stack<JetElement>>();

    public LabelResolver() {}

    public void enterLabeledElement(@NotNull LabelName labelName, @NotNull JetExpression labeledExpression) {
        JetExpression deparenthesized = JetPsiUtil.deparenthesizeWithNoTypeResolution(labeledExpression);
        if (deparenthesized != null) {
            Stack<JetElement> stack = labeledElements.get(labelName);
            if (stack == null) {
                stack = new Stack<JetElement>();
                labeledElements.put(labelName, stack);
            }
            stack.push(deparenthesized);
        }
    }

    public void exitLabeledElement(@NotNull JetExpression expression) {
        JetExpression deparenthesized = JetPsiUtil.deparenthesizeWithNoTypeResolution(expression);
        // TODO : really suboptimal
        for (Iterator<Map.Entry<LabelName,Stack<JetElement>>> mapIter = labeledElements.entrySet().iterator(); mapIter.hasNext(); ) {
            Map.Entry<LabelName, Stack<JetElement>> entry = mapIter.next();
            Stack<JetElement> stack = entry.getValue();
            for (Iterator<JetElement> stackIter = stack.iterator(); stackIter.hasNext(); ) {
                JetElement recorded = stackIter.next();
                if (recorded == deparenthesized) {
                    stackIter.remove();
                }
            }
            if (stack.isEmpty()) {
                mapIter.remove();
            }
        }
    }

    @Nullable
    private JetElement resolveControlLabel(@NotNull LabelName labelName, @NotNull JetSimpleNameExpression labelExpression, boolean reportUnresolved, ExpressionTypingContext context) {
        Collection<DeclarationDescriptor> declarationsByLabel = context.scope.getDeclarationsByLabel(labelName);
        int size = declarationsByLabel.size();

        if (size == 1) {
            DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
            JetElement element;
            if (declarationDescriptor instanceof FunctionDescriptor || declarationDescriptor instanceof ClassDescriptor) {
                element = (JetElement) BindingContextUtils.descriptorToDeclaration(context.trace.getBindingContext(), declarationDescriptor);
            }
            else {
                throw new UnsupportedOperationException(declarationDescriptor.getClass().toString()); // TODO
            }
            context.trace.record(LABEL_TARGET, labelExpression, element);
            return element;
        }
        else if (size == 0) {
            return resolveNamedLabel(labelName, labelExpression, reportUnresolved, context);
        }
        BindingContextUtils.reportAmbiguousLabel(context.trace, labelExpression, declarationsByLabel);
        return null;
    }

    @Nullable
    public JetElement resolveLabel(JetLabelQualifiedExpression expression, ExpressionTypingContext context) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();
        if (labelElement != null) {
            LabelName labelName = new LabelName(expression.getLabelName());
            return resolveControlLabel(labelName, labelElement, true, context);
        }
        return null;
    }

    private JetElement resolveNamedLabel(@NotNull LabelName labelName, @NotNull JetSimpleNameExpression labelExpression, boolean reportUnresolved, ExpressionTypingContext context) {
        Stack<JetElement> stack = labeledElements.get(labelName);
        if (stack == null || stack.isEmpty()) {
            if (reportUnresolved) {
                context.trace.report(UNRESOLVED_REFERENCE.on(labelExpression, labelExpression));
            }
            return null;
        }
        else if (stack.size() > 1) {
            context.trace.report(LABEL_NAME_CLASH.on(labelExpression));
        }

        JetElement result = stack.peek();
        context.trace.record(LABEL_TARGET, labelExpression, result);
        return result;
    }

    public LabeledReceiverResolutionResult resolveThisLabel(JetReferenceExpression thisReference, JetSimpleNameExpression targetLabel,
            ExpressionTypingContext context, LabelName labelName) {
        Collection<DeclarationDescriptor> declarationsByLabel = context.scope.getDeclarationsByLabel(labelName);
        int size = declarationsByLabel.size();
        assert targetLabel != null;
        if (size == 1) {
            DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
            ReceiverParameterDescriptor thisReceiver;
            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                thisReceiver = classDescriptor.getThisAsReceiverParameter();
            }
            else if (declarationDescriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) declarationDescriptor;
                thisReceiver = functionDescriptor.getReceiverParameter();
            }
            else if (declarationDescriptor instanceof PropertyDescriptor) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) declarationDescriptor;
                thisReceiver = propertyDescriptor.getReceiverParameter();
            }
            else {
                throw new UnsupportedOperationException("Unsupported descriptor: " + declarationDescriptor); // TODO
            }
            PsiElement element = BindingContextUtils.descriptorToDeclaration(context.trace.getBindingContext(), declarationDescriptor);
            assert element != null : "No PSI element for descriptor: " + declarationDescriptor;
            context.trace.record(LABEL_TARGET, targetLabel, element);
            context.trace.record(REFERENCE_TARGET, thisReference, declarationDescriptor);

            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                if (!DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, targetLabel, classDescriptor)) {
                    return LabeledReceiverResolutionResult.labelResolutionFailed();
                }
            }

            return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver);
        }
        else if (size == 0) {
            JetElement element = resolveNamedLabel(labelName, targetLabel, false, context);
            if (element instanceof JetFunctionLiteralExpression) {
                DeclarationDescriptor declarationDescriptor = context.trace.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
                if (declarationDescriptor instanceof FunctionDescriptor) {
                    ReceiverParameterDescriptor thisReceiver = ((FunctionDescriptor) declarationDescriptor).getReceiverParameter();
                    if (thisReceiver != null) {
                        context.trace.record(LABEL_TARGET, targetLabel, element);
                        context.trace.record(REFERENCE_TARGET, thisReference, declarationDescriptor);
                    }
                    return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver);
                }
                else {
                    context.trace.report(UNRESOLVED_REFERENCE.on(targetLabel, targetLabel));
                }
            }
            else {
                context.trace.report(UNRESOLVED_REFERENCE.on(targetLabel, targetLabel));
            }
        }
        else {
            BindingContextUtils.reportAmbiguousLabel(context.trace, targetLabel, declarationsByLabel);
        }
        return LabeledReceiverResolutionResult.labelResolutionFailed();
    }

    public static final class LabeledReceiverResolutionResult {
        public static LabeledReceiverResolutionResult labelResolutionSuccess(@Nullable ReceiverParameterDescriptor receiverParameterDescriptor) {
            if (receiverParameterDescriptor == null) {
                return new LabeledReceiverResolutionResult(Code.NO_THIS, null);
            }
            return new LabeledReceiverResolutionResult(Code.SUCCESS, receiverParameterDescriptor);
        }

        public static LabeledReceiverResolutionResult labelResolutionFailed() {
            return new LabeledReceiverResolutionResult(Code.LABEL_RESOLUTION_ERROR, null);
        }

        public enum Code {
            LABEL_RESOLUTION_ERROR,
            NO_THIS,
            SUCCESS
        }

        private final Code code;
        private final ReceiverParameterDescriptor receiverParameterDescriptor;

        private LabeledReceiverResolutionResult(
                Code code,
                ReceiverParameterDescriptor receiverParameterDescriptor
        ) {
            this.code = code;
            this.receiverParameterDescriptor = receiverParameterDescriptor;
        }

        public Code getCode() {
            return code;
        }

        public boolean success() {
            return code == Code.SUCCESS;
        }

        public ReceiverParameterDescriptor getReceiverParameterDescriptor() {
            assert success() : "Don't try to obtain the receiver when resolution failed with " + code;
            return receiverParameterDescriptor;
        }
    }
}
