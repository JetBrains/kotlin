package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.Maps;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.PsiElementOnlyDiagnosticFactory;

import java.util.Map;

/**
* @author svtk
*/
public class QuickFixes {
    private static Map<PsiElementOnlyDiagnosticFactory, IntentionActionFactory> actionMap = Maps.newHashMap();

    public static IntentionActionFactory get(PsiElementOnlyDiagnosticFactory diagnosticFactory) {
        return actionMap.get(diagnosticFactory);
    }

    private QuickFixes() {}

    private static <T extends PsiElement> void add(PsiElementOnlyDiagnosticFactory<T> diagnosticFactory, IntentionActionFactory<T> actionFactory) {
        actionMap.put(diagnosticFactory, actionFactory);
    }


    static {
        add(Errors.REDUNDANT_ABSTRACT, RemoveAbstractModifierFix.factory);
        add(Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, RemoveAbstractModifierFix.factory);
        add(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, AddAbstractModifierFix.factory);
    }
}

