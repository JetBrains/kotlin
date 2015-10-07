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

import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.scopes.utils.UtilsPackage;

import java.util.Collection;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.LABEL_NAME_CLASH;
import static org.jetbrains.kotlin.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.jetbrains.kotlin.resolve.BindingContext.LABEL_TARGET;
import static org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET;

public class LabelResolver {
    
    public static LabelResolver INSTANCE = new LabelResolver();

    private LabelResolver() {}

    @NotNull
    private Set<JetElement> getElementsByLabelName(@NotNull Name labelName, @NotNull JetSimpleNameExpression labelExpression) {
        Set<JetElement> elements = Sets.newLinkedHashSet();
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
    public Name getLabelNameIfAny(@NotNull PsiElement element) {
        if (element instanceof JetLabeledExpression) {
            return ((JetLabeledExpression) element).getLabelNameAsName();
        }

        if (element instanceof JetFunctionLiteral) {
            return getLabelNameIfAny(element.getParent());
        }

        if (element instanceof JetFunctionLiteralExpression) {
            return getLabelForFunctionalExpression((JetExpression) element);
        }

        if (element instanceof JetNamedFunction) {
            Name name = ((JetNamedFunction) element).getNameAsName();
            if (name != null) return name;
            return getLabelForFunctionalExpression((JetExpression) element);
        }

        return null;
    }

    private Name getLabelForFunctionalExpression(@NotNull JetExpression element) {
        if (element.getParent() instanceof JetLabeledExpression) {
            return getLabelNameIfAny(element.getParent());
        }
        return getCallerName(element);
    }

    @NotNull
    private JetExpression getExpressionUnderLabel(@NotNull JetExpression labeledExpression) {
        JetExpression expression = JetPsiUtil.safeDeparenthesize(labeledExpression);
        if (expression instanceof JetFunctionLiteralExpression) {
            return ((JetFunctionLiteralExpression) expression).getFunctionLiteral();
        }
        return expression;
    }

    @Nullable
    private Name getCallerName(@NotNull JetExpression expression) {
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
    private JetCallExpression getContainingCallExpression(@NotNull JetExpression expression) {
        PsiElement parent = expression.getParent();
        if (parent instanceof JetFunctionLiteralArgument) {
            // f {}
            PsiElement call = parent.getParent();
            if (call instanceof JetCallExpression) {
                return (JetCallExpression) call;
            }
        }

        if (parent instanceof JetValueArgument) {
            // f ({}) or f(p = {}) or f (fun () {})
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
    public JetElement resolveControlLabel(
            @NotNull JetExpressionWithLabel expression,
            @NotNull ResolutionContext context
    ) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();
        Name labelName = expression.getLabelNameAsName();
        if (labelElement == null || labelName == null) return null;

        Collection<DeclarationDescriptor> declarationsByLabel = UtilsPackage.getDeclarationsByLabel(context.scope, labelName);
        int size = declarationsByLabel.size();

        if (size > 1) {
            BindingContextUtils.reportAmbiguousLabel(context.trace, labelElement, declarationsByLabel);
            return null;
        }
        if (size == 0) {
            JetElement element = resolveNamedLabel(labelName, labelElement, context.trace);
            if (element == null) {
                context.trace.report(UNRESOLVED_REFERENCE.on(labelElement, labelElement));
            }
            return element;
        }
        DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
        JetElement element;
        if (declarationDescriptor instanceof FunctionDescriptor || declarationDescriptor instanceof ClassDescriptor) {
            element = (JetElement) DescriptorToSourceUtils.descriptorToDeclaration(declarationDescriptor);
        }
        else {
            throw new UnsupportedOperationException(declarationDescriptor.getClass().toString()); // TODO
        }
        context.trace.record(LABEL_TARGET, labelElement, element);
        return element;
    }

    private JetElement resolveNamedLabel(
            @NotNull Name labelName, 
            @NotNull JetSimpleNameExpression labelExpression, 
            @NotNull BindingTrace trace
    ) {
        Set<JetElement> list = getElementsByLabelName(labelName, labelExpression);
        if (list.isEmpty()) return null;

        if (list.size() > 1) {
            trace.report(LABEL_NAME_CLASH.on(labelExpression));
        }

        JetElement result = list.iterator().next();
        trace.record(LABEL_TARGET, labelExpression, result);
        return result;
    }
    
    @NotNull
    public LabeledReceiverResolutionResult resolveThisOrSuperLabel(
            @NotNull JetInstanceExpressionWithLabel expression,
            @NotNull ResolutionContext context,
            @NotNull Name labelName
    ) {
        JetReferenceExpression referenceExpression = expression.getInstanceReference();
        JetSimpleNameExpression targetLabel = expression.getTargetLabel();
        assert targetLabel != null : expression;

        Collection<DeclarationDescriptor> declarationsByLabel = UtilsPackage.getDeclarationsByLabel(context.scope, labelName);
        int size = declarationsByLabel.size();
        if (size == 1) {
            DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
            ReceiverParameterDescriptor thisReceiver;
            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                thisReceiver = classDescriptor.getThisAsReceiverParameter();
            }
            else if (declarationDescriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) declarationDescriptor;
                thisReceiver = functionDescriptor.getExtensionReceiverParameter();
            }
            else if (declarationDescriptor instanceof PropertyDescriptor) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) declarationDescriptor;
                thisReceiver = propertyDescriptor.getExtensionReceiverParameter();
            }
            else {
                throw new UnsupportedOperationException("Unsupported descriptor: " + declarationDescriptor); // TODO
            }
            PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(declarationDescriptor);
            assert element != null : "No PSI element for descriptor: " + declarationDescriptor;
            context.trace.record(LABEL_TARGET, targetLabel, element);
            context.trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor);

            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                if (!DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, targetLabel, classDescriptor)) {
                    return LabeledReceiverResolutionResult.labelResolutionFailed();
                }
            }

            return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver);
        }
        else if (size == 0) {
            JetElement element = resolveNamedLabel(labelName, targetLabel, context.trace);
            if (element instanceof JetFunctionLiteral) {
                DeclarationDescriptor declarationDescriptor =
                        context.trace.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
                if (declarationDescriptor instanceof FunctionDescriptor) {
                    ReceiverParameterDescriptor thisReceiver = ((FunctionDescriptor) declarationDescriptor).getExtensionReceiverParameter();
                    if (thisReceiver != null) {
                        context.trace.record(LABEL_TARGET, targetLabel, element);
                        context.trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor);
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
