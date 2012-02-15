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

package org.jetbrains.jet.lang.types.expressions;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
* @author abreslav
*/
public class LabelResolver {

    private final Map<String, Stack<JetElement>> labeledElements = new HashMap<String, Stack<JetElement>>();

    public LabelResolver() {}

    public void enterLabeledElement(@NotNull String labelName, @NotNull JetExpression labeledExpression) {
        JetExpression deparenthesized = JetPsiUtil.deparenthesize(labeledExpression);
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
        JetExpression deparenthesized = JetPsiUtil.deparenthesize(expression);
        // TODO : really suboptimal
        for (Iterator<Map.Entry<String, Stack<JetElement>>> mapIter = labeledElements.entrySet().iterator(); mapIter.hasNext(); ) {
            Map.Entry<String, Stack<JetElement>> entry = mapIter.next();
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
    private JetElement resolveControlLabel(@NotNull String labelName, @NotNull JetSimpleNameExpression labelExpression, boolean reportUnresolved, ExpressionTypingContext context) {
        Collection<DeclarationDescriptor> declarationsByLabel = context.scope.getDeclarationsByLabel(labelName);
        int size = declarationsByLabel.size();

        if (size == 1) {
            DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
            JetElement element;
            if (declarationDescriptor instanceof FunctionDescriptor || declarationDescriptor instanceof ClassDescriptor) {
                element = (JetElement) context.trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declarationDescriptor);
            }
            else {
                throw new UnsupportedOperationException(); // TODO
            }
            context.trace.record(LABEL_TARGET, labelExpression, element);
            return element;
        }
        else if (size == 0) {
            return resolveNamedLabel(labelName, labelExpression, reportUnresolved, context);
        }
        context.trace.report(AMBIGUOUS_LABEL.on(labelExpression));
        return null;
    }

    @Nullable
    public JetElement resolveLabel(JetLabelQualifiedExpression expression, ExpressionTypingContext context) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();
        if (labelElement != null) {
            String labelName = expression.getLabelName();
            assert labelName != null;
            return resolveControlLabel(labelName, labelElement, true, context);
        }
        return null;
    }

    private JetElement resolveNamedLabel(@NotNull String labelName, @NotNull JetSimpleNameExpression labelExpression, boolean reportUnresolved, ExpressionTypingContext context) {
        Stack<JetElement> stack = labeledElements.get(labelName);
        if (stack == null || stack.isEmpty()) {
            if (reportUnresolved) {
                context.trace.report(UNRESOLVED_REFERENCE.on(labelExpression));
            }
            return null;
        }
        else if (stack.size() > 1) {
            context.trace.report(LABEL_NAME_CLASH.on(labelExpression));
        }

        JetElement result = stack.peek();
        context.trace.record(BindingContext.LABEL_TARGET, labelExpression, result);
        return result;
    }

    public ReceiverDescriptor resolveThisLabel(JetReferenceExpression thisReference, JetSimpleNameExpression targetLabel, ExpressionTypingContext context, ReceiverDescriptor thisReceiver, String labelName) {
        Collection<DeclarationDescriptor> declarationsByLabel = context.scope.getDeclarationsByLabel(labelName);
        int size = declarationsByLabel.size();
        assert targetLabel != null;
        if (size == 1) {
            DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                thisReceiver = classDescriptor.getImplicitReceiver();
            }
            else if (declarationDescriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) declarationDescriptor;
                thisReceiver = functionDescriptor.getReceiverParameter();
            }
            else {
                throw new UnsupportedOperationException(); // TODO
            }
            PsiElement element = context.trace.get(DESCRIPTOR_TO_DECLARATION, declarationDescriptor);
            assert element != null;
            context.trace.record(LABEL_TARGET, targetLabel, element);
            context.trace.record(REFERENCE_TARGET, thisReference, declarationDescriptor);
        }
        else if (size == 0) {
            JetElement element = resolveNamedLabel(labelName, targetLabel, false, context);
            if (element instanceof JetFunctionLiteralExpression) {
                DeclarationDescriptor declarationDescriptor = context.trace.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
                if (declarationDescriptor instanceof FunctionDescriptor) {
                    thisReceiver = ((FunctionDescriptor) declarationDescriptor).getReceiverParameter();
                    if (thisReceiver.exists()) {
                        context.trace.record(LABEL_TARGET, targetLabel, element);
                        context.trace.record(REFERENCE_TARGET, thisReference, declarationDescriptor);
                    }
                }
                else {
                    context.trace.report(UNRESOLVED_REFERENCE.on(targetLabel));
                }
            }
            else {
                context.trace.report(UNRESOLVED_REFERENCE.on(targetLabel));
            }
        }
        else {
            context.trace.report(AMBIGUOUS_LABEL.on(targetLabel));
        }
        return thisReceiver;
    }
}
