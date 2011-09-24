package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * @author max
 * @author alex.tkachman
 */
public class ClassCodegen {
    private final GenerationState state;

    public ClassCodegen(GenerationState state) {
        this.state = state;
    }

    public void generate(ClassContext parentContext, JetClassOrObject aClass) {
        GenerationState.prepareAnonymousClasses((JetElement) aClass, state.getTypeMapper());

        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);

        generateImplementation(parentContext, aClass, OwnerKind.IMPLEMENTATION);

        final ClassContext contextForInners = parentContext.intoClass(descriptor, OwnerKind.IMPLEMENTATION);
        for (JetDeclaration declaration : aClass.getDeclarations()) {
            if (declaration instanceof JetClass && !(declaration instanceof JetEnumEntry)) {
                generate(contextForInners, (JetClass) declaration);
            }
        }
    }

    private void generateImplementation(ClassContext parentContext, JetClassOrObject aClass, OwnerKind kind) {
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);
        ClassVisitor v = state.forClassImplementation(descriptor);
        new ImplementationBodyCodegen(aClass, parentContext.intoClass(descriptor, kind), v, state).generate();
        
        if(aClass instanceof JetClass && ((JetClass)aClass).isTrait()) {
            v = state.forTraitImplementation(descriptor);
            new TraitImplBodyCodegen(aClass, parentContext.intoClass(descriptor, OwnerKind.TRAIT_IMPL), v, state).generate();
        }
    }


    public static void newTypeInfo(InstructionAdapter v, boolean isNullable, Type asmType) {
        v.aconst(asmType);
        v.iconst(isNullable?1:0);
        v.invokestatic("jet/typeinfo/TypeInfo", "getTypeInfo", "(Ljava/lang/Class;Z)Ljet/typeinfo/TypeInfo;");
    }
}
