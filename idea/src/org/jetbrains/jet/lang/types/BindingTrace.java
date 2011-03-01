package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetTypeReference;

/**
 * @author abreslav
 */
public class BindingTrace {
    public static final BindingTrace DUMMY = new BindingTrace();

    public void recordExpressionType(JetExpression expression, Type type) {
    }

    public void recordReferenceResolution(JetReferenceExpression expression, DeclarationDescriptor descriptor) {

    }

    public void recordDeclarationResolution(JetDeclaration declaration, DeclarationDescriptor descriptor) {

    }

    public void recordTypeResolution(JetTypeReference typeReference, Type type) {

    }
}
