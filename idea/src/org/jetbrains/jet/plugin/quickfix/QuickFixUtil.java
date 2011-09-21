package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithAdditionalInfo;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElementImpl;

/**
 * @author svtk
 */
public class QuickFixUtil {
    private QuickFixUtil() {}

    public static <T extends PsiElement> IntentionActionFactory<T> createFactoryRedirectingAdditionalInfoToAnotherFactory(final IntentionActionFactory<T> factory) {
        return new IntentionActionFactory<T>() {
            @Override
            public IntentionActionForPsiElement<T> createAction(DiagnosticWithPsiElement diagnostic) {
                //no type check; should be followed manually
                assert diagnostic instanceof DiagnosticWithAdditionalInfo;
                Object info = ((DiagnosticWithAdditionalInfo) diagnostic).getInfo();
                T element = null;
                try {
                    element = (T) info;
                }
                catch (ClassCastException ex) {
                    assert false : ex;
                }
                return factory.createAction(new DiagnosticWithPsiElementImpl<T>(diagnostic.getFactory(), diagnostic.getSeverity(), diagnostic.getMessage(), element));
            }
        };
    }
    
}
