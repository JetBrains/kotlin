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

import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.context.ClassContext;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.inline.InlineCodegenUtil;
import org.jetbrains.jet.codegen.inline.NameGenerator;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.SpecialNames;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.Collections;

import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED;
import static org.jetbrains.org.objectweb.asm.Opcodes.ACC_STATIC;

public class MemberCodegen extends ParentCodegenAwareImpl {
    protected final FieldOwnerContext context;
    protected final ClassBuilder v;
    protected ExpressionCodegen clInit;

    private NameGenerator inlineNameGenerator;

    public MemberCodegen(
            @NotNull GenerationState state,
            @Nullable MemberCodegen parentCodegen,
            @NotNull FieldOwnerContext context,
            ClassBuilder builder
    ) {
        super(state, parentCodegen);
        this.context = context;
        this.v = builder;
    }

    public void genFunctionOrProperty(
            @NotNull JetTypeParameterListOwner functionOrProperty,
            @NotNull ClassBuilder classBuilder
    ) {
        FunctionCodegen functionCodegen = new FunctionCodegen(context, classBuilder, state, this);
        if (functionOrProperty instanceof JetNamedFunction) {
            try {
                functionCodegen.gen((JetNamedFunction) functionOrProperty);
            }
            catch (ProcessCanceledException e) {
                throw e;
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
                new PropertyCodegen(context, classBuilder, functionCodegen, this).gen((JetProperty) functionOrProperty);
            }
            catch (ProcessCanceledException e) {
                throw e;
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

    public static void genClassOrObject(
            @NotNull CodegenContext parentContext,
            @NotNull JetClassOrObject aClass,
            @NotNull GenerationState state,
            @Nullable MemberCodegen parentCodegen
    ) {
        ClassDescriptor descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);

        if (descriptor == null || ErrorUtils.isError(descriptor)) {
            badDescriptor(descriptor, state.getClassBuilderMode());
            return;
        }

        if (descriptor.getName().equals(SpecialNames.NO_NAME_PROVIDED)) {
            badDescriptor(descriptor, state.getClassBuilderMode());
        }

        ClassBuilder classBuilder = state.getFactory().forClassImplementation(descriptor, aClass.getContainingFile());
        ClassContext classContext = parentContext.intoClass(descriptor, OwnerKind.IMPLEMENTATION, state);
        new ImplementationBodyCodegen(aClass, classContext, classBuilder, state, parentCodegen).generate();
        classBuilder.done();

        if (aClass instanceof JetClass && ((JetClass) aClass).isTrait()) {
            ClassBuilder traitBuilder = state.getFactory().forTraitImplementation(descriptor, state, aClass.getContainingFile());
            new TraitImplBodyCodegen(aClass, parentContext.intoClass(descriptor, OwnerKind.TRAIT_IMPL, state), traitBuilder, state, parentCodegen)
                    .generate();
            traitBuilder.done();
        }
    }

    private static void badDescriptor(ClassDescriptor descriptor, ClassBuilderMode mode) {
        if (mode != ClassBuilderMode.LIGHT_CLASSES) {
            throw new IllegalStateException("Generating bad descriptor in ClassBuilderMode = " + mode + ": " + descriptor);
        }
    }

    public void genClassOrObject(JetClassOrObject aClass) {
        genClassOrObject(context, aClass, state, this);
    }

    @NotNull
    public ClassBuilder getBuilder() {
        return v;
    }

    @NotNull
    public NameGenerator getInlineNameGenerator() {
        if (inlineNameGenerator == null) {
            String prefix = InlineCodegenUtil.getInlineName(context, typeMapper);
            inlineNameGenerator = new NameGenerator(prefix);
        }
        return inlineNameGenerator;
    }

    @NotNull
    protected ExpressionCodegen createOrGetClInitCodegen() {
        DeclarationDescriptor descriptor = context.getContextDescriptor();
        assert state.getClassBuilderMode() == ClassBuilderMode.FULL
                : "<clinit> should not be generated for light classes. Descriptor: " + descriptor;
        if (clInit == null) {
            MethodVisitor mv = v.newMethod(null, ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            SimpleFunctionDescriptorImpl clInit =
                    SimpleFunctionDescriptorImpl.create(descriptor, Annotations.EMPTY, Name.special("<clinit>"), SYNTHESIZED);
            clInit.initialize(null, null, Collections.<TypeParameterDescriptor>emptyList(),
                              Collections.<ValueParameterDescriptor>emptyList(), null, null, Visibilities.PRIVATE);

            this.clInit = new ExpressionCodegen(mv, new FrameMap(), Type.VOID_TYPE, context.intoFunction(clInit), state, this);
        }
        return clInit;
    }
}
