package org.jetbrains.jet.plugin.quickfix;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameter;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElementImpl;

/**
 * @author svtk
 */
public class QuickFixUtil {
    private QuickFixUtil() {
    }

    public static <T extends PsiElement, P extends T> JetIntentionActionFactory<PsiElement> createFactoryRedirectingAdditionalInfoToAnotherFactory(final JetIntentionActionFactory<T> factory, final DiagnosticParameter<P> parameter) {
        return new JetIntentionActionFactory<PsiElement>() {
            @Override
            public JetIntentionAction<PsiElement> createAction(DiagnosticWithPsiElement diagnostic) {

                DiagnosticWithParameters<PsiElement> diagnosticWithParameters = JetIntentionAction.assertAndCastToDiagnosticWithParameters(diagnostic, parameter);
                P element = diagnosticWithParameters.getParameter(parameter);
                return (JetIntentionAction<PsiElement>) factory.createAction(new DiagnosticWithPsiElementImpl<T>(diagnostic.getFactory(), diagnostic.getSeverity(), diagnostic.getMessage(), element));
            }
        };
    }

    public static <T extends PsiElement, P extends T> JetIntentionActionFactory<PsiElement> createFactoryRedirectingAdditionalInfoIfAnyToAnotherFactory(final JetIntentionActionFactory<T> factory, final DiagnosticParameter<P> parameter) {
        return new JetIntentionActionFactory<PsiElement>() {
            @Override
            public JetIntentionAction<PsiElement> createAction(DiagnosticWithPsiElement diagnostic) {

                if (diagnostic instanceof DiagnosticWithParameters && ((DiagnosticWithParameters<PsiElement>) diagnostic).hasParameter(parameter)) {
                    P element = ((DiagnosticWithParameters<PsiElement>) diagnostic).getParameter(parameter);
                    return (JetIntentionAction<PsiElement>) factory.createAction(new DiagnosticWithPsiElementImpl<T>(diagnostic.getFactory(), diagnostic.getSeverity(), diagnostic.getMessage(), element));
                }
                return createDoNothingAction(diagnostic);
            }
        };
    }

    public static JetIntentionAction<PsiElement> createDoNothingAction(DiagnosticWithPsiElement diagnostic) {
        return new JetIntentionAction<PsiElement>(diagnostic.getPsiElement()) {
            @Override
            public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
                return false;
            }

            @NotNull
            @Override
            public String getText() {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public String getFamilyName() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static boolean removePossiblyWhiteSpace(ASTDelegatePsiElement element, PsiElement possiblyWhiteSpace) {
        if (possiblyWhiteSpace instanceof PsiWhiteSpace) {
            element.deleteChildInternal(possiblyWhiteSpace.getNode());
            return true;
        }
        return false;
    }
}
