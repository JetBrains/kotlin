/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.FunctionsFromAnyGenerator;
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.SimpleType;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.correctElementType;
import static org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive;
import static org.jetbrains.kotlin.codegen.DescriptorAsmUtil.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class FunctionsFromAnyGeneratorImpl extends FunctionsFromAnyGenerator {
    private final ClassDescriptor classDescriptor;
    private final Type classAsmType;
    private final FieldOwnerContext<?> fieldOwnerContext;
    private final ClassBuilder v;
    private final GenerationState generationState;
    private final KotlinTypeMapper typeMapper;
    private final JvmKotlinType underlyingType;
    private final boolean isInErasedInlineClass;

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
        this.underlyingType = new JvmKotlinType(
                typeMapper.mapType(descriptor),
                InlineClassesUtilsKt.substitutedUnderlyingType(descriptor.getDefaultType())
        );
        this.isInErasedInlineClass = fieldOwnerContext.getContextKind() == OwnerKind.ERASED_INLINE_CLASS;
    }

    @Override
    protected void generateToStringMethod(
            @NotNull FunctionDescriptor function,
            @NotNull List<? extends PropertyDescriptor> properties
    ) {
        MethodContext context = fieldOwnerContext.intoFunction(function);
        JvmDeclarationOrigin methodOrigin = JvmDeclarationOriginKt.OtherOrigin(function);
        String toStringMethodName = mapFunctionName(function);
        String toStringMethodDesc = getToStringDesc();
        MethodVisitor mv = v.newMethod(methodOrigin, getAccess(), toStringMethodName, toStringMethodDesc, null, null);

        if (!isInErasedInlineClass && InlineClassesUtilsKt.isInlineClass(classDescriptor)) {
            FunctionCodegen.generateMethodInsideInlineClassWrapper(methodOrigin, function, classDescriptor, mv, typeMapper);
            return;
        }

        if (!isInErasedInlineClass) {
            visitEndForAnnotationVisitor(mv.visitAnnotation(Type.getDescriptor(NotNull.class), false));
        }

        if (!generationState.getClassBuilderMode().generateBodies) {
            FunctionCodegen.endVisit(mv, toStringMethodName, getDeclaration());
            return;
        }

        InstructionAdapter iv = new InstructionAdapter(mv);

        mv.visitCode();

        if (properties.isEmpty()) {
            iv.aconst(classDescriptor.getName().asString());
        } else {
            StringConcatGenerator generator = StringConcatGenerator.Companion.create(generationState, iv);
            generator.genStringBuilderConstructorIfNeded();
            boolean first = true;

            for (PropertyDescriptor propertyDescriptor : properties) {
                if (first) {
                    generator.addStringConstant(classDescriptor.getName() + "(" + propertyDescriptor.getName().asString() + "=");
                    first = false;
                }
                else {
                    generator.addStringConstant(", " + propertyDescriptor.getName().asString() + "=");
                }

                JvmKotlinType type = genOrLoadOnStack(iv, context, propertyDescriptor, 0);
                Type asmType = type.getType();
                KotlinType kotlinType = type.getKotlinType();

                if (asmType.getSort() == Type.ARRAY) {
                    Type elementType = correctElementType(asmType);
                    if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                        iv.invokestatic("java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;", false);
                        asmType = JAVA_STRING_TYPE;
                        kotlinType = DescriptorUtilsKt.getBuiltIns(function).getStringType();
                    }
                    else if (elementType.getSort() != Type.CHAR) {
                        iv.invokestatic("java/util/Arrays", "toString", "(" + asmType.getDescriptor() + ")Ljava/lang/String;", false);
                        asmType = JAVA_STRING_TYPE;
                        kotlinType = DescriptorUtilsKt.getBuiltIns(function).getStringType();
                    }
                }
                genInvokeAppendMethod(generator, asmType, kotlinType, typeMapper, StackValue.onStack(asmType));
            }

            generator.addStringConstant(")");

            generator.genToString();
        }
        iv.areturn(JAVA_STRING_TYPE);

        FunctionCodegen.endVisit(mv, toStringMethodName, getDeclaration());

        recordMethodForFunctionIfRequired(function, toStringMethodName, toStringMethodDesc);
    }

    private void recordMethodForFunctionIfRequired(@NotNull FunctionDescriptor function, @NotNull String name, @NotNull String desc) {
        if (isInErasedInlineClass) {
            v.getSerializationBindings().put(JvmSerializationBindings.METHOD_FOR_FUNCTION, function, new Method(name, desc));
        }
    }

    @Override
    protected void generateHashCodeMethod(
            @NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties
    ) {
        MethodContext context = fieldOwnerContext.intoFunction(function);
        JvmDeclarationOrigin methodOrigin = JvmDeclarationOriginKt.OtherOrigin(function);
        String hashCodeMethodName = mapFunctionName(function);
        String hashCodeMethodDesc = getHashCodeDesc();
        MethodVisitor mv = v.newMethod(methodOrigin, getAccess(), hashCodeMethodName, hashCodeMethodDesc, null, null);

        if (!isInErasedInlineClass && InlineClassesUtilsKt.isInlineClass(classDescriptor)) {
            FunctionCodegen.generateMethodInsideInlineClassWrapper(methodOrigin, function, classDescriptor, mv, typeMapper);
            return;
        }

        if (!generationState.getClassBuilderMode().generateBodies) {
            FunctionCodegen.endVisit(mv, hashCodeMethodName, getDeclaration());
            return;
        }

        InstructionAdapter iv = new InstructionAdapter(mv);

        mv.visitCode();
        if (properties.isEmpty()) {
            iv.iconst(DescriptorUtils.getFqNameSafe(classDescriptor).asString().hashCode());
        } else {
            boolean first = true;
            for (PropertyDescriptor propertyDescriptor : properties) {
                if (!first) {
                    iv.iconst(31);
                    iv.mul(Type.INT_TYPE);
                }

                JvmKotlinType propertyType = genOrLoadOnStack(iv, context, propertyDescriptor, 0);
                KotlinType kotlinType = propertyDescriptor.getReturnType();
                Type asmType = typeMapper.mapType(kotlinType);
                StackValue.coerce(propertyType.getType(), propertyType.getKotlinType(), asmType, kotlinType, iv);

                Label ifNull = null;
                if (!isPrimitive(asmType)) {
                    ifNull = new Label();
                    iv.dup();
                    iv.ifnull(ifNull);
                }

                genHashCode(mv, iv, asmType, generationState.getConfig().getTarget());

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
        }

        mv.visitInsn(IRETURN);

        FunctionCodegen.endVisit(mv, hashCodeMethodName, getDeclaration());

        recordMethodForFunctionIfRequired(function, hashCodeMethodName, hashCodeMethodDesc);
    }

    private String mapFunctionName(@NotNull FunctionDescriptor functionDescriptor) {
        return typeMapper.mapFunctionName(functionDescriptor, fieldOwnerContext.getContextKind());
    }

    @Override
    protected void generateEqualsMethod(
            @NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> properties
    ) {
        MethodContext context = fieldOwnerContext.intoFunction(function);
        JvmDeclarationOrigin methodOrigin = JvmDeclarationOriginKt.OtherOrigin(function);
        String equalsMethodName = mapFunctionName(function);
        String equalsMethodDesc = getEqualsDesc();
        MethodVisitor mv = v.newMethod(methodOrigin, getAccess(), equalsMethodName, equalsMethodDesc, null, null);

        if (!isInErasedInlineClass && InlineClassesUtilsKt.isInlineClass(classDescriptor)) {
            FunctionCodegen.generateMethodInsideInlineClassWrapper(methodOrigin, function, classDescriptor, mv, typeMapper);
            return;
        }

        if (!isInErasedInlineClass) {
            visitEndForAnnotationVisitor(mv.visitParameterAnnotation(0, Type.getDescriptor(Nullable.class), false));
        }

        if (!generationState.getClassBuilderMode().generateBodies) {
            FunctionCodegen.endVisit(mv, equalsMethodName, getDeclaration());
            return;
        }

        InstructionAdapter iv = new InstructionAdapter(mv);

        mv.visitCode();
        Label eq = new Label();
        Label ne = new Label();

        int storedValueIndex = generateBasicChecksAndStoreTarget(iv, eq, ne);

        for (PropertyDescriptor propertyDescriptor : properties) {
            KotlinType kotlinType = propertyDescriptor.getReturnType();
            Type asmType = typeMapper.mapType(kotlinType);

            StackValue thisPropertyValue = StackValue.operation(asmType, kotlinType, (InstructionAdapter iiv) -> {
                JvmKotlinType thisPropertyType = genOrLoadOnStack(iiv, context, propertyDescriptor, 0);
                StackValue.coerce(thisPropertyType.getType(), thisPropertyType.getKotlinType(), asmType, kotlinType, iiv);
                return Unit.INSTANCE;
            });
            StackValue otherPropertyValue = StackValue.operation(asmType, kotlinType, (InstructionAdapter iiv) -> {
                JvmKotlinType otherPropertyType = genOrLoadOnStack(iiv, context, propertyDescriptor, storedValueIndex);
                StackValue.coerce(otherPropertyType.getType(), otherPropertyType.getKotlinType(), asmType, kotlinType, iiv);
                return Unit.INSTANCE;
            });

            genTotalOrderEqualsForExpressionOnStack(thisPropertyValue, otherPropertyValue, asmType).condJump(ne, iv, true);
        }

        iv.mark(eq);
        iv.iconst(1);
        iv.areturn(Type.INT_TYPE);

        iv.mark(ne);
        iv.iconst(0);
        iv.areturn(Type.INT_TYPE);

        FunctionCodegen.endVisit(mv, equalsMethodName, getDeclaration());

        recordMethodForFunctionIfRequired(function, equalsMethodName, equalsMethodDesc);
    }

    private static void visitEndForAnnotationVisitor(@Nullable AnnotationVisitor annotation) {
        if (annotation != null) {
            annotation.visitEnd();
        }
    }

    private int generateBasicChecksAndStoreTarget(InstructionAdapter iv, Label eq, Label ne) {
        if (fieldOwnerContext.getContextKind() == OwnerKind.ERASED_INLINE_CLASS) {
            SimpleType wrapperKotlinType = classDescriptor.getDefaultType();
            Type wrapperType = typeMapper.mapTypeAsDeclaration(wrapperKotlinType);
            int secondParameterIndex = underlyingType.getType().getSize();

            iv.load(secondParameterIndex, OBJECT_TYPE);
            iv.instanceOf(wrapperType);
            iv.ifeq(ne);

            int unboxedValueIndex = secondParameterIndex + 1;

            iv.load(secondParameterIndex, OBJECT_TYPE);
            iv.checkcast(wrapperType);
            StackValue.unboxInlineClass(wrapperType, wrapperKotlinType, iv, typeMapper);
            iv.store(unboxedValueIndex, underlyingType.getType());

            return unboxedValueIndex;
        }
        else {
            iv.load(0, OBJECT_TYPE);
            iv.load(1, OBJECT_TYPE);
            iv.ifacmpeq(eq);

            iv.load(1, OBJECT_TYPE);
            iv.instanceOf(classAsmType);
            iv.ifeq(ne);

            iv.load(1, OBJECT_TYPE);
            iv.checkcast(classAsmType);
            iv.store(2, OBJECT_TYPE);

            return 2;
        }
    }

    private JvmKotlinType genOrLoadOnStack(InstructionAdapter iv, MethodContext context, PropertyDescriptor propertyDescriptor, int index) {
        if (fieldOwnerContext.getContextKind() == OwnerKind.ERASED_INLINE_CLASS) {
            iv.load(index, underlyingType.getType());
            return underlyingType;
        }
        else {
            return ImplementationBodyCodegen.genPropertyOnStack(
                    iv, context, propertyDescriptor, classAsmType, index, generationState
            );
        }
    }

    private String getToStringDesc() {
        return "(" + getFirstParameterDesc() + ")Ljava/lang/String;";
    }

    private String getHashCodeDesc() {
        return "(" + getFirstParameterDesc() + ")I";
    }

    private String getEqualsDesc() {
        return "(" + getFirstParameterDesc() + "Ljava/lang/Object;)Z";
    }

    private String getFirstParameterDesc() {
        return fieldOwnerContext.getContextKind() == OwnerKind.ERASED_INLINE_CLASS ? underlyingType.getType().getDescriptor() : "";
    }

    private int getAccess() {
        int access = ACC_PUBLIC;
        if (fieldOwnerContext.getContextKind() == OwnerKind.ERASED_INLINE_CLASS) {
            access |= ACC_STATIC;
        }

        return access;
    }
}
