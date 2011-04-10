package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.ClassVisitor;

import java.util.List;

/**
 * @author max
 */
public class ClassCodegen {
    private final Project project;
    private final ClassVisitor v;
    private final BindingContext bindingContext;

    public ClassCodegen(Project project, Codegens v, JetClass jetClass, BindingContext bindingContext) {
        this.project = project;
        this.v = null;
        this.bindingContext = bindingContext;

        ClassDescriptor descriptor =  bindingContext.getClassDescriptor(jetClass);
        String fqName = CodeGenUtil.getInternalInterfaceName(descriptor);

        List<JetDelegationSpecifier> delegationSpecifiers = jetClass.getDelegationSpecifiers();
        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            JetType superType = bindingContext.resolveTypeReference(specifier.getTypeReference());
            String superClassFQN = CodeGenUtil.getInternalInterfaceName((ClassDescriptor) superType.getConstructor().getDeclarationDescriptor());
        }
        //descriptor.

/*
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                fqName.replace('.', '/'),
                null,
                //"jet/lang/Namespace",
                "java/lang/Object",
                new String[0]
                );
*/
    }

    public void generate(JetNamespace namespace) {
        final PropertyCodegen propertyCodegen = new PropertyCodegen(v);
        final FunctionCodegen functionCodegen = new FunctionCodegen(v, JetStandardLibrary.getJetStandardLibrary(project), bindingContext);

        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration, namespace);
            }
            else if (declaration instanceof JetFunction) {
                functionCodegen.gen((JetFunction) declaration, namespace);
            }
        }
    }

    public ClassVisitor getVisitor() {
        return v;
    }
}
