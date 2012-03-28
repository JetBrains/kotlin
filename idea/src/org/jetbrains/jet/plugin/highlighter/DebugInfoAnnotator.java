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

package org.jetbrains.jet.plugin.highlighter;

import com.google.common.collect.Sets;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.UnresolvedReferenceDiagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;

import java.util.Collection;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * Quick showing possible problems with jet internals in IDEA with a tooltips
 *
 * @author abreslav
 */
public class DebugInfoAnnotator implements Annotator {

    public static final TokenSet EXCLUDED = TokenSet.create(COLON, AS_KEYWORD, AS_SAFE, IS_KEYWORD, NOT_IS, OROR, ANDAND, EQ, EQEQEQ, EXCLEQEQEQ, ELVIS, EXCLEXCL);

    public static boolean isDebugInfoEnabled() {
        return ApplicationManager.getApplication().isInternal();
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        if (!isDebugInfoEnabled() || !JetPsiChecker.isErrorReportingEnabled()) {
            return;
        }
        
        if (element instanceof JetFile) {
            JetFile file = (JetFile) element;
            try {
                final BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file);

                final Set<JetReferenceExpression> unresolvedReferences = Sets.newHashSet();
                for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
                    if (diagnostic instanceof UnresolvedReferenceDiagnostic) {
                        unresolvedReferences.add(((UnresolvedReferenceDiagnostic) diagnostic).getPsiElement());
                    }
                }

                file.acceptChildren(new JetVisitorVoid() {

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
                        else {
                            PsiElement labelTarget = bindingContext.get(LABEL_TARGET, expression);
                            if (labelTarget != null) {
                                target = labelTarget.getText();
                            }
                            else {
                                Collection<? extends DeclarationDescriptor> declarationDescriptors = bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, expression);
                                if (declarationDescriptors != null) {
                                    target = "[" + declarationDescriptors.size() + " descriptors]";
                                }
                            }
                        }
                        boolean resolved = target != null;
                        boolean unresolved = unresolvedReferences.contains(expression);
                        JetType expressionType = bindingContext.get(EXPRESSION_TYPE, expression);
                        if (declarationDescriptor != null && !ApplicationManager.getApplication().isUnitTestMode() && (ErrorUtils.isError(declarationDescriptor) || ErrorUtils.containsErrorType(expressionType))) {
                            holder.createErrorAnnotation(expression, "[DEBUG] Resolved to error element").setTextAttributes(JetHighlightingColors.RESOLVED_TO_ERROR);
                        }
                        if (resolved && unresolved) {
                            holder.createErrorAnnotation(expression, "[DEBUG] Reference marked as unresolved is actually resolved to " + target).setTextAttributes(JetHighlightingColors.DEBUG_INFO);
                        }
                        else if (!resolved && !unresolved) {
                            holder.createErrorAnnotation(expression, "[DEBUG] Reference is not resolved to anything, but is not marked unresolved").setTextAttributes(JetHighlightingColors.DEBUG_INFO);
                        }
                    }

                    @Override
                    public void visitJetElement(JetElement element) {
                        element.acceptChildren(this);
                    }
                });
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                // TODO
                holder.createErrorAnnotation(element, e.getClass().getCanonicalName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
