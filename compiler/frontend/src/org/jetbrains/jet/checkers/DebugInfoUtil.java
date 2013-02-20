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

package org.jetbrains.jet.checkers;

import com.google.common.collect.Maps;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.AbstractDiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetSuperExpression;
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class DebugInfoUtil {
    private static final TokenSet EXCLUDED = TokenSet.create(
            COLON, AS_KEYWORD, AS_SAFE, IS_KEYWORD, NOT_IS, OROR, ANDAND, EQ, EQEQEQ, EXCLEQEQEQ, ELVIS, EXCLEXCL, IN_KEYWORD, NOT_IN);

    public interface DebugInfoReporter {

        void reportElementWithErrorType(@NotNull JetReferenceExpression expression);

        void reportMissingUnresolved(@NotNull JetReferenceExpression expression);

        void reportUnresolvedWithTarget(@NotNull JetReferenceExpression expression, @NotNull String target);
    }

    public static void markDebugAnnotations(
            @NotNull PsiElement root,
            @NotNull final BindingContext bindingContext,
            @NotNull final DebugInfoReporter debugInfoReporter
    ) {
        final Map<JetReferenceExpression, AbstractDiagnosticFactory> markedWithErrorElements = Maps.newHashMap();
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            AbstractDiagnosticFactory factory = diagnostic.getFactory();
            if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                markedWithErrorElements.put((JetReferenceExpression) diagnostic.getPsiElement(), factory);
            }
            else if (factory == Errors.SUPER_IS_NOT_AN_EXPRESSION
                    || factory == Errors.SUPER_NOT_AVAILABLE) {
                JetSuperExpression superExpression = (JetSuperExpression) diagnostic.getPsiElement();
                markedWithErrorElements.put(superExpression.getInstanceReference(), factory);
            }
            else if (factory == Errors.EXPRESSION_EXPECTED_NAMESPACE_FOUND) {
                markedWithErrorElements.put((JetSimpleNameExpression) diagnostic.getPsiElement(), factory);
            }
        }

        root.acceptChildren(new JetTreeVisitorVoid() {

            @Override
            public void visitReferenceExpression(JetReferenceExpression expression) {
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) expression;
                    IElementType elementType = expression.getNode().getElementType();
                    if (elementType == JetNodeTypes.OPERATION_REFERENCE) {
                        IElementType referencedNameElementType = nameExpression.getReferencedNameElementType();
                        if (EXCLUDED.contains(referencedNameElementType)) {
                            return;
                        }
                        if (JetTokens.LABELS.contains(referencedNameElementType)) return;
                    }
                    else if (nameExpression.getReferencedNameElementType() == JetTokens.THIS_KEYWORD) {
                        return;
                    }
                }

                String target = null;
                DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, expression);
                if (declarationDescriptor != null) {
                    target = declarationDescriptor.toString();
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

                boolean resolved = target != null;
                boolean markedWithError = markedWithErrorElements.containsKey(expression);
                JetType expressionType = bindingContext.get(EXPRESSION_TYPE, expression);
                AbstractDiagnosticFactory factory = markedWithErrorElements.get(expression);
                if (declarationDescriptor != null &&
                    (ErrorUtils.isError(declarationDescriptor) || ErrorUtils.containsErrorType(expressionType))) {
                    if (factory != Errors.EXPRESSION_EXPECTED_NAMESPACE_FOUND) {
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
        });
    }

}
