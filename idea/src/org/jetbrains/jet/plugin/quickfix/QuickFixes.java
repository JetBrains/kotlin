package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.Maps;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.PsiElementOnlyDiagnosticFactory;

import java.util.Map;

/**
* @author svtk
*/
public class QuickFixes {
    private static Map<PsiElementOnlyDiagnosticFactory, IntentionActionFactory> actionMap = Maps.newHashMap();

    public static IntentionActionFactory get(PsiElementOnlyDiagnosticFactory f) {
        return actionMap.get(f);
    }

    private QuickFixes() {}

    public static abstract class IntentionActionForPsiElement<T extends PsiElement> implements IntentionAction {
        protected T psiElement;

        public IntentionActionForPsiElement(T element) {
            this.psiElement = element;
        }
    }

    public interface IntentionActionFactory<T extends PsiElement> {
        IntentionActionForPsiElement createAction(DiagnosticWithPsiElement diagnostic);
    }

    private static <T extends PsiElement> void add(PsiElementOnlyDiagnosticFactory<T> diagnosticFactory, IntentionActionFactory<T> actionFactory) {
        actionMap.put(diagnosticFactory, actionFactory);
    }


    static {
        add(Errors.REDUNDANT_ABSTRACT, RemoveAbstractModifierFix.factory);
    }
}

