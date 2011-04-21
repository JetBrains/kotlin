package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.psi.*;
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
    private final Codegens codegens;

    public NamespaceCodegen(Project project, ClassVisitor v, String fqName, Codegens codegens) {
        this.project = project;
        this.v = v;
        this.codegens = codegens;

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

        final JetStandardLibrary standardLibrary = JetStandardLibrary.getJetStandardLibrary(project);
        final FunctionCodegen functionCodegen = new FunctionCodegen(v, standardLibrary, bindingContext);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(v, standardLibrary, bindingContext, functionCodegen);
        final ClassCodegen classCodegen = codegens.forClass(bindingContext);

        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.genInNamespace((JetProperty) declaration);
            }
            else if (declaration instanceof JetFunction) {
                functionCodegen.genInNamespace((JetFunction) declaration);
            }
            else if (declaration instanceof JetClass) {
                classCodegen.generate((JetClass) declaration);
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
