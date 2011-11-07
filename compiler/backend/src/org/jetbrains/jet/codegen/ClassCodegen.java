package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
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

    public void generate(ClassContext context, JetClassOrObject aClass) {
        GenerationState.prepareAnonymousClasses((JetElement) aClass, state.getTypeMapper());

        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);

        final ClassContext contextForInners = context.intoClass(null, descriptor, OwnerKind.IMPLEMENTATION);
        for (JetDeclaration declaration : aClass.getDeclarations()) {
            if (declaration instanceof JetClass && !(declaration instanceof JetEnumEntry)) {
                generate(contextForInners, (JetClass) declaration);
            }
        }

        generateImplementation(context, aClass, OwnerKind.IMPLEMENTATION, contextForInners.accessors);
    }

    private void generateImplementation(ClassContext context, JetClassOrObject aClass, OwnerKind kind, HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors) {
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);
        ClassBuilder v = state.forClassImplementation(descriptor);
        ClassContext classContext = context.intoClass(null, descriptor, kind);
        new ImplementationBodyCodegen(aClass, classContext, v, state).generate(accessors);

        if(aClass instanceof JetClass && ((JetClass)aClass).isTrait()) {
            v = state.forTraitImplementation(descriptor);
            new TraitImplBodyCodegen(aClass, context.intoClass(null, descriptor, OwnerKind.TRAIT_IMPL), v, state).generate(null);
        }
    }


}
