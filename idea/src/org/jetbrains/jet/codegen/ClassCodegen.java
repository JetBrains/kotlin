package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

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

    public void generate(JetClassOrObject aClass) {
        if (aClass instanceof JetObjectDeclaration) {
            generateImplementation(aClass, OwnerKind.IMPLEMENTATION);
        }
        else {
            generateInterface(aClass);
            generateImplementation(aClass, OwnerKind.IMPLEMENTATION);
            generateImplementation(aClass, OwnerKind.DELEGATING_IMPLEMENTATION);
        }

        for (JetDeclaration declaration : aClass.getDeclarations()) {
            if (declaration instanceof JetClass) {
                generate((JetClass) declaration);
            }
        }
    }

    private void generateInterface(JetClassOrObject aClass) {
        final ClassVisitor visitor = factory.forClassInterface(bindingContext.getClassDescriptor(aClass));
        new InterfaceBodyCodegen(bindingContext, JetStandardLibrary.getJetStandardLibrary(project), aClass, visitor).generate();
    }

    private void generateImplementation(JetClassOrObject aClass, OwnerKind kind) {
        ClassDescriptor descriptor =  bindingContext.getClassDescriptor(aClass);
        ClassVisitor v = kind == OwnerKind.IMPLEMENTATION
                ? factory.forClassImplementation(descriptor)
                : factory.forClassDelegatingImplementation(descriptor);
        new ImplementationBodyCodegen(bindingContext, JetStandardLibrary.getJetStandardLibrary(project), aClass, kind, v).generate();
    }


    public static void newTypeInfo(InstructionAdapter v, Type asmType) {
        v.anew(JetTypeMapper.TYPE_TYPEINFO);
        v.dup();
        v.aconst(asmType);
        v.invokespecial("jet/typeinfo/TypeInfo", "<init>", "(Ljava/lang/Class;)V");
    }
}
