package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetNamespace;
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

    public void gen(JetProperty p, JetNamespace owner) {

    }

    public void gen(JetProperty p, JetClass owner) {

    }
}
