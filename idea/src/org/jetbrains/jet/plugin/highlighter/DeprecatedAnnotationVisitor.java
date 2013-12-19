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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationResolver;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.renderer.DescriptorRenderer;

public class DeprecatedAnnotationVisitor extends AfterAnalysisHighlightingVisitor {

    private static final TokenSet PROPERTY_SET_OPERATIONS =
            TokenSet.create(JetTokens.EQ, JetTokens.PLUSEQ, JetTokens.MINUSEQ, JetTokens.MULTEQ,
                            JetTokens.DIVEQ, JetTokens.PERCEQ, JetTokens.PLUSPLUS, JetTokens.MINUSMINUS);

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
        ResolvedCall resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, expression);
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
        // Deprecated for Class, for ClassObject (if reference isn't in UserType or in ModifierList (trait))
        if (!reportAnnotationIfNeeded(expression, target)) {
            if (PsiTreeUtil.getParentOfType(expression, JetUserType.class) == null &&
                PsiTreeUtil.getParentOfType(expression, JetModifierList.class) == null) {
                ClassDescriptor classObjectDescriptor = target.getClassObjectDescriptor();
                if (classObjectDescriptor != null) {
                    reportAnnotationIfNeeded(expression, classObjectDescriptor);
                }
            }
        }
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
        for (AnnotationDescriptor annotation : descriptor.getAnnotations()) {
            ClassDescriptor jetDeprecated = KotlinBuiltIns.getInstance().getDeprecatedAnnotation();
            ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(annotation.getType());
            if (jetDeprecated.equals(classDescriptor)) {
                return annotation;
            }
        }
        return null;
    }

    private static String composeTooltipString(@NotNull DeclarationDescriptor declarationDescriptor, @NotNull AnnotationDescriptor descriptor) {
        return "'" + getDescriptorString(declarationDescriptor) + "' is deprecated. " + getMessageFromAnnotationDescriptor(descriptor);
    }

    private static String getMessageFromAnnotationDescriptor(@NotNull AnnotationDescriptor descriptor) {
        ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(descriptor.getType());
        assert classDescriptor != null : "ClassDescriptor for jet.deprecated mustn't be null";
        ValueParameterDescriptor parameter = DescriptorResolverUtils.getAnnotationParameterByName(
                JavaAnnotationResolver.DEFAULT_ANNOTATION_MEMBER_NAME, classDescriptor);
        assert parameter != null : "jet.deprecated must have one parameter called value";
        CompileTimeConstant<?> valueArgument = descriptor.getValueArgument(parameter);
        assert valueArgument != null : "jet.deprecated must have value argument";
        return (String) valueArgument.getValue();
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
            return "fun " + descriptor.getName() + DescriptorRenderer.TEXT.renderFunctionParameters((FunctionDescriptor) descriptor);
        }
        return DescriptorRenderer.TEXT.render(descriptor);
    }
}
