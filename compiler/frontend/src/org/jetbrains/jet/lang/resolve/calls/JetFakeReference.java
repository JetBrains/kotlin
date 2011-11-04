package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

/**
 *
 * This class is used to wrap an expression that occurs in a reference position, such as a function literal, into a reference expression
 *
 * @author abreslav
 */
public class JetFakeReference extends JetReferenceExpression {
    private final JetElement actualElement;

    public JetFakeReference(@NotNull JetElement actualElement) {
        super(actualElement.getNode());
        this.actualElement = actualElement;
    }

    public JetElement getActualElement() {
        return actualElement;
    }
}
