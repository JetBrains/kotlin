package org.jetbrains.jet.plugin.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringActionHandler;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.plugin.refactoring.introduceVariable.JetIntroduceVariableHandler;

/**
 * User: Alefas
 * Date: 25.01.12
 */
public class JetRefactoringSupportProvider extends RefactoringSupportProvider {
    @Override
    public RefactoringActionHandler getIntroduceVariableHandler() {
        return new JetIntroduceVariableHandler();
    }

    @Override
    public boolean isInplaceRenameAvailable(PsiElement element, PsiElement context) {
        if (element instanceof JetProperty) {
            JetProperty property = (JetProperty) element;
            if (property.isLocal()) return true;
        } else if (element instanceof JetFunction) {
            JetFunction function = (JetFunction) element;
            if (function.isLocal()) return true;
        }
        return false;
    }
}
