package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;

/**
* @author svtk
*/
public interface JetIntentionActionFactory<T extends PsiElement> {

    JetIntentionAction<T> createAction(DiagnosticWithPsiElement diagnostic);

}
