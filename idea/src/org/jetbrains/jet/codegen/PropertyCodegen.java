package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.JetProperty;
import org.objectweb.asm.ClassVisitor;

/**
 * @author max
 */
public class PropertyCodegen {
    private final ClassVisitor v;

    public PropertyCodegen(ClassVisitor v) {
        this.v = v;
    }

    public void genInNamespace(JetProperty p) {
       gen(p, OwnerKind.NAMESPACE);
    }

    public void genInInterface(JetProperty p) {
        gen(p, OwnerKind.INTERFACE);
    }

    public void genInImplementation(JetProperty p) {
        gen(p, OwnerKind.IMPLEMENTATION);
    }

    public void genInDelegatingImplementation(JetProperty p) {
        gen(p, OwnerKind.DELEGATING_IMPLEMENTATION);
    }

    public void gen(JetProperty p, OwnerKind kind) {

    }
}
