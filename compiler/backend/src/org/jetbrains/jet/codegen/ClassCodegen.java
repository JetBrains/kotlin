package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.HashMap;

/**
 * @author max
 * @author alex.tkachman
 */
public class ClassCodegen {
    private final GenerationState state;

    public ClassCodegen(GenerationState state) {
        this.state = state;
    }

    public void generate(CodegenContext context, JetClassOrObject aClass) {
        GenerationState.prepareAnonymousClasses((JetElement) aClass, state.getTypeMapper());

        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);

        final CodegenContext contextForInners = context.intoClass(descriptor, OwnerKind.IMPLEMENTATION);
        for (JetDeclaration declaration : aClass.getDeclarations()) {
            if (declaration instanceof JetClass && !(declaration instanceof JetEnumEntry)) {
                generate(contextForInners, (JetClass) declaration);
            }
        }

        generateImplementation(context, aClass, OwnerKind.IMPLEMENTATION, contextForInners.accessors);
    }

    private void generateImplementation(CodegenContext context, JetClassOrObject aClass, OwnerKind kind, HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors) {
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);
        ClassBuilder v = state.forClassImplementation(descriptor);
        CodegenContext classContext = context.intoClass(descriptor, kind);
        new ImplementationBodyCodegen(aClass, classContext, v, state).generate(accessors);

        if(aClass instanceof JetClass && ((JetClass)aClass).isTrait()) {
            v = state.forTraitImplementation(descriptor);
            new TraitImplBodyCodegen(aClass, context.intoClass(descriptor, OwnerKind.TRAIT_IMPL), v, state).generate(null);
        }
    }
}
