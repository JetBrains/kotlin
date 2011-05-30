package org.jetbrains.jet.codegen;

/**
 * @author max
 */
public class OwnerKind {
    private final String name;

    public OwnerKind(String name) {
        this.name = name;
    }

    public static final OwnerKind NAMESPACE = new OwnerKind("namespace");
    public static final OwnerKind INTERFACE = new OwnerKind("interface");
    public static final OwnerKind IMPLEMENTATION = new OwnerKind("implementation");
    public static final OwnerKind DELEGATING_IMPLEMENTATION = new OwnerKind("delegating implementation");

    public static class DelegateKind extends OwnerKind {
        private final StackValue delegate;
        private final String ownerClass;

        public DelegateKind(StackValue delegate, String ownerClass) {
            super("delegateKind");
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

    @Override
    public String toString() {
        return "OwnerKind(" + name + ")";
    }
}
