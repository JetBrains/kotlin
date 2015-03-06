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

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.types.TypeUtils;

import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME;

public class DeprecatedAnnotationVisitor extends AfterAnalysisHighlightingVisitor {

    private static final TokenSet PROPERTY_SET_OPERATIONS =
            TokenSet.create(JetTokens.EQ, JetTokens.PLUSEQ, JetTokens.MINUSEQ, JetTokens.MULTEQ,
                            JetTokens.DIVEQ, JetTokens.PERCEQ, JetTokens.PLUSPLUS, JetTokens.MINUSMINUS);
    private static final FqName JAVA_DEPRECATED = new FqName(Deprecated.class.getName());
    private static final FqName KOTLIN_DEPRECATED = DescriptorUtils.getFqNameSafe(KotlinBuiltIns.getInstance().getDeprecatedAnnotation());

    protected DeprecatedAnnotationVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitSuperExpression(@NotNull JetSuperExpression expression) {
        // Deprecated for super expression. Unnecessary to mark it as Deprecated
    }

    @Override
    public void visitReferenceExpression(@NotNull JetReferenceExpression expression) {
        super.visitReferenceExpression(expression);

        if (expression.getParent() instanceof JetThisExpression) {
            return;
        }

        ResolvedCall resolvedCall = CallUtilPackage.getResolvedCall(expression, bindingContext);
        if (resolvedCall != null && resolvedCall instanceof VariableAsFunctionResolvedCall) {
            // Deprecated for invoke()
            JetCallExpression parent = PsiTreeUtil.getParentOfType(expression, JetCallExpression.class);
            if (parent != null) {
                reportAnnotationIfNeeded(parent, resolvedCall.getResultingDescriptor(), true);
            }
        }
        if (expression.getNode().getElementType() == JetNodeTypes.OPERATION_REFERENCE) {
            // Deprecated for operations (mark as warning)
            checkDeprecatedForOperations(expression);
        }
        else {
            checkDeprecatedForReferenceExpression(expression);
        }
    }

    private void checkDeprecatedForReferenceExpression(@NotNull JetReferenceExpression expression) {
        DeclarationDescriptor target = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (target != null) {
            if (target instanceof ConstructorDescriptor) {
                checkConstructorDescriptor(expression, target);
            }
            else if (target instanceof ClassDescriptor) {
                checkClassDescriptor(expression, (ClassDescriptor) target);
            }
            else if (target instanceof PropertyDescriptor) {
                checkPropertyDescriptor(expression, (PropertyDescriptor) target);
            }
            else if (target instanceof FunctionDescriptor) {
                checkFunctionDescriptor(expression, target);
            }
        }
    }

    private void checkFunctionDescriptor(JetExpression expression, DeclarationDescriptor target) {
        // Deprecated for Function
        reportAnnotationIfNeeded(expression, target, expression instanceof JetArrayAccessExpression);
    }

    private void checkConstructorDescriptor(@NotNull JetExpression expression, @NotNull DeclarationDescriptor target) {
        // Deprecated for Class and for Constructor
        DeclarationDescriptor containingDeclaration = target.getContainingDeclaration();
        if (containingDeclaration != null) {
            if (!reportAnnotationIfNeeded(expression, containingDeclaration)) {
                reportAnnotationIfNeeded(expression, target);
            }
        }
    }

    private void checkClassDescriptor(@NotNull JetExpression expression, @NotNull ClassDescriptor target) {
        reportAnnotationIfNeeded(expression, target);
    }

    private void checkPropertyDescriptor(
            @NotNull JetExpression expression,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        // Deprecated for Property
        if (reportAnnotationIfNeeded(expression, propertyDescriptor, propertyDescriptor.isVar())) {
            return;
        }

        // Deprecated for Getter (val, var), Setter (var)
        if (!propertyDescriptor.isVar()) {
            checkPropertyGetter(propertyDescriptor, expression);
        }
        else {
            IElementType operation = null;
            JetBinaryExpression binaryExpression = PsiTreeUtil.getParentOfType(expression, JetBinaryExpression.class);
            if (binaryExpression != null) {
                JetExpression left = binaryExpression.getLeft();
                if (left == expression) {
                    operation = binaryExpression.getOperationToken();
                }
                else {
                    JetReferenceExpression[] jetReferenceExpressions = PsiTreeUtil.getChildrenOfType(left, JetReferenceExpression.class);
                    if (jetReferenceExpressions != null) {
                        for (JetReferenceExpression expr : jetReferenceExpressions) {
                            if (expr == expression) {
                                operation = binaryExpression.getOperationToken();
                                break;
                            }
                        }
                    }
                }
            }
            else {
                JetUnaryExpression unaryExpression = PsiTreeUtil.getParentOfType(expression, JetUnaryExpression.class);
                if (unaryExpression != null) {
                    operation = unaryExpression.getOperationReference().getReferencedNameElementType();
                }
            }

            if (operation != null && PROPERTY_SET_OPERATIONS.contains(operation)) {
                checkPropertySetter(propertyDescriptor, expression);
            }
            else {
                checkPropertyGetter(propertyDescriptor, expression);
            }
        }
    }

    private void checkPropertySetter(@NotNull PropertyDescriptor descriptor, @NotNull JetExpression expression) {
        PropertySetterDescriptor setter = descriptor.getSetter();
        if (setter != null) {
            checkPropertyAccessor(setter, expression, true);
        }
    }

    private void checkPropertyGetter(@NotNull PropertyDescriptor descriptor, @NotNull JetExpression expression) {
        PropertyGetterDescriptor getter = descriptor.getGetter();
        if (getter != null) {
            checkPropertyAccessor(getter, expression, descriptor.isVar());
        }
    }

    private void checkPropertyAccessor(
            @NotNull PropertyAccessorDescriptor accessor,
            @NotNull JetExpression expression, boolean isCrossingDisallowed
    ) {
        reportAnnotationIfNeeded(expression, accessor, isCrossingDisallowed);
    }

    private void checkDeprecatedForOperations(@NotNull JetReferenceExpression expression) {
        DeclarationDescriptor target = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (target != null) {
            reportAnnotationIfNeeded(expression, target, true);
        }
    }

    private boolean reportAnnotationIfNeeded(@NotNull PsiElement element, @NotNull DeclarationDescriptor descriptor) {
        return reportAnnotationIfNeeded(element, descriptor, false);
    }

    private boolean reportAnnotationIfNeeded(@NotNull PsiElement element, @NotNull DeclarationDescriptor descriptor, boolean isCrossingDisallowed) {
        AnnotationDescriptor deprecated = getDeprecated(descriptor);
        if (deprecated != null) {
            if (isCrossingDisallowed) {
                holder.createWarningAnnotation(element, composeTooltipString(descriptor, deprecated))
                        .setTextAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES);
            }
            else {
                holder.createWarningAnnotation(element, composeTooltipString(descriptor, deprecated))
                        .setTextAttributes(CodeInsightColors.DEPRECATED_ATTRIBUTES);
            }
            return true;
        }
        return false;
    }

    @Nullable
    private static AnnotationDescriptor getDeprecated(DeclarationDescriptor descriptor) {
        AnnotationDescriptor kotlinDeprecated = descriptor.getAnnotations().findAnnotation(KOTLIN_DEPRECATED);
        return kotlinDeprecated != null ? kotlinDeprecated : descriptor.getAnnotations().findAnnotation(JAVA_DEPRECATED);
    }

    private static String composeTooltipString(@NotNull DeclarationDescriptor declarationDescriptor, @NotNull AnnotationDescriptor descriptor) {
        String fact = "'" + getDescriptorString(declarationDescriptor) + "' is deprecated.";
        String message = getMessageFromAnnotationDescriptor(descriptor);
        return message == null ? fact : fact + " " + message;
    }

    @Nullable
    private static String getMessageFromAnnotationDescriptor(@NotNull AnnotationDescriptor descriptor) {
        ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(descriptor.getType());
        if (classDescriptor != null) {
            ValueParameterDescriptor parameter =
                    DescriptorResolverUtils.getAnnotationParameterByName(DEFAULT_ANNOTATION_MEMBER_NAME, classDescriptor);
            if (parameter != null) {
                CompileTimeConstant<?> valueArgument = descriptor.getAllValueArguments().get(parameter);
                if (valueArgument != null) {
                    Object value = valueArgument.getValue();
                    if (value instanceof String) {
                        return String.valueOf(value);
                    }
                }
            }
        }
        return null;
    }

    private static String getDescriptorString(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            return DescriptorUtils.getFqName(descriptor).asString();
        }
        else if (descriptor instanceof ConstructorDescriptor) {
            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            assert containingDeclaration != null;
            return "constructor for " + containingDeclaration.getName();
        }
        else if (descriptor instanceof PropertyGetterDescriptor) {
            return "getter for " + ((PropertyGetterDescriptor) descriptor).getCorrespondingProperty().getName();
        }
        else if (descriptor instanceof PropertySetterDescriptor) {
            return "setter for " + ((PropertySetterDescriptor) descriptor).getCorrespondingProperty().getName();
        }
        else if (descriptor instanceof PropertyDescriptor) {
            if (((PropertyDescriptor) descriptor).isVar()) {
                return "var " + descriptor.getName();
            }
            return "val " + descriptor.getName();
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return "fun " + descriptor.getName() + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderFunctionParameters((FunctionDescriptor) descriptor);
        }
        return DescriptorRenderer.FQ_NAMES_IN_TYPES.render(descriptor);
    }
}
