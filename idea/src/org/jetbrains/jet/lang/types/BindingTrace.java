package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetTypeReference;

/**
 * @author abreslav
 */
public class BindingTrace {
    public static final BindingTrace DUMMY = new BindingTrace();

    public void recordExpressionType(@NotNull JetExpression expression, @NotNull Type type) {
    }

    public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor) {

    }

    public void recordDeclarationResolution(@NotNull JetDeclaration declaration, @NotNull DeclarationDescriptor descriptor) {

    }

    public void recordTypeResolution(@NotNull JetTypeReference typeReference, @NotNull Type type) {

    }
}
