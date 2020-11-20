/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.Visibilities;
import org.jetbrains.kotlin.descriptors.Visibility;
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.Map;

import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class AsmUtil {
    private static final int NO_FLAG_LOCAL = 0;
    public static final int NO_FLAG_PACKAGE_PRIVATE = 0;

    @NotNull
    private static final Map<Visibility, Integer> visibilityToAccessFlag = ImmutableMap.<Visibility, Integer>builder()
            .put(Visibilities.Private.INSTANCE, ACC_PRIVATE)
            .put(Visibilities.PrivateToThis.INSTANCE, ACC_PRIVATE)
            .put(Visibilities.Protected.INSTANCE, ACC_PROTECTED)
            .put(JavaVisibilities.ProtectedStaticVisibility.INSTANCE, ACC_PROTECTED)
            .put(JavaVisibilities.ProtectedAndPackage.INSTANCE, ACC_PROTECTED)
            .put(Visibilities.Public.INSTANCE, ACC_PUBLIC)
            .put(Visibilities.Internal.INSTANCE, ACC_PUBLIC)
            .put(Visibilities.Local.INSTANCE, NO_FLAG_LOCAL)
            .put(JavaVisibilities.PackageVisibility.INSTANCE, NO_FLAG_PACKAGE_PRIVATE)
            .build();

    public static final String CAPTURED_PREFIX = "$";

    public static final String THIS = "this";

    public static final String THIS_IN_DEFAULT_IMPLS = "$this";

    public static final String LABELED_THIS_FIELD = THIS + "_";

    public static final String CAPTURED_LABELED_THIS_FIELD = CAPTURED_PREFIX + LABELED_THIS_FIELD;

    public static final String INLINE_DECLARATION_SITE_THIS = "this_";

    public static final String LABELED_THIS_PARAMETER = CAPTURED_PREFIX + THIS + "$";

    public static final String CAPTURED_THIS_FIELD = "this$0";

    public static final String RECEIVER_PARAMETER_NAME = "$receiver";

    /*
        This is basically an old convention. Starting from Kotlin 1.3, it was replaced with `$this_<label>`.
        Note that it is still used for inlined callable references and anonymous callable extension receivers
        even in 1.3.
    */
    public static final String CAPTURED_RECEIVER_FIELD = "receiver$0";

    // For non-inlined callable references ('kotlin.jvm.internal.CallableReference' has a 'receiver' field)
    public static final String BOUND_REFERENCE_RECEIVER = "receiver";

    public static final String LOCAL_FUNCTION_VARIABLE_PREFIX = "$fun$";

    private static final ImmutableMap<Integer, JvmPrimitiveType> primitiveTypeByAsmSort;
    private static final ImmutableMap<Type, Type> primitiveTypeByBoxedType;

    static {
        ImmutableMap.Builder<Integer, JvmPrimitiveType> typeBySortBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Type, Type> typeByWrapperBuilder = ImmutableMap.builder();
        for (JvmPrimitiveType primitiveType : JvmPrimitiveType.values()) {
            Type asmType = Type.getType(primitiveType.getDesc());
            typeBySortBuilder.put(asmType.getSort(), primitiveType);
            typeByWrapperBuilder.put(asmTypeByFqNameWithoutInnerClasses(primitiveType.getWrapperFqName()), asmType);
        }
        primitiveTypeByAsmSort = typeBySortBuilder.build();
        primitiveTypeByBoxedType = typeByWrapperBuilder.build();
    }

    private AsmUtil() {
    }

    @NotNull
    public static String getCapturedFieldName(@NotNull String originalName) {
        return CAPTURED_PREFIX + originalName;
    }

    @NotNull
    public static String getLabeledThisName(@NotNull String callableName, @NotNull String prefix, @NotNull String defaultName) {
        if (!Name.isValidIdentifier(callableName)) {
            return defaultName;
        }

        return prefix + CommonVariableAsmNameManglingUtils.mangleNameIfNeeded(callableName);
    }

    @NotNull
    public static Type boxType(@NotNull Type type) {
        Type boxedType = boxPrimitiveType(type);
        return boxedType != null ? boxedType : type;
    }

    @Nullable
    public static Type boxPrimitiveType(@NotNull Type type) {
        JvmPrimitiveType jvmPrimitiveType = primitiveTypeByAsmSort.get(type.getSort());
        return jvmPrimitiveType != null ? asmTypeByFqNameWithoutInnerClasses(jvmPrimitiveType.getWrapperFqName()) : null;
    }

    @NotNull
    public static Type unboxType(@NotNull Type boxedType) {
        Type primitiveType = unboxPrimitiveTypeOrNull(boxedType);
        if (primitiveType == null) {
            throw new UnsupportedOperationException("Unboxing: " + boxedType);
        }
        return primitiveType;
    }

    @Nullable
    public static Type unboxPrimitiveTypeOrNull(@NotNull Type boxedType) {
        return primitiveTypeByBoxedType.get(boxedType);
    }

    public static boolean isBoxedPrimitiveType(@NotNull Type boxedType) {
        return primitiveTypeByBoxedType.get(boxedType) != null;
    }

    @NotNull
    public static Type unboxUnlessPrimitive(@NotNull Type boxedOrPrimitiveType) {
        if (isPrimitive(boxedOrPrimitiveType)) return boxedOrPrimitiveType;
        return unboxType(boxedOrPrimitiveType);
    }

    public static boolean isBoxedTypeOf(@NotNull Type boxedType, @NotNull Type unboxedType) {
        return unboxPrimitiveTypeOrNull(boxedType) == unboxedType;
    }

    public static boolean isIntPrimitive(Type type) {
        return type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE || type == Type.CHAR_TYPE;
    }

    public static boolean isIntOrLongPrimitive(Type type) {
        return isIntPrimitive(type) || type == Type.LONG_TYPE;
    }

    public static boolean isPrimitive(Type type) {
        return type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY;
    }

    @NotNull
    public static Type correctElementType(@NotNull Type type) {
        String internalName = type.getInternalName();
        assert internalName.charAt(0) == '[';
        return Type.getType(internalName.substring(1));
    }

    @NotNull
    public static Type getArrayType(@NotNull Type componentType) {
        return Type.getType("[" + componentType.getDescriptor());
    }

    @Nullable
    public static PrimitiveType asmPrimitiveTypeToLangPrimitiveType(Type type) {
        JvmPrimitiveType jvmPrimitiveType = primitiveTypeByAsmSort.get(type.getSort());
        return jvmPrimitiveType != null ? jvmPrimitiveType.getPrimitiveType() : null;
    }

    @NotNull
    public static Method method(@NotNull String name, @NotNull Type returnType, @NotNull Type... parameterTypes) {
        return new Method(name, Type.getMethodDescriptor(returnType, parameterTypes));
    }

    public static Type stringValueOfType(Type type) {
        int sort = type.getSort();
        return sort == Type.OBJECT || sort == Type.ARRAY
               ? OBJECT_TYPE
               : sort == Type.BYTE || sort == Type.SHORT ? Type.INT_TYPE : type;
    }

    public static void genThrow(@NotNull InstructionAdapter v, @NotNull String exception, @Nullable String message) {
        v.anew(Type.getObjectType(exception));
        v.dup();
        if (message != null) {
            v.aconst(message);
            v.invokespecial(exception, "<init>", "(Ljava/lang/String;)V", false);
        }
        else {
            v.invokespecial(exception, "<init>", "()V", false);
        }
        v.athrow();
    }

    public static void genStringBuilderConstructor(InstructionAdapter v) {
        v.visitTypeInsn(NEW, "java/lang/StringBuilder");
        v.dup();
        v.invokespecial("java/lang/StringBuilder", "<init>", "()V", false);
    }

    public static void genInvertBoolean(InstructionAdapter v) {
        v.iconst(1);
        v.xor(Type.INT_TYPE);
    }

    public static void numConst(int value, Type type, InstructionAdapter v) {
        if (type == Type.FLOAT_TYPE) {
            v.fconst(value);
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.dconst(value);
        }
        else if (type == Type.LONG_TYPE) {
            v.lconst(value);
        }
        else if (type == Type.CHAR_TYPE || type == Type.BYTE_TYPE || type == Type.SHORT_TYPE || type == Type.INT_TYPE) {
            v.iconst(value);
        }
        else {
            throw new IllegalArgumentException("Primitive numeric type expected, got: " + type);
        }
    }

    public static void swap(InstructionAdapter v, Type stackTop, Type afterTop) {
        if (stackTop.getSize() == 1) {
            if (afterTop.getSize() == 1) {
                v.swap();
            }
            else {
                v.dupX2();
                v.pop();
            }
        }
        else {
            if (afterTop.getSize() == 1) {
                v.dup2X1();
            }
            else {
                v.dup2X2();
            }
            v.pop2();
        }
    }

    public static void pushDefaultValueOnStack(@NotNull Type type, @NotNull InstructionAdapter v) {
        v.visitInsn(defaultValueOpcode(type));
    }

    public static int defaultValueOpcode(@NotNull Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return ACONST_NULL;
        }
        if (type.getSort() == Type.FLOAT) {
            return FCONST_0;
        }
        if (type.getSort() == Type.DOUBLE) {
            return DCONST_0;
        }
        if (type.getSort() == Type.LONG) {
            return LCONST_0;
        }
        return ICONST_0;
    }

    public static Type comparisonOperandType(Type left, Type right) {
        if (left == Type.DOUBLE_TYPE || right == Type.DOUBLE_TYPE) return Type.DOUBLE_TYPE;
        if (left == Type.FLOAT_TYPE || right == Type.FLOAT_TYPE) return Type.FLOAT_TYPE;
        if (left == Type.LONG_TYPE || right == Type.LONG_TYPE) return Type.LONG_TYPE;
        if (left == Type.CHAR_TYPE || right == Type.CHAR_TYPE) return Type.CHAR_TYPE;
        return Type.INT_TYPE;
    }

    @NotNull
    public static Type numberFunctionOperandType(@NotNull Type expectedType) {
        if (expectedType == Type.SHORT_TYPE || expectedType == Type.BYTE_TYPE || expectedType == Type.CHAR_TYPE) {
            return Type.INT_TYPE;
        }
        return expectedType;
    }

    public static void pop(@NotNull MethodVisitor v, @NotNull Type type) {
        if (type.getSize() == 2) {
            v.visitInsn(Opcodes.POP2);
        }
        else {
            v.visitInsn(Opcodes.POP);
        }
    }

    public static void pop2(@NotNull MethodVisitor v, @NotNull Type topOfStack, @NotNull Type afterTop) {
        if (topOfStack.getSize() == 1 && afterTop.getSize() == 1) {
            v.visitInsn(POP2);
        } else {
            pop(v, topOfStack);
            pop(v, afterTop);
        }
    }

    public static void pop2(@NotNull MethodVisitor v, @NotNull Type type) {
        if (type.getSize() == 2) {
            v.visitInsn(Opcodes.POP2);
            v.visitInsn(Opcodes.POP2);
        }
        else {
            v.visitInsn(Opcodes.POP2);
        }
    }

    public static void dup(@NotNull InstructionAdapter v, @NotNull Type type) {
        dup(v, type.getSize());
    }

    private static void dup(@NotNull InstructionAdapter v, int size) {
        if (size == 2) {
            v.dup2();
        }
        else if (size == 1) {
            v.dup();
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public static void dupx(@NotNull InstructionAdapter v, @NotNull Type type) {
        dupx(v, type.getSize());
    }

    private static void dupx(@NotNull InstructionAdapter v, int size) {
        if (size == 2) {
            v.dup2X2();
        }
        else if (size == 1) {
            v.dupX1();
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    // Duplicate the element afterTop and push it on the top of the stack.
    public static void dupSecond(@NotNull InstructionAdapter v, @NotNull Type topOfStack, @NotNull Type afterTop) {
        if (afterTop.getSize() == 0) {
            return;
        }

        if (topOfStack.getSize() == 0) {
            dup(v, afterTop);
        } else if (topOfStack.getSize() == 1 && afterTop.getSize() == 1) {
            v.dup2();
            v.pop();
        } else {
            swap(v, topOfStack, afterTop);
            if (topOfStack.getSize() == 1 && afterTop.getSize() == 2) {
                v.dup2X1();
            } else if (topOfStack.getSize() == 2 && afterTop.getSize() == 1) {
                v.dupX2();
            } else /* top = 2, after top = 2 */ {
                v.dup2X2();
            }
        }
    }

    public static void dup(@NotNull InstructionAdapter v, @NotNull Type topOfStack, @NotNull Type afterTop) {
        if (topOfStack.getSize() == 0 && afterTop.getSize() == 0) {
            return;
        }

        if (topOfStack.getSize() == 0) {
            dup(v, afterTop);
        }
        else if (afterTop.getSize() == 0) {
            dup(v, topOfStack);
        }
        else if (afterTop.getSize() == 1) {
            if (topOfStack.getSize() == 1) {
                dup(v, 2);
            }
            else {
                v.dup2X1();
                v.pop2();
                v.dupX2();
                v.dupX2();
                v.pop();
                v.dup2X1();
            }
        }
        else {
            //Note: it's possible to write dup3 and dup4
            throw new UnsupportedOperationException("Don't know how generate dup3/dup4 for: " + topOfStack + " and " + afterTop);
        }
    }

    public static void writeAnnotationData(
            @NotNull AnnotationVisitor av, @NotNull String[] data, @NotNull String[] strings
    ) {
        AnnotationVisitor dataVisitor = av.visitArray(JvmAnnotationNames.METADATA_DATA_FIELD_NAME);
        for (String string : data) {
            dataVisitor.visit(null, string);
        }
        dataVisitor.visitEnd();

        AnnotationVisitor stringsVisitor = av.visitArray(JvmAnnotationNames.METADATA_STRINGS_FIELD_NAME);
        for (String string : strings) {
            stringsVisitor.visit(null, string);
        }
        stringsVisitor.visitEnd();
    }

    @NotNull
    public static Type asmTypeByFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        return Type.getObjectType(internalNameByFqNameWithoutInnerClasses(fqName));
    }

    @NotNull
    public static Type asmTypeByClassId(@NotNull ClassId classId) {
        return Type.getObjectType(classId.asString().replace('.', '$'));
    }

    @NotNull
    public static String internalNameByFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        return JvmClassName.byFqNameWithoutInnerClasses(fqName).getInternalName();
    }


    public static void wrapJavaClassIntoKClass(@NotNull InstructionAdapter v) {
        v.invokestatic(REFLECTION, "getOrCreateKotlinClass", Type.getMethodDescriptor(K_CLASS_TYPE, getType(Class.class)), false);
    }

    public static void wrapJavaClassesIntoKClasses(@NotNull InstructionAdapter v) {
        v.invokestatic(REFLECTION, "getOrCreateKotlinClasses", Type.getMethodDescriptor(K_CLASS_ARRAY_TYPE, getType(Class[].class)), false);
    }

    @Nullable
    public static Integer getVisibilityAccessFlag(Visibility visibility) {
        return visibilityToAccessFlag.get(visibility);
    }
}
