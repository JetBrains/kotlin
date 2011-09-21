package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;

/**
* @author svtk
*/
public interface IntentionActionFactory<T extends PsiElement> {

    IntentionActionForPsiElement<T> createAction(DiagnosticWithPsiElement diagnostic);

}
