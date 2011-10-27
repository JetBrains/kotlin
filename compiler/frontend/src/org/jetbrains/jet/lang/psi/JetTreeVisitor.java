package org.jetbrains.jet.lang.psi;

/**
 * @author svtk
 */
public class JetTreeVisitor<D> extends JetVisitor<Void, D> {
    @Override
    public Void visitJetElement(JetElement element, D data) {
        element.acceptChildren(this, data);
        return null;
    }
}
