package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.JavaLangScope;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * @author max
 */
public class NamespaceCodegen {
    public NamespaceCodegen() {
    }

    public void generate(JetNamespace namespace, ClassVisitor v, Project project) {
        List<JetDeclaration> declarations = namespace.getDeclarations();

        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(project, ErrorHandler.THROW_EXCEPTION);
        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        ScopeWithImports scope = new ScopeWithImports(semanticServices.getStandardLibrary().getLibraryScope());
        scope.addImport(new JavaLangScope(new JavaSemanticServices(project, semanticServices, bindingTraceContext)));
        new TopDownAnalyzer(semanticServices, bindingTraceContext).process(scope, declarations);
        BindingContext bindingContext = bindingTraceContext;

        final PropertyCodegen propertyCodegen = new PropertyCodegen(v);
        final FunctionCodegen functionCodegen = new FunctionCodegen(v, semanticServices.getStandardLibrary(), bindingContext);
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
