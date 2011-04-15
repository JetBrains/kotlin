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
    private final Project project;
    private final ClassVisitor v;

    public NamespaceCodegen(Project project, ClassVisitor v, String fqName) {
        this.project = project;
        this.v = v;

        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                getJVMClassName(fqName),
                null,
                //"jet/lang/Namespace",
                "java/lang/Object",
                new String[0]
                );
    }

    public void generate(JetNamespace namespace) {
        BindingContext bindingContext = AnalyzingUtils.analyzeNamespace(namespace, ErrorHandler.THROW_EXCEPTION);

        final PropertyCodegen propertyCodegen = new PropertyCodegen(v);
        final FunctionCodegen functionCodegen = new FunctionCodegen(v, JetStandardLibrary.getJetStandardLibrary(project), bindingContext);

        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.genInNamespace((JetProperty) declaration);
            }
            else if (declaration instanceof JetFunction) {
                functionCodegen.genInNamespace((JetFunction) declaration);
            }
        }
    }

    public void done() {
        v.visitEnd();
    }

    public static String getJVMClassName(String fqName) {
        if (fqName.length() == 0) {
            return "namespace";
        }
        return fqName.replace('.', '/') + "/namespace";
    }
}
