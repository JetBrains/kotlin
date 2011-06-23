package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
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
        final ClassVisitor visitor = state.forClassInterface(state.getBindingContext().getClassDescriptor(aClass));
        new InterfaceBodyCodegen(aClass, visitor, state).generate();
    }

    private void generateImplementation(JetClassOrObject aClass, OwnerKind kind) {
        ClassDescriptor descriptor =  state.getBindingContext().getClassDescriptor(aClass);
        ClassVisitor v = kind == OwnerKind.IMPLEMENTATION
                ? state.forClassImplementation(descriptor)
                : state.forClassDelegatingImplementation(descriptor);
        new ImplementationBodyCodegen(aClass, kind, v, state).generate();
    }


    public static void newTypeInfo(InstructionAdapter v, Type asmType) {
        v.anew(JetTypeMapper.TYPE_TYPEINFO);
        v.dup();
        v.aconst(asmType);
        v.invokespecial("jet/typeinfo/TypeInfo", "<init>", "(Ljava/lang/Class;)V");
    }
}
