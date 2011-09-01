package org.jetbrains.jet.plugin.annotations;

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
import org.jetbrains.jet.lang.JetDiagnostic;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetHighlighter;

import java.util.Set;

import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author abreslav
 */
public class DebugInfoAnnotator implements Annotator {

    private static volatile boolean debugInfoEnabled = !ApplicationManager.getApplication().isUnitTestMode();
    public static final TokenSet EXCLUDED = TokenSet.create(COLON, AS_KEYWORD, AS_SAFE);

    public static void setDebugInfoEnabled(boolean value) {
        debugInfoEnabled = value;
    }

    public static boolean isDebugInfoEnabled() {
        return debugInfoEnabled;
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        if (!debugInfoEnabled) {
            return;
        }
        
        if (element instanceof JetFile) {
            JetFile file = (JetFile) element;
            try {
                final BindingContext bindingContext = AnalyzingUtils.analyzeFileWithCache(file);

                final Set<JetReferenceExpression> unresolvedReferences = Sets.newHashSet();
                for (JetDiagnostic diagnostic : bindingContext.getDiagnostics()) {
                    if (diagnostic instanceof JetDiagnostic.UnresolvedReferenceError) {
                        JetDiagnostic.UnresolvedReferenceError error = (JetDiagnostic.UnresolvedReferenceError) diagnostic;
                        unresolvedReferences.add(error.getReferenceExpression());                        
                    }
                }

                file.acceptChildren(new JetVisitorVoid() {

                    @Override
                    public void visitReferenceExpression(JetReferenceExpression expression) {
                        if (expression instanceof JetSimpleNameExpression) {
                            JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) expression;
                            IElementType elementType = expression.getNode().getElementType();
                            if (elementType == JetNodeTypes.OPERATION_REFERENCE && EXCLUDED.contains(nameExpression.getReferencedNameElementType())) {
                                return;
                            }
                        }
                        DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, expression);
                        boolean resolved = declarationDescriptor != null;
                        boolean unresolved = unresolvedReferences.contains(expression);
                        if (resolved && unresolved) {
                            holder.createErrorAnnotation(expression, "Reference marked as unresolved is actually resolved to " + declarationDescriptor).setTextAttributes(JetHighlighter.JET_DEBUG_INFO);
                        }
                        else if (!resolved && !unresolved) {
                            holder.createErrorAnnotation(expression, "Reference is not resolved to anything, but is not marked unresolved").setTextAttributes(JetHighlighter.JET_DEBUG_INFO);
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
