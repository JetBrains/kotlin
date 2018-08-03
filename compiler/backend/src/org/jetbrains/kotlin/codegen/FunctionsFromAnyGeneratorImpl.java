/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.FunctionsFromAnyGenerator;
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.jetbrains.org.objectweb.asm.Opcodes.IRETURN;

public class FunctionsFromAnyGeneratorImpl extends FunctionsFromAnyGenerator {
    private final ClassDescriptor classDescriptor;
    private final Type classAsmType;
    private final FieldOwnerContext<?> fieldOwnerContext;
    private final ClassBuilder v;
    private final GenerationState generationState;
    private final KotlinTypeMapper typeMapper;

    public FunctionsFromAnyGeneratorImpl(
            @NotNull KtClassOrObject declaration,
            @NotNull BindingContext bindingContext,
            @NotNull ClassDescriptor descriptor,
            @NotNull Type type,
            @NotNull FieldOwnerContext<?> fieldOwnerContext,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state
    ) {
        super(declaration, bindingContext);
        this.classDescriptor = descriptor;
        this.classAsmType = type;
        this.fieldOwnerContext = fieldOwnerContext;
        this.v = v;
        this.generationState = state;
        this.typeMapper = state.getTypeMapper();
    }

    @Override
    protected void generateToStringMethod(
            @NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties
    ) {
        MethodContext context = fieldOwnerContext.intoFunction(function);
        MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(function), ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        mv.visitAnnotation(Type.getDescriptor(NotNull.class), false);
        InstructionAdapter iv = new InstructionAdapter(mv);

        mv.visitCode();
        genStringBuilderConstructor(iv);

        boolean first = true;
        for (PropertyDescriptor propertyDescriptor : properties) {
            if (first) {
                iv.aconst(classDescriptor.getName() + "(" + propertyDescriptor.getName().asString()+"=");
                first = false;
            }
            else {
                iv.aconst(", " + propertyDescriptor.getName().asString() + "=");
            }
            genInvokeAppendMethod(iv, JAVA_STRING_TYPE, null);

            JvmKotlinType type = ImplementationBodyCodegen.genPropertyOnStack(
                    iv, context, propertyDescriptor, classAsmType, 0, generationState
            );
            Type asmType = type.getType();

            if (asmType.getSort() == Type.ARRAY) {
                Type elementType = correctElementType(asmType);
                if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                    iv.invokestatic("java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;", false);
                    asmType = JAVA_STRING_TYPE;
                }
                else {
                    if (elementType.getSort() != Type.CHAR) {
                        iv.invokestatic("java/util/Arrays", "toString", "(" + asmType.getDescriptor() + ")Ljava/lang/String;", false);
                        asmType = JAVA_STRING_TYPE;
                    }
                }
            }
            genInvokeAppendMethod(iv, asmType, type.getKotlinType());
        }

        iv.aconst(")");
        genInvokeAppendMethod(iv, JAVA_STRING_TYPE, null);

        iv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        iv.areturn(JAVA_STRING_TYPE);

        FunctionCodegen.endVisit(mv, "toString", getDeclaration());
    }

    @Override
    protected void generateHashCodeMethod(
            @NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties
    ) {
        MethodContext context = fieldOwnerContext.intoFunction(function);
        MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(function), ACC_PUBLIC, "hashCode", "()I", null, null);
        InstructionAdapter iv = new InstructionAdapter(mv);

        mv.visitCode();
        boolean first = true;
        for (PropertyDescriptor propertyDescriptor : properties) {
            if (!first) {
                iv.iconst(31);
                iv.mul(Type.INT_TYPE);
            }

            JvmKotlinType propertyType = ImplementationBodyCodegen.genPropertyOnStack(
                    iv, context, propertyDescriptor, classAsmType, 0, generationState
            );
            KotlinType kotlinType = propertyDescriptor.getReturnType();
            Type asmType = typeMapper.mapType(kotlinType);
            StackValue.coerce(propertyType.getType(), propertyType.getKotlinType(), asmType, kotlinType, iv);

            Label ifNull = null;
            if (!isPrimitive(asmType)) {
                ifNull = new Label();
                iv.dup();
                iv.ifnull(ifNull);
            }

            genHashCode(mv, iv, asmType, generationState.getTarget());

            if (ifNull != null) {
                Label end = new Label();
                iv.goTo(end);
                iv.mark(ifNull);
                iv.pop();
                iv.iconst(0);
                iv.mark(end);
            }

            if (first) {
                first = false;
            }
            else {
                iv.add(Type.INT_TYPE);
            }
        }

        mv.visitInsn(IRETURN);

        FunctionCodegen.endVisit(mv, "hashCode", getDeclaration());

    }

    @Override
    protected void generateEqualsMethod(
            @NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties
    ) {
        MethodContext context = fieldOwnerContext.intoFunction(function);
        MethodVisitor
                mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(function), ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
        mv.visitParameterAnnotation(0, Type.getDescriptor(Nullable.class), false);
        InstructionAdapter iv = new InstructionAdapter(mv);

        mv.visitCode();
        Label eq = new Label();
        Label ne = new Label();

        iv.load(0, OBJECT_TYPE);
        iv.load(1, OBJECT_TYPE);
        iv.ifacmpeq(eq);

        iv.load(1, OBJECT_TYPE);
        iv.instanceOf(classAsmType);
        iv.ifeq(ne);

        iv.load(1, OBJECT_TYPE);
        iv.checkcast(classAsmType);
        iv.store(2, OBJECT_TYPE);

        for (PropertyDescriptor propertyDescriptor : properties) {
            KotlinType kotlinType = propertyDescriptor.getReturnType();
            Type asmType = typeMapper.mapType(kotlinType);

            JvmKotlinType thisPropertyType = ImplementationBodyCodegen.genPropertyOnStack(
                    iv, context, propertyDescriptor, classAsmType, 0, generationState
            );
            StackValue.coerce(thisPropertyType.getType(), thisPropertyType.getKotlinType(), asmType, kotlinType, iv);

            JvmKotlinType otherPropertyType = ImplementationBodyCodegen.genPropertyOnStack(
                    iv, context, propertyDescriptor, classAsmType, 2, generationState
            );
            StackValue.coerce(otherPropertyType.getType(), otherPropertyType.getKotlinType(), asmType, kotlinType, iv);

            if (asmType.getSort() == Type.FLOAT) {
                iv.invokestatic("java/lang/Float", "compare", "(FF)I", false);
                iv.ifne(ne);
            }
            else if (asmType.getSort() == Type.DOUBLE) {
                iv.invokestatic("java/lang/Double", "compare", "(DD)I", false);
                iv.ifne(ne);
            }
            else {
                StackValue value = genEqualsForExpressionsOnStack(
                        KtTokens.EQEQ, StackValue.onStack(asmType, kotlinType), StackValue.onStack(asmType, kotlinType)
                );
                value.put(Type.BOOLEAN_TYPE, iv);
                iv.ifeq(ne);
            }
        }

        iv.mark(eq);
        iv.iconst(1);
        iv.areturn(Type.INT_TYPE);

        iv.mark(ne);
        iv.iconst(0);
        iv.areturn(Type.INT_TYPE);

        FunctionCodegen.endVisit(mv, "equals", getDeclaration());
    }
}
