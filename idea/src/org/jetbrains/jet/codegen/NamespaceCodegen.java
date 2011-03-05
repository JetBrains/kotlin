package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author max
 */
public class NamespaceCodegen {
    public NamespaceCodegen() {
    }

    public void generate(JetNamespace namespace, ClassVisitor v, Project project) {
        BindingContext bindingContext = AnalyzingUtils.analyzeNamespace(namespace, ErrorHandler.THROW_EXCEPTION);

        final PropertyCodegen propertyCodegen = new PropertyCodegen(v);
        final FunctionCodegen functionCodegen = new FunctionCodegen(v, JetStandardLibrary.getJetStandardLibrary(project), bindingContext);
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
