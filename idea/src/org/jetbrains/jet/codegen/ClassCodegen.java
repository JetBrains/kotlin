package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * @author max
 */
public class ClassCodegen {
    private final Project project;
    private final BindingContext bindingContext;
    private final Codegens factory;

    public ClassCodegen(Project project, Codegens factory, BindingContext bindingContext) {
        this.project = project;
        this.factory = factory;
        this.bindingContext = bindingContext;

    }

    public void generate(JetClass aClass) {
        ClassDescriptor descriptor =  bindingContext.getClassDescriptor(aClass);
        String fqName = CodeGenUtil.getInternalInterfaceName(descriptor);

        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            JetType superType = bindingContext.resolveTypeReference(specifier.getTypeReference());
            String superClassFQN = CodeGenUtil.getInternalInterfaceName((ClassDescriptor) superType.getConstructor().getDeclarationDescriptor());
        }

        //descriptor.

        ClassVisitor v = factory.forClassImplementation(descriptor);
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                fqName.replace('.', '/'),
                null,
                //"jet/lang/Namespace",
                "java/lang/Object",
                new String[0]
                );

        final PropertyCodegen propertyCodegen = new PropertyCodegen(v);
        final FunctionCodegen functionCodegen = new FunctionCodegen(v, JetStandardLibrary.getJetStandardLibrary(project), bindingContext);

        for (JetDeclaration declaration : aClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.genInInterface((JetProperty) declaration);
            }
            else if (declaration instanceof JetFunction) {
                functionCodegen.genInInterface((JetFunction) declaration);
            }
        }
    }
}
