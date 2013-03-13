/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ErrorUtils;

import java.util.Map;

import static org.jetbrains.jet.codegen.binding.CodegenBinding.enumEntryNeedSubclass;

public class MemberCodegen extends GenerationStateAware {
    public MemberCodegen(@NotNull GenerationState state) {
        super(state);
    }

    public void genFunctionOrProperty(
            CodegenContext context,
            @NotNull JetTypeParameterListOwner functionOrProperty,
            @NotNull ClassBuilder classBuilder
    ) {
        FunctionCodegen functionCodegen = new FunctionCodegen(context, classBuilder, state);
        if (functionOrProperty instanceof JetNamedFunction) {
            try {
                functionCodegen.gen((JetNamedFunction) functionOrProperty);
            }
            catch (CompilationException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CompilationException("Failed to generate function " + functionOrProperty.getName(), e, functionOrProperty);
            }
        }
        else if (functionOrProperty instanceof JetProperty) {
            try {
                new PropertyCodegen(context, classBuilder, functionCodegen).gen((JetProperty) functionOrProperty);
            }
            catch (CompilationException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CompilationException("Failed to generate property " + functionOrProperty.getName(), e, functionOrProperty);
            }
        }
        else {
            throw new IllegalArgumentException("Unknown parameter: " + functionOrProperty);
        }
    }

    public static void genImplementation(
            CodegenContext context,
            GenerationState state,
            JetClassOrObject aClass,
            OwnerKind kind,
            Map<DeclarationDescriptor, DeclarationDescriptor> accessors,
            ClassBuilder classBuilder
    ) {
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);
        CodegenContext classContext = context.intoClass(descriptor, kind, state);
        classContext.copyAccessors(accessors);

        new ImplementationBodyCodegen(aClass, classContext, classBuilder, state).generate();

        if (aClass instanceof JetClass && ((JetClass) aClass).isTrait()) {
            ClassBuilder traitBuilder = state.getFactory().forTraitImplementation(descriptor, state, aClass.getContainingFile());
            new TraitImplBodyCodegen(aClass, context.intoClass(descriptor, OwnerKind.TRAIT_IMPL, state), traitBuilder, state)
                    .generate();
            traitBuilder.done();
        }
    }

    public void genInners(CodegenContext context, GenerationState state, JetClassOrObject aClass) {
        for (JetDeclaration declaration : aClass.getDeclarations()) {
            if (declaration instanceof JetClass) {
                if (declaration instanceof JetEnumEntry && !enumEntryNeedSubclass(
                        state.getBindingContext(), (JetEnumEntry) declaration)) {
                    continue;
                }

                genClassOrObject(context, (JetClass) declaration);
            }
            else if (declaration instanceof JetClassObject) {
                genClassOrObject(context, ((JetClassObject) declaration).getObjectDeclaration());
            }
            else if (declaration instanceof JetObjectDeclaration) {
                genClassOrObject(context, (JetObjectDeclaration) declaration);
            }
        }
    }

    public void genClassOrObject(CodegenContext context, JetClassOrObject aClass) {
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);

        if (descriptor == null || ErrorUtils.isError(descriptor) || descriptor.getName().equals(JetPsiUtil.NO_NAME_PROVIDED)) {
            if (state.getClassBuilderMode() != ClassBuilderMode.SIGNATURES) {
                throw new IllegalStateException(
                        "Generating bad descriptor in ClassBuilderMode = " + state.getClassBuilderMode() + ": " + descriptor);
            }
            return;
        }

        ClassBuilder classBuilder = state.getFactory().forClassImplementation(descriptor, aClass.getContainingFile());

        CodegenContext contextForInners = context.intoClass(descriptor, OwnerKind.IMPLEMENTATION, state);

        if (state.getClassBuilderMode() == ClassBuilderMode.SIGNATURES) {
            // Outer class implementation must happen prior inner classes so we get proper scoping tree in JetLightClass's delegate
            // The same code is present below for the case when we genClassOrObject real bytecode. This is because the order should be
            // different for the case when we compute closures
            genImplementation(context, state, aClass, OwnerKind.IMPLEMENTATION, contextForInners.getAccessors(), classBuilder);
        }

        genInners(contextForInners, state, aClass);

        if (state.getClassBuilderMode() != ClassBuilderMode.SIGNATURES) {
            genImplementation(context, state, aClass, OwnerKind.IMPLEMENTATION, contextForInners.getAccessors(), classBuilder);
        }

        classBuilder.done();
    }
}
