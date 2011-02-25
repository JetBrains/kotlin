package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author max
 */
public class NamespaceCodegen {
    private final ClassVisitorFactory factory;

    public NamespaceCodegen(ClassVisitorFactory factory) {
        this.factory = factory;
    }

    public void generate(JetNamespace namespace) {
        final ClassVisitor v = factory.visitorForClassIn(namespace);

        final PropertyCodegen propertyCodegen = new PropertyCodegen(v);
        final FunctionCodegen functionCodegen = new FunctionCodegen(v);
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                namespace.getFQName().replace('.', '/') + "/namespace",
                null,
                "jet/lang/Namespace",
                new String[0]
                );

        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration, namespace);
            }
            else if (declaration instanceof JetFunction) {
                functionCodegen.gen((JetFunction) declaration, namespace);
            }
        }


        v.visitEnd();
    }
}
