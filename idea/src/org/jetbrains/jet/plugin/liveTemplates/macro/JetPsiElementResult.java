package org.jetbrains.jet.plugin.liveTemplates.macro;

import com.intellij.codeInsight.template.JavaPsiElementResult;
import com.intellij.psi.PsiNamedElement;

/**
 * @author Evgeny Gerashchenko
 * @since 1/30/12
 */
public class JetPsiElementResult extends JavaPsiElementResult {
    public JetPsiElementResult(PsiNamedElement element) {
        super(element);
    }

    @Override
    public String toString() {
        return ((PsiNamedElement) getElement()).getName();
    }
}
