package org.jetbrains.jet.plugin.liveTemplates.macro;

import com.intellij.codeInsight.template.PsiElementResult;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;

/**
 * @author Evgeny Gerashchenko
 * @since 1/30/12
 */
public class JetPsiElementResult extends PsiElementResult {
    public JetPsiElementResult(JetNamedDeclaration element) {
        super(element);
    }

    @Override
    public String toString() {
        return ((JetNamedDeclaration) getElement()).getName();
    }
}
