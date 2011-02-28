package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * @author max
 */
public class NamespaceCodegen {
    public NamespaceCodegen() {
    }

    public void generate(JetNamespace namespace, ClassVisitor v) {
        List<JetDeclaration> declarations = namespace.getDeclarations();
        BindingContext bindingContext = new TopDownAnalyzer().process(JetStandardClasses.STANDARD_CLASSES, declarations);

        final PropertyCodegen propertyCodegen = new PropertyCodegen(v);
        final FunctionCodegen functionCodegen = new FunctionCodegen(v, bindingContext);
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                getJVMClassName(namespace),
                null,
                //"jet/lang/Namespace",
                "java/lang/Object",
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

    public static String getJVMClassName(JetNamespace namespace) {
        return namespace.getFQName().replace('.', '/') + "/namespace";
    }
}
