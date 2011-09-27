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

    public static <T extends PsiElement> JetIntentionActionFactory<T> createFactoryRedirectingAdditionalInfoToAnotherFactory(final JetIntentionActionFactory<T> factory, final DiagnosticParameter<T> parameter) {
        return new JetIntentionActionFactory<T>() {
            @Override
            public JetIntentionAction<T> createAction(DiagnosticWithPsiElement diagnostic) {

                DiagnosticWithParameters<PsiElement> diagnosticWithParameters = JetIntentionAction.assertAndCastToDiagnosticWithParameters(diagnostic, parameter);
                T element = diagnosticWithParameters.getParameter(parameter);
                return factory.createAction(new DiagnosticWithPsiElementImpl<T>(diagnostic.getFactory(), diagnostic.getSeverity(), diagnostic.getMessage(), element));
            }
        };
    }
    
}
