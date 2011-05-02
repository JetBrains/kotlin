package org.jetbrains.jet.codegen;

import org.objectweb.asm.Type;

/**
 * @author max
 */
public class OwnerKind {
    public static final OwnerKind NAMESPACE = new OwnerKind();
    public static final OwnerKind INTERFACE = new OwnerKind();
    public static final OwnerKind IMPLEMENTATION = new OwnerKind();
    public static final OwnerKind DELEGATING_IMPLEMENTATION = new OwnerKind();

    public static class DelegateKind extends OwnerKind {
        private final StackValue delegate;
        private final String ownerClass;

        public DelegateKind(StackValue delegate, String ownerClass) {
            this.delegate = delegate;
            this.ownerClass = ownerClass;
        }

        public StackValue getDelegate() {
            return delegate;
        }

        public String getOwnerClass() {
            return ownerClass;
        }
    }
}
