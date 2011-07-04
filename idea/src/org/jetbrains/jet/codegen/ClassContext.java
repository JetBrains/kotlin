/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

public class ClassContext {
    public static final ClassContext STATIC = new ClassContext(null, OwnerKind.NAMESPACE, null);
    private final DeclarationDescriptor contextType;
    private final OwnerKind contextKind;
    private final StackValue thisExpression;

    public ClassContext(DeclarationDescriptor contextType, OwnerKind contextKind, StackValue thisExpression) {
        this.contextType = contextType;
        this.contextKind = contextKind;
        this.thisExpression = thisExpression;
    }

    public DeclarationDescriptor getContextType() {
        return contextType;
    }

    public OwnerKind getContextKind() {
        return contextKind;
    }

    public StackValue getThisExpression() {
        return thisExpression;
    }
}
