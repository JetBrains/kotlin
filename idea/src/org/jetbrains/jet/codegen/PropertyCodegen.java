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

    }

    public void genInInterface(JetProperty p) {

    }

    public void genInImplementation(JetProperty p) {

    }

    public void genInDelegatingImplementation(JetProperty p) {

    }
}
