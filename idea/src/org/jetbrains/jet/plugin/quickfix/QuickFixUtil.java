package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameter;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElementImpl;

/**
 * @author svtk
 */
public class QuickFixUtil {
    private QuickFixUtil() {}

    public static <T extends PsiElement, P extends T> JetIntentionActionFactory<PsiElement> createFactoryRedirectingAdditionalInfoToAnotherFactory(final JetIntentionActionFactory<T> factory, final DiagnosticParameter<P> parameter) {
        return new JetIntentionActionFactory<PsiElement>() {
            @Override
            public JetIntentionAction<PsiElement> createAction(DiagnosticWithPsiElement diagnostic) {

                DiagnosticWithParameters<PsiElement> diagnosticWithParameters = JetIntentionAction.assertAndCastToDiagnosticWithParameters(diagnostic, parameter);
                T element = diagnosticWithParameters.getParameter(parameter);
                return (JetIntentionAction<PsiElement>) factory.createAction(new DiagnosticWithPsiElementImpl<T>(diagnostic.getFactory(), diagnostic.getSeverity(), diagnostic.getMessage(), element));
            }
        };
    }
    
}
