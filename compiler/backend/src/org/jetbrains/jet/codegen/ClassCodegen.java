/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);
        ClassBuilder classBuilder = state.forClassImplementation(descriptor);
        if (classBuilder.generateCode()) {
            GenerationState.prepareAnonymousClasses(aClass, state.getTypeMapper());
        }

        final CodegenContext contextForInners = context.intoClass(descriptor, OwnerKind.IMPLEMENTATION, state.getTypeMapper());

        if (!classBuilder.generateCode()) {
            // Outer class implementation must happen prior inner classes so we get proper scoping tree in JetLightClass's delegate
            generateImplementation(context, aClass, OwnerKind.IMPLEMENTATION, contextForInners.accessors, classBuilder);
        }
        
        for (JetDeclaration declaration : aClass.getDeclarations()) {
            if (declaration instanceof JetClass && !(declaration instanceof JetEnumEntry)) {
                generate(contextForInners, (JetClass) declaration);
            }
            if(declaration instanceof JetClassObject) {
                state.forClass().generate(contextForInners, ((JetClassObject)declaration).getObjectDeclaration());
            }
        }

        if (classBuilder.generateCode()) {
            generateImplementation(context, aClass, OwnerKind.IMPLEMENTATION, contextForInners.accessors, classBuilder);
        }
        
        classBuilder.done();
    }

    private void generateImplementation(CodegenContext context, JetClassOrObject aClass, OwnerKind kind, HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors, ClassBuilder classBuilder) {
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);
        CodegenContext classContext = context.intoClass(descriptor, kind, state.getTypeMapper());
        classContext.copyAccessors(accessors);
        new ImplementationBodyCodegen(aClass, classContext, classBuilder, state).generate();

        if(aClass instanceof JetClass && ((JetClass)aClass).isTrait()) {
            ClassBuilder traitBuilder = state.forTraitImplementation(descriptor);
            new TraitImplBodyCodegen(aClass, context.intoClass(descriptor, OwnerKind.TRAIT_IMPL, state.getTypeMapper()), traitBuilder, state).generate();
            traitBuilder.done();
        }
    }
}
