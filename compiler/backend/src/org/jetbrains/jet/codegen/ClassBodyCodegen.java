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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.context.ClassContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.genMethodThrow;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.enumEntryNeedSubclass;

public abstract class ClassBodyCodegen extends MemberCodegen {
    protected final JetClassOrObject myClass;
    protected final OwnerKind kind;
    protected final ClassDescriptor descriptor;
    protected final ClassBuilder v;
    protected final ClassContext context;

    private MethodVisitor clInitMethod;

    private ExpressionCodegen clInitCodegen;

    protected ClassBodyCodegen(
            @NotNull JetClassOrObject aClass,
            @NotNull ClassContext context,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @Nullable MemberCodegen parentCodegen
    ) {
        super(state, parentCodegen);
        descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);
        myClass = aClass;
        this.context = context;
        this.kind = context.getContextKind();
        this.v = v;
    }

    public void generate() {
        generateDeclaration();

        generateClassBody();

        generateSyntheticParts();

        generateStaticInitializer();

        generateRemoveInIterator();

        generateKotlinAnnotation();
    }

    protected abstract void generateDeclaration();

    protected void generateKotlinAnnotation() {
    }

    protected void generateSyntheticParts() {
    }

    private void generateClassBody() {
        FunctionCodegen functionCodegen = new FunctionCodegen(context, v, state);
        PropertyCodegen propertyCodegen = new PropertyCodegen(context, v, functionCodegen, this);

        if (kind != OwnerKind.TRAIT_IMPL) {
            //generate nested classes first and only then generate class body. It necessary to access to nested CodegenContexts
            for (JetDeclaration declaration : myClass.getDeclarations()) {
                if (shouldProcessFirst(declaration)) {
                    generateDeclaration(propertyCodegen, declaration);
                }
            }
        }

        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (!shouldProcessFirst(declaration)) {
                generateDeclaration(propertyCodegen, declaration);
            }
        }

        generatePrimaryConstructorProperties(propertyCodegen, myClass);
    }

    private boolean shouldProcessFirst(JetDeclaration declaration) {
        return false == (declaration instanceof JetProperty || declaration instanceof JetNamedFunction);
    }


    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration) {
        if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
            genFunctionOrProperty(context, (JetTypeParameterListOwner) declaration, v);
        }
        else if (declaration instanceof JetClassOrObject) {
            if (declaration instanceof JetEnumEntry && !enumEntryNeedSubclass(
                    state.getBindingContext(), (JetEnumEntry) declaration)) {
                return;
            }

            genClassOrObject(context, (JetClassOrObject) declaration);
        }
        else if (declaration instanceof JetClassObject) {
            genClassOrObject(context, ((JetClassObject) declaration).getObjectDeclaration());
        }
    }

    private void generatePrimaryConstructorProperties(PropertyCodegen propertyCodegen, JetClassOrObject origin) {
        boolean isAnnotation = origin instanceof JetClass && ((JetClass) origin).isAnnotation();
        for (JetParameter p : getPrimaryConstructorParameters()) {
            if (p.getValOrVarNode() != null) {
                PropertyDescriptor propertyDescriptor = state.getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, p);
                if (propertyDescriptor != null) {
                    if (!isAnnotation) {
                        propertyCodegen.generatePrimaryConstructorProperty(p, propertyDescriptor);
                    }
                    else {
                        propertyCodegen.generateConstructorPropertyAsMethodForAnnotationClass(p, propertyDescriptor);
                    }
                }
            }
        }
    }

    protected @NotNull List<JetParameter> getPrimaryConstructorParameters() {
        if (myClass instanceof JetClass) {
            return ((JetClass) myClass).getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

    private void generateStaticInitializer() {
        if (clInitMethod != null) {
            createOrGetClInitMethod();

            if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
                ExpressionCodegen codegen = createOrGetClInitCodegen();

                createOrGetClInitMethod().visitInsn(RETURN);
                FunctionCodegen.endVisit(codegen.v, "static initializer", myClass);
            }
        }
    }

    @Nullable
    protected MethodVisitor createOrGetClInitMethod() {
        if (clInitMethod == null) {
            clInitMethod = v.newMethod(null, ACC_STATIC, "<clinit>", "()V", null, null);
        }
        return clInitMethod;
    }

    @Nullable
    protected ExpressionCodegen createOrGetClInitCodegen() {
        assert state.getClassBuilderMode() == ClassBuilderMode.FULL;
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            if (clInitCodegen == null) {
                MethodVisitor method = createOrGetClInitMethod();
                method.visitCode();
                SimpleFunctionDescriptorImpl clInit =
                        new SimpleFunctionDescriptorImpl(descriptor, Collections.<AnnotationDescriptor>emptyList(),
                                                         Name.special("<clinit>"),
                                                         CallableMemberDescriptor.Kind.SYNTHESIZED);
                clInit.initialize(null, null, Collections.<TypeParameterDescriptor>emptyList(),
                                  Collections.<ValueParameterDescriptor>emptyList(), null, null, Visibilities.PRIVATE, false);

                clInitCodegen = new ExpressionCodegen(method, new FrameMap(), Type.VOID_TYPE, context.intoFunction(clInit), state);
            }
        }
        return clInitCodegen;
    }

    private void generateRemoveInIterator() {
        // generates stub 'remove' function for subclasses of Iterator to be compatible with java.util.Iterator
        if (isIteratorWithoutRemoveImpl(descriptor)) {
            MethodVisitor mv = v.getVisitor().visitMethod(ACC_PUBLIC, "remove", "()V", null, null);
            genMethodThrow(mv, "java/lang/UnsupportedOperationException", "Mutating method called on a Kotlin Iterator");
        }
    }

    private static boolean isIteratorWithoutRemoveImpl(@NotNull ClassDescriptor classDescriptor) {
        ClassDescriptor iteratorOfT = KotlinBuiltIns.getInstance().getIterator();
        JetType iteratorOfAny =
                TypeUtils.substituteParameters(iteratorOfT, Collections.singletonList(KotlinBuiltIns.getInstance().getAnyType()));
        if (!JetTypeChecker.INSTANCE.isSubtypeOf(classDescriptor.getDefaultType(), iteratorOfAny)) {
            return false;
        }

        for (FunctionDescriptor function : classDescriptor.getDefaultType().getMemberScope().getFunctions(Name.identifier("remove"))) {
            if (function.getValueParameters().isEmpty() && function.getTypeParameters().isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
