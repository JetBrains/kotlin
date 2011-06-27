/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

public class EnclosedValueDescriptor {
    private final DeclarationDescriptor descriptor;
    private final StackValue innerValue;
    private final StackValue outerValue;

    public EnclosedValueDescriptor(DeclarationDescriptor descriptor, StackValue innerValue, StackValue outerValue) {
        this.descriptor = descriptor;
        this.innerValue = innerValue;
        this.outerValue = outerValue;
    }

    public DeclarationDescriptor getDescriptor() {
        return descriptor;
    }

    public StackValue getInnerValue() {
        return innerValue;
    }

    public StackValue getOuterValue() {
        return outerValue;
    }
}
