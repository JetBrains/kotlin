package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.Maps;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.PsiElementOnlyDiagnosticFactory;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

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
        IntentionActionFactory<JetModifierListOwner> removeAbstractModifierFactory = RemoveModifierFix.createFactory(JetTokens.ABSTRACT_KEYWORD);
        IntentionActionFactory<JetModifierListOwner> addAbstractModifierFactory = AddModifierFix.createFactory(JetTokens.ABSTRACT_KEYWORD, new JetToken[]{JetTokens.OPEN_KEYWORD});

        add(Errors.REDUNDANT_ABSTRACT, removeAbstractModifierFactory);
        add(Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
        add(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addAbstractModifierFactory);
    }
}

