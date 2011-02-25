package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.objectweb.asm.ClassVisitor;

/**
 * @author max
 */
public class FunctionCodegen {
    private final ClassVisitor v;

    public FunctionCodegen(ClassVisitor v) {
        this.v = v;
    }

    public void gen(JetFunction f, JetNamespace owner) {

    }

    public void gen(JetFunction f, JetClass owner) {

    }
}
