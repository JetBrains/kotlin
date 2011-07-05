package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * @author max
 */
public class ClassCodegen {
    private final GenerationState state;

    public ClassCodegen(GenerationState state) {
        this.state = state;
    }

    public void generate(ClassContext parentContext, JetClassOrObject aClass) {
        state.prepareAnonymousClasses((JetElement) aClass);

        if (aClass instanceof JetObjectDeclaration) {
            generateImplementation(parentContext, aClass, OwnerKind.IMPLEMENTATION);
        }
        else {
            generateInterface(parentContext, aClass);
            generateImplementation(parentContext, aClass, OwnerKind.IMPLEMENTATION);
            generateImplementation(parentContext, aClass, OwnerKind.DELEGATING_IMPLEMENTATION);
        }

        ClassDescriptor descriptor =  state.getBindingContext().getClassDescriptor(aClass);
        final ClassContext contextForInners = parentContext.intoClass(descriptor, OwnerKind.IMPLEMENTATION);
        for (JetDeclaration declaration : aClass.getDeclarations()) {
            if (declaration instanceof JetClass) {
                generate(contextForInners, (JetClass) declaration);
            }
        }
    }

    private void generateInterface(ClassContext parentContext, JetClassOrObject aClass) {
        ClassDescriptor descriptor =  state.getBindingContext().getClassDescriptor(aClass);
        final ClassVisitor visitor = state.forClassInterface(descriptor);
        new InterfaceBodyCodegen(aClass, parentContext.intoClass(descriptor, OwnerKind.INTERFACE), visitor, state).generate();
    }

    private void generateImplementation(ClassContext parentContext, JetClassOrObject aClass, OwnerKind kind) {
        ClassDescriptor descriptor =  state.getBindingContext().getClassDescriptor(aClass);
        ClassVisitor v = kind == OwnerKind.IMPLEMENTATION
                ? state.forClassImplementation(descriptor)
                : state.forClassDelegatingImplementation(descriptor);
        new ImplementationBodyCodegen(aClass, parentContext.intoClass(descriptor, kind), v, state).generate();
    }


    public static void newTypeInfo(InstructionAdapter v, boolean isNullable, Type asmType) {
        v.anew(JetTypeMapper.TYPE_TYPEINFO);
        v.dup();
        v.aconst(asmType);
        v.aconst(isNullable);
        v.invokespecial("jet/typeinfo/TypeInfo", "<init>", "(Ljava/lang/Class;Z)V");
    }
}
