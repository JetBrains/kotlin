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

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.LABEL_NAME_CLASH;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.jetbrains.jet.lang.resolve.BindingContext.LABEL_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;

public class LabelResolver {
    
    public static LabelResolver INSTANCE = new LabelResolver();

    private LabelResolver() {}

    @NotNull
    private List<JetElement> getElementsByLabelName(@NotNull Name labelName, @NotNull JetSimpleNameExpression labelExpression) {
        List<JetElement> elements = Lists.newArrayList();
        PsiElement parent = labelExpression.getParent();
        while (parent != null) {
            Name name = getLabelNameIfAny(parent);
            if (name != null && name.equals(labelName)) {
                elements.add(getExpressionUnderLabel((JetExpression) parent));
            }
            parent = parent.getParent();
        }
        return elements;
    }

    @Nullable
    private Name getLabelNameIfAny(@NotNull PsiElement element) {
        if (element instanceof JetPrefixExpression) {
            JetPrefixExpression prefixExpression = (JetPrefixExpression) element;
            if (JetPsiUtil.isLabeledExpression(prefixExpression)) {
                return Name.identifierForLabel(JetPsiUtil.getLabelName(prefixExpression));
            }
        }
        else if (element instanceof JetFunctionLiteralExpression) {
            return getCallerName((JetFunctionLiteralExpression) element);
        }
        return null;
    }

    @NotNull
    private JetExpression getExpressionUnderLabel(@NotNull JetExpression labeledExpression) {
        JetExpression expression = JetPsiUtil.safeDeparenthesize(labeledExpression, true);
        if (expression instanceof JetFunctionLiteralExpression) {
            return ((JetFunctionLiteralExpression) expression).getFunctionLiteral();
        }
        return expression;
    }

    @Nullable
    private Name getCallerName(@NotNull JetFunctionLiteralExpression expression) {
        JetCallExpression callExpression = getContainingCallExpression(expression);
        if (callExpression == null) return null;

        JetExpression calleeExpression = callExpression.getCalleeExpression();
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) calleeExpression;
            return nameExpression.getReferencedNameAsName();
        }

        return null;
    }

    @Nullable
    private JetCallExpression getContainingCallExpression(JetFunctionLiteralExpression expression) {
        PsiElement parent = expression.getParent();
        if (parent instanceof JetCallExpression) {
            // f {}
            return (JetCallExpression) parent;
        }

        if (parent instanceof JetValueArgument) {
            // f ({}) or f(p = {})
            JetValueArgument argument = (JetValueArgument) parent;
            PsiElement argList = argument.getParent();
            if (argList == null) return null;
            PsiElement call = argList.getParent();
            if (call instanceof JetCallExpression) {
                return (JetCallExpression) call;
            }
        }
        return null;
    }

    @Nullable
    private JetElement resolveControlLabel(@NotNull Name labelName, @NotNull JetSimpleNameExpression labelExpression, ExpressionTypingContext context) {
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
            JetElement element = resolveNamedLabel(labelName, labelExpression, context);
            if (element == null) {
                context.trace.report(UNRESOLVED_REFERENCE.on(labelExpression, labelExpression));
            }
            return element;
        }
        BindingContextUtils.reportAmbiguousLabel(context.trace, labelExpression, declarationsByLabel);
        return null;
    }

    @Nullable
    public JetElement resolveLabel(JetLabelQualifiedExpression expression, ExpressionTypingContext context) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();
        if (labelElement != null) {
            Name labelName = Name.identifierForLabel(expression.getLabelName());
            return resolveControlLabel(labelName, labelElement, context);
        }
        return null;
    }

    private JetElement resolveNamedLabel(@NotNull Name labelName, @NotNull JetSimpleNameExpression labelExpression, ExpressionTypingContext context) {
        List<JetElement> list = getElementsByLabelName(labelName, labelExpression);
        if (list.isEmpty()) return null;

        if (list.size() > 1) {
            context.trace.report(LABEL_NAME_CLASH.on(labelExpression));
        }

        JetElement result = list.get(0);
        context.trace.record(LABEL_TARGET, labelExpression, result);
        return result;
    }

    public LabeledReceiverResolutionResult resolveThisLabel(JetReferenceExpression thisReference, JetSimpleNameExpression targetLabel,
            ExpressionTypingContext context, Name labelName) {
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
            JetElement element = resolveNamedLabel(labelName, targetLabel, context);
            if (element instanceof JetFunctionLiteral) {
                DeclarationDescriptor declarationDescriptor =
                        context.trace.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
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
