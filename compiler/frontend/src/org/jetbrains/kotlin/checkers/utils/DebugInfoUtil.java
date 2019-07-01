/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallsKt;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.kotlin.lexer.KtTokens.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;

public class DebugInfoUtil {
    private static final TokenSet MAY_BE_UNRESOLVED = TokenSet.create(IN_KEYWORD, NOT_IN);
    private static final TokenSet EXCLUDED = TokenSet.create(
            COLON, AS_KEYWORD, AS_SAFE, IS_KEYWORD, NOT_IS, OROR, ANDAND, EQ, EQEQEQ, EXCLEQEQEQ, ELVIS, EXCLEXCL);

    public abstract static class DebugInfoReporter {

        public void preProcessReference(@NotNull KtReferenceExpression expression) {
            // do nothing
        }

        public abstract void reportElementWithErrorType(@NotNull KtReferenceExpression expression);

        public abstract void reportMissingUnresolved(@NotNull KtReferenceExpression expression);

        public abstract void reportUnresolvedWithTarget(@NotNull KtReferenceExpression expression, @NotNull String target);

        public void reportDynamicCall(@NotNull KtElement element, DeclarationDescriptor declarationDescriptor) { }
    }

    public static void markDebugAnnotations(
            @NotNull PsiElement root,
            @NotNull BindingContext bindingContext,
            @NotNull DebugInfoReporter debugInfoReporter
    ) {
        Map<KtReferenceExpression, DiagnosticFactory<?>> markedWithErrorElements = new HashMap<>();
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            DiagnosticFactory<?> factory = diagnostic.getFactory();
            if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                markedWithErrorElements.put((KtReferenceExpression) diagnostic.getPsiElement(), factory);
            }
            else if (factory == Errors.SUPER_IS_NOT_AN_EXPRESSION
                    || factory == Errors.SUPER_NOT_AVAILABLE) {
                KtSuperExpression superExpression = (KtSuperExpression) diagnostic.getPsiElement();
                markedWithErrorElements.put(superExpression.getInstanceReference(), factory);
            }
            else if (factory == Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND) {
                markedWithErrorElements.put((KtSimpleNameExpression) diagnostic.getPsiElement(), factory);
            }
            else if (factory == Errors.UNSUPPORTED) {
                for (KtReferenceExpression reference : PsiTreeUtil.findChildrenOfType(diagnostic.getPsiElement(),
                                                                                      KtReferenceExpression.class)) {
                    markedWithErrorElements.put(reference, factory);
                }
            }
        }

        root.acceptChildren(new KtTreeVisitorVoid() {

            @Override
            public void visitForExpression(@NotNull KtForExpression expression) {
                KtExpression range = expression.getLoopRange();
                reportIfDynamicCall(range, range, LOOP_RANGE_ITERATOR_RESOLVED_CALL);
                reportIfDynamicCall(range, range, LOOP_RANGE_HAS_NEXT_RESOLVED_CALL);
                reportIfDynamicCall(range, range, LOOP_RANGE_NEXT_RESOLVED_CALL);
                super.visitForExpression(expression);
            }

            @Override
            public void visitDestructuringDeclaration(@NotNull KtDestructuringDeclaration destructuringDeclaration) {
                for (KtDestructuringDeclarationEntry entry : destructuringDeclaration.getEntries()) {
                    reportIfDynamicCall(entry, entry, COMPONENT_RESOLVED_CALL);
                }
                super.visitDestructuringDeclaration(destructuringDeclaration);
            }

            @Override
            public void visitProperty(@NotNull KtProperty property) {
                VariableDescriptor descriptor = bindingContext.get(VARIABLE, property);
                if (descriptor instanceof PropertyDescriptor && property.getDelegate() != null) {
                    PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
                    reportIfDynamicCall(property.getDelegate(), propertyDescriptor, PROVIDE_DELEGATE_RESOLVED_CALL);
                    reportIfDynamicCall(property.getDelegate(), propertyDescriptor.getGetter(), DELEGATED_PROPERTY_RESOLVED_CALL);
                    reportIfDynamicCall(property.getDelegate(), propertyDescriptor.getSetter(), DELEGATED_PROPERTY_RESOLVED_CALL);
                }
                super.visitProperty(property);
            }

            @Override
            public void visitThisExpression(@NotNull KtThisExpression expression) {
                ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilKt.getResolvedCall(expression, bindingContext);
                if (resolvedCall != null) {
                    reportIfDynamic(expression, resolvedCall.getResultingDescriptor(), debugInfoReporter);
                }
                super.visitThisExpression(expression);
            }

            @Override
            public void visitReferenceExpression(@NotNull KtReferenceExpression expression) {
                super.visitReferenceExpression(expression);
                if (!BindingContextUtils.isExpressionWithValidReference(expression, bindingContext)){
                    return;
                }
                IElementType referencedNameElementType = null;
                if (expression instanceof KtSimpleNameExpression) {
                    KtSimpleNameExpression nameExpression = (KtSimpleNameExpression) expression;
                    IElementType elementType = expression.getNode().getElementType();
                    if (elementType == KtNodeTypes.OPERATION_REFERENCE) {
                        referencedNameElementType = nameExpression.getReferencedNameElementType();
                        if (EXCLUDED.contains(referencedNameElementType)) {
                            return;
                        }
                    }
                    if (elementType == KtNodeTypes.LABEL ||
                        nameExpression.getReferencedNameElementType() == KtTokens.THIS_KEYWORD) {
                        return;
                    }
                }

                debugInfoReporter.preProcessReference(expression);

                String target = null;
                DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, expression);
                if (declarationDescriptor != null) {
                    target = declarationDescriptor.toString();

                    reportIfDynamic(expression, declarationDescriptor, debugInfoReporter);
                }
                if (target == null) {
                    PsiElement labelTarget = bindingContext.get(LABEL_TARGET, expression);
                    if (labelTarget != null) {
                        target = labelTarget.getText();
                    }
                }
                if (target == null) {
                    Collection<? extends DeclarationDescriptor> declarationDescriptors =
                            bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, expression);
                    if (declarationDescriptors != null) {
                        target = "[" + declarationDescriptors.size() + " descriptors]";
                    }
                }
                if (target == null) {
                    Collection<? extends PsiElement> labelTargets = bindingContext.get(AMBIGUOUS_LABEL_TARGET, expression);
                    if (labelTargets != null) {
                        target = "[" + labelTargets.size() + " elements]";
                    }
                }

                if (MAY_BE_UNRESOLVED.contains(referencedNameElementType)) {
                    return;
                }

                boolean resolved = target != null;
                boolean markedWithError = markedWithErrorElements.containsKey(expression);
                if (expression instanceof KtArrayAccessExpression &&
                    markedWithErrorElements.containsKey(((KtArrayAccessExpression) expression).getArrayExpression())) {
                    // if 'foo' in 'foo[i]' is unresolved it means 'foo[i]' is unresolved (otherwise 'foo[i]' is marked as 'missing unresolved')
                    markedWithError = true;
                }
                KotlinType expressionType = bindingContext.getType(expression);
                DiagnosticFactory<?> factory = markedWithErrorElements.get(expression);
                if (declarationDescriptor != null &&
                    (ErrorUtils.isError(declarationDescriptor) || ErrorUtils.containsErrorType(expressionType))) {
                    if (factory != Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND) {
                        debugInfoReporter.reportElementWithErrorType(expression);
                    }
                }
                if (resolved && markedWithError) {
                    if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(factory)) {
                        debugInfoReporter.reportUnresolvedWithTarget(expression, target);
                    }
                }
                else if (!resolved && !markedWithError) {
                    debugInfoReporter.reportMissingUnresolved(expression);
                }
            }

            private <E extends KtElement, K, D extends CallableDescriptor> boolean reportIfDynamicCall(E element, K key, WritableSlice<K, ResolvedCall<D>> slice) {
                ResolvedCall<D> resolvedCall = bindingContext.get(slice, key);
                if (resolvedCall != null) {
                    return reportIfDynamic(element, resolvedCall.getResultingDescriptor(), debugInfoReporter);
                }
                return false;
            }
        });
    }

    private static boolean reportIfDynamic(KtElement element, DeclarationDescriptor declarationDescriptor, DebugInfoReporter debugInfoReporter) {
        if (declarationDescriptor != null && DynamicCallsKt.isDynamic(declarationDescriptor)) {
            debugInfoReporter.reportDynamicCall(element, declarationDescriptor);
            return true;
        }
        return false;
    }
}
