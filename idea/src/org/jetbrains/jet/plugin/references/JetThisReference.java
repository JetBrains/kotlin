package org.jetbrains.jet.plugin.references;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.jet.lang.psi.JetThisReferenceExpression;

/**
* @author yole
*/
public class JetThisReference extends JetPsiReference {
    public JetThisReference(JetThisReferenceExpression expression) {
        super(expression);
    }

    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, getElement().getTextLength());
    }
}
