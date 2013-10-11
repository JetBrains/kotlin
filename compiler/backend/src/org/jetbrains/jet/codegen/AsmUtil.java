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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JavaVisibilities;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaCallableMemberDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.JAVA_STRING_TYPE;
import static org.jetbrains.jet.lang.resolve.java.mapping.PrimitiveTypesUtil.asmTypeForPrimitive;

public class AsmUtil {
    private static final Set<ClassDescriptor> PRIMITIVE_NUMBER_CLASSES = Sets.newHashSet(
            KotlinBuiltIns.getInstance().getByte(),
            KotlinBuiltIns.getInstance().getShort(),
            KotlinBuiltIns.getInstance().getInt(),
            KotlinBuiltIns.getInstance().getLong(),
            KotlinBuiltIns.getInstance().getFloat(),
            KotlinBuiltIns.getInstance().getDouble(),
            KotlinBuiltIns.getInstance().getChar()
    );

    private static final int NO_FLAG_LOCAL = 0;
    public static final int NO_FLAG_PACKAGE_PRIVATE = 0;

    @NotNull
    private static final Map<Visibility, Integer> visibilityToAccessFlag = ImmutableMap.<Visibility, Integer>builder()
            .put(Visibilities.PRIVATE, ACC_PRIVATE)
            .put(Visibilities.PROTECTED, ACC_PROTECTED)
            .put(JavaVisibilities.PROTECTED_STATIC_VISIBILITY, ACC_PROTECTED)
            .put(JavaVisibilities.PROTECTED_AND_PACKAGE, ACC_PROTECTED)
            .put(Visibilities.PUBLIC, ACC_PUBLIC)
            .put(Visibilities.INTERNAL, ACC_PUBLIC)
            .put(Visibilities.LOCAL, NO_FLAG_LOCAL)
            .put(JavaVisibilities.PACKAGE_VISIBILITY, NO_FLAG_PACKAGE_PRIVATE)
            .build();

    public static final String CAPTURED_RECEIVER_FIELD = "receiver$0";
    public static final String CAPTURED_THIS_FIELD = "this$0";

    private static final ImmutableMap<Integer, JvmPrimitiveType> primitiveTypeByAsmSort;
    private static final ImmutableMap<Type, Type> primitiveTypeByBoxedType;

    static {
        ImmutableMap.Builder<Integer, JvmPrimitiveType> typeBySortBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Type, Type> typeByWrapperBuilder = ImmutableMap.builder();
        for (JvmPrimitiveType primitiveType : JvmPrimitiveType.values()) {
            Type asmType = asmTypeForPrimitive(primitiveType);
            typeBySortBuilder.put(asmType.getSort(), primitiveType);
            typeByWrapperBuilder.put(asmTypeByFqNameWithoutInnerClasses(primitiveType.getWrapperFqName()), asmType);
        }
        primitiveTypeByAsmSort = typeBySortBuilder.build();
        primitiveTypeByBoxedType = typeByWrapperBuilder.build();
    }

    private AsmUtil() {
    }

    @NotNull
    public static Type boxType(@NotNull Type type) {
        JvmPrimitiveType jvmPrimitiveType = primitiveTypeByAsmSort.get(type.getSort());
        return jvmPrimitiveType != null ? asmTypeByFqNameWithoutInnerClasses(jvmPrimitiveType.getWrapperFqName()) : type;
    }

    @NotNull
    public static Type unboxType(@NotNull Type boxedType) {
        Type primitiveType = primitiveTypeByBoxedType.get(boxedType);
        if (primitiveType == null) {
            throw new UnsupportedOperationException("Unboxing: " + boxedType);
        }
        return primitiveType;
    }

    public static boolean isIntPrimitive(Type type) {
        return type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE || type == Type.CHAR_TYPE;
    }

    public static boolean isNumberPrimitive(Type type) {
        return isIntPrimitive(type) || type == Type.FLOAT_TYPE || type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE;
    }

    public static boolean isPrimitive(Type type) {
        return type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY;
    }

    public static boolean isPrimitiveNumberClassDescriptor(DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) {
            return false;
        }
        return PRIMITIVE_NUMBER_CLASSES.contains(descriptor);
    }

    public static Type correctElementType(Type type) {
        String internalName = type.getInternalName();
        assert internalName.charAt(0) == '[';
        return Type.getType(internalName.substring(1));
    }

    public static boolean isAbstractMethod(FunctionDescriptor functionDescriptor, OwnerKind kind) {
        return (functionDescriptor.getModality() == Modality.ABSTRACT
                || isInterface(functionDescriptor.getContainingDeclaration()))
               && !isStaticMethod(kind, functionDescriptor);
    }

    public static boolean isStaticMethod(OwnerKind kind, FunctionDescriptor functionDescriptor) {
        return isStatic(kind) || JetTypeMapper.isAccessor(functionDescriptor);
    }

    public static boolean isStatic(OwnerKind kind) {
        return kind == OwnerKind.NAMESPACE || kind == OwnerKind.TRAIT_IMPL;
    }

    public static int getMethodAsmFlags(FunctionDescriptor functionDescriptor, OwnerKind kind) {
        int flags = getCommonCallableFlags(functionDescriptor);

        if (functionDescriptor.getModality() == Modality.FINAL && !(functionDescriptor instanceof ConstructorDescriptor)) {
            DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
            if (!(containingDeclaration instanceof ClassDescriptor) ||
                ((ClassDescriptor) containingDeclaration).getKind() != ClassKind.TRAIT) {
                flags |= ACC_FINAL;
            }
        }

        if (isStaticMethod(kind, functionDescriptor)) {
            flags |= ACC_STATIC;
        }

        if (isAbstractMethod(functionDescriptor, kind)) {
            flags |= ACC_ABSTRACT;
        }

        if (JetTypeMapper.isAccessor(functionDescriptor)) {
            flags |= ACC_SYNTHETIC;
        }

        return flags;
    }

    private static int getCommonCallableFlags(FunctionDescriptor functionDescriptor) {
        int flags = getVisibilityAccessFlag(functionDescriptor);
        flags |= getVarargsFlag(functionDescriptor);
        flags |= getDeprecatedAccessFlag(functionDescriptor);
        return flags;
    }

    //TODO: move mapping logic to front-end java
    public static int getVisibilityAccessFlag(@NotNull MemberDescriptor descriptor) {
        Integer specialCase = specialCaseVisibility(descriptor);
        if (specialCase != null) {
            return specialCase;
        }
        Integer defaultMapping = visibilityToAccessFlag.get(descriptor.getVisibility());
        if (defaultMapping == null) {
            throw new IllegalStateException(descriptor.getVisibility() + " is not a valid visibility in backend. Descriptor: " + descriptor);
        }
        return defaultMapping;
    }

    /*
        Use this method to get visibility flag for class to define it in byte code (v.defineClass method).
        For other cases use getVisibilityAccessFlag(MemberDescriptor descriptor)
        Classes in byte code should be public or package private
     */
    public static int getVisibilityAccessFlagForClass(ClassDescriptor descriptor) {
        if (DescriptorUtils.isTopLevelDeclaration(descriptor) ||
            descriptor.getVisibility() == Visibilities.PUBLIC ||
            descriptor.getVisibility() == Visibilities.INTERNAL) {
            return ACC_PUBLIC;
        }
        return NO_FLAG_PACKAGE_PRIVATE;
    }

    public static int getDeprecatedAccessFlag(@NotNull MemberDescriptor descriptor) {
        if (descriptor instanceof PropertyAccessorDescriptor) {
            return KotlinBuiltIns.getInstance().isDeprecated(descriptor)
                     ? ACC_DEPRECATED
                     : getDeprecatedAccessFlag(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty());
        }
        else if (KotlinBuiltIns.getInstance().isDeprecated(descriptor)) {
            return ACC_DEPRECATED;
        }
        return 0;
    }

    private static int getVarargsFlag(FunctionDescriptor functionDescriptor) {
        if (!functionDescriptor.getValueParameters().isEmpty()
            && functionDescriptor.getValueParameters().get(functionDescriptor.getValueParameters().size() - 1)
                       .getVarargElementType() != null) {
            return ACC_VARARGS;
        }
        return 0;
    }

    @Nullable
    private static Integer specialCaseVisibility(@NotNull MemberDescriptor memberDescriptor) {
        DeclarationDescriptor containingDeclaration = memberDescriptor.getContainingDeclaration();
        if (isInterface(containingDeclaration)) {
            return ACC_PUBLIC;
        }
        Visibility memberVisibility = memberDescriptor.getVisibility();
        if (memberVisibility == Visibilities.LOCAL && memberDescriptor instanceof CallableMemberDescriptor) {
            return ACC_PUBLIC;
        }
        if (isEnumEntry(memberDescriptor)) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }
        if (memberVisibility != Visibilities.PRIVATE) {
            return null;
        }
        // the following code is only for PRIVATE visibility of member
        if (memberDescriptor instanceof ConstructorDescriptor) {
            if (isAnonymousObject(containingDeclaration)) {
                return NO_FLAG_PACKAGE_PRIVATE;
            }

            ClassKind kind = ((ClassDescriptor) containingDeclaration).getKind();
            if (kind == ClassKind.OBJECT) {
                return NO_FLAG_PACKAGE_PRIVATE;
            }
            else if (kind == ClassKind.ENUM_ENTRY) {
                return NO_FLAG_PACKAGE_PRIVATE;
            }
            else if (kind == ClassKind.ENUM_CLASS) {
                //TODO: should be ACC_PRIVATE
                // see http://youtrack.jetbrains.com/issue/KT-2680
                return ACC_PROTECTED;
            }
        }
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            return ACC_PUBLIC;
        }
        return null;
    }

    @NotNull
    public static Type getTraitImplThisParameterType(@NotNull ClassDescriptor traitDescriptor, @NotNull JetTypeMapper typeMapper) {
        JetType jetType = getSuperClass(traitDescriptor);
        Type type = typeMapper.mapType(jetType);
        if (type.getInternalName().equals("java/lang/Object")) {
            return typeMapper.mapType(traitDescriptor.getDefaultType());
        }
        return type;
    }

    private static Type stringValueOfOrStringBuilderAppendType(Type type) {
        int sort = type.getSort();
        return sort == Type.OBJECT || sort == Type.ARRAY
                   ? AsmTypeConstants.OBJECT_TYPE
                   : sort == Type.BYTE || sort == Type.SHORT ? Type.INT_TYPE : type;
    }

    public static void genThrow(MethodVisitor mv, String exception, String message) {
        InstructionAdapter iv = new InstructionAdapter(mv);
        iv.anew(Type.getObjectType(exception));
        iv.dup();
        iv.aconst(message);
        iv.invokespecial(exception, "<init>", "(Ljava/lang/String;)V");
        iv.athrow();
    }

    public static void genMethodThrow(MethodVisitor mv, String exception, String message) {
        mv.visitCode();
        genThrow(mv, exception, message);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    public static void genClosureFields(CalculatedClosure closure, ClassBuilder v, JetTypeMapper typeMapper) {
        ClassifierDescriptor captureThis = closure.getCaptureThis();
        int access = NO_FLAG_PACKAGE_PRIVATE | ACC_SYNTHETIC | ACC_FINAL;
        if (captureThis != null) {
            v.newField(null, access, CAPTURED_THIS_FIELD, typeMapper.mapType(captureThis).getDescriptor(), null,
                       null);
        }

        JetType captureReceiverType = closure.getCaptureReceiverType();
        if (captureReceiverType != null) {
            v.newField(null, access, CAPTURED_RECEIVER_FIELD, typeMapper.mapType(captureReceiverType).getDescriptor(),
                       null, null);
        }

        List<Pair<String, Type>> fields = closure.getRecordedFields();
        for (Pair<String, Type> field : fields) {
            v.newField(null, access, field.first, field.second.getDescriptor(), null, null);
        }
    }

    public static int genAssignInstanceFieldFromParam(FieldInfo info, int index, InstructionAdapter iv) {
        assert !info.isStatic();
        Type fieldType = info.getFieldType();
        iv.load(0, info.getOwnerType());//this
        iv.load(index, fieldType); //param
        iv.visitFieldInsn(PUTFIELD, info.getOwnerInternalName(), info.getFieldName(), fieldType.getDescriptor());
        index += fieldType.getSize();
        return index;
    }

    public static void genStringBuilderConstructor(InstructionAdapter v) {
        v.visitTypeInsn(NEW, "java/lang/StringBuilder");
        v.dup();
        v.invokespecial("java/lang/StringBuilder", "<init>", "()V");
    }

    public static void genInvokeAppendMethod(InstructionAdapter v, Type type) {
        type = stringValueOfOrStringBuilderAppendType(type);
        v.invokevirtual("java/lang/StringBuilder", "append", "(" + type.getDescriptor() + ")Ljava/lang/StringBuilder;");
    }

    public static StackValue genToString(InstructionAdapter v, StackValue receiver, Type receiverType) {
        Type type = stringValueOfOrStringBuilderAppendType(receiverType);
        receiver.put(type, v);
        v.invokestatic("java/lang/String", "valueOf", "(" + type.getDescriptor() + ")Ljava/lang/String;");
        return StackValue.onStack(JAVA_STRING_TYPE);
    }

    static void genHashCode(MethodVisitor mv, InstructionAdapter iv, Type type) {
        if (type.getSort() == Type.ARRAY) {
            Type elementType = correctElementType(type);
            if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                iv.invokestatic("java/util/Arrays", "hashCode", "([Ljava/lang/Object;)I");
            }
            else {
                iv.invokestatic("java/util/Arrays", "hashCode", "(" + type.getDescriptor() + ")I");
            }
        }
        else if (type.getSort() == Type.OBJECT) {
            iv.invokevirtual("java/lang/Object", "hashCode", "()I");
        }
        else if (type.getSort() == Type.LONG) {
            genLongHashCode(mv, iv);
        }
        else if (type.getSort() == Type.DOUBLE) {
            iv.invokestatic("java/lang/Double", "doubleToLongBits", "(D)J");
            genLongHashCode(mv, iv);
        }
        else if (type.getSort() == Type.FLOAT) {
            iv.invokestatic("java/lang/Float", "floatToIntBits", "(F)I");
        }
        else if (type.getSort() == Type.BOOLEAN) {
            Label end = new Label();
            iv.dup();
            iv.ifeq(end);
            iv.pop();
            iv.iconst(1);
            iv.mark(end);
        }
        else { // byte short char int
            // do nothing
        }
    }

    private static void genLongHashCode(MethodVisitor mv, InstructionAdapter iv) {
        iv.dup2();
        iv.iconst(32);
        iv.ushr(Type.LONG_TYPE);
        iv.xor(Type.LONG_TYPE);
        mv.visitInsn(L2I);
    }

    static void genInvertBoolean(InstructionAdapter v) {
        v.iconst(1);
        v.xor(Type.INT_TYPE);
    }

    public static StackValue genEqualsForExpressionsOnStack(
            InstructionAdapter v,
            IElementType opToken,
            Type leftType,
            Type rightType
    ) {
        if ((isNumberPrimitive(leftType) || leftType.getSort() == Type.BOOLEAN) && leftType == rightType) {
            return StackValue.cmp(opToken, leftType);
        }
        else {
            if (opToken == JetTokens.EQEQEQ || opToken == JetTokens.EXCLEQEQEQ) {
                return StackValue.cmp(opToken, leftType);
            }
            else {
                v.invokestatic("jet/runtime/Intrinsics", "areEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z");

                if (opToken == JetTokens.EXCLEQ || opToken == JetTokens.EXCLEQEQEQ) {
                    genInvertBoolean(v);
                }

                return StackValue.onStack(Type.BOOLEAN_TYPE);
            }
        }
    }

    public static void genIncrement(Type expectedType, int myDelta, InstructionAdapter v) {
        if (expectedType == Type.LONG_TYPE) {
            v.lconst(myDelta);
        }
        else if (expectedType == Type.FLOAT_TYPE) {
            v.fconst(myDelta);
        }
        else if (expectedType == Type.DOUBLE_TYPE) {
            v.dconst(myDelta);
        }
        else {
            v.iconst(myDelta);
            v.add(Type.INT_TYPE);
            StackValue.coerce(Type.INT_TYPE, expectedType, v);
            return;
        }
        v.add(expectedType);
    }

    public static Type genNegate(Type expectedType, InstructionAdapter v) {
        if (expectedType == Type.BYTE_TYPE || expectedType == Type.SHORT_TYPE || expectedType == Type.CHAR_TYPE) {
            expectedType = Type.INT_TYPE;
        }
        v.neg(expectedType);
        return expectedType;
    }

    public static void swap(InstructionAdapter v, Type stackTop, Type afterTop) {
        if (stackTop.getSize() == 1) {
            if (afterTop.getSize() == 1) {
                v.swap();
            } else {
                v.dupX2();
                v.pop();
            }
        } else {
            if (afterTop.getSize() == 1) {
                v.dup2X1();
            } else {
                v.dup2X2();
            }
            v.pop2();
        }
    }

    public static void genNotNullAssertionsForParameters(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor descriptor,
            @NotNull FrameMap frameMap
    ) {
        if (!state.isGenerateNotNullParamAssertions()) return;

        // Private method is not accessible from other classes, no assertions needed
        if (getVisibilityAccessFlag(descriptor) == ACC_PRIVATE) return;

        for (ValueParameterDescriptor parameter : descriptor.getValueParameters()) {
            JetType type = parameter.getReturnType();
            if (type == null || isNullableType(type)) continue;

            int index = frameMap.getIndex(parameter);
            Type asmType = state.getTypeMapper().mapReturnType(type);
            if (asmType.getSort() == Type.OBJECT || asmType.getSort() == Type.ARRAY) {
                v.load(index, asmType);
                v.visitLdcInsn(parameter.getName().asString());
                v.invokestatic("jet/runtime/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V");
            }
        }
    }

    public static void genNotNullAssertionForField(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull PropertyDescriptor descriptor
    ) {
        genNotNullAssertion(v, state, descriptor, "checkFieldIsNotNull");
    }

    public static void genNotNullAssertionForMethod(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull ResolvedCall resolvedCall
    ) {
        CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();
        if (descriptor instanceof ConstructorDescriptor) return;

        genNotNullAssertion(v, state, descriptor, "checkReturnedValueIsNotNull");
    }

    private static void genNotNullAssertion(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull CallableDescriptor descriptor,
            @NotNull String assertMethodToCall
    ) {
        if (!state.isGenerateNotNullAssertions()) return;

        if (!isDeclaredInJava(descriptor)) return;

        JetType type = descriptor.getReturnType();
        if (type == null || isNullableType(type)) return;

        Type asmType = state.getTypeMapper().mapReturnType(type);
        if (asmType.getSort() == Type.OBJECT || asmType.getSort() == Type.ARRAY) {
            v.dup();
            v.visitLdcInsn(descriptor.getContainingDeclaration().getName().asString());
            v.visitLdcInsn(descriptor.getName().asString());
            v.invokestatic("jet/runtime/Intrinsics", assertMethodToCall, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V");
        }
    }

    private static boolean isDeclaredInJava(@NotNull CallableDescriptor callableDescriptor) {
        CallableDescriptor descriptor = callableDescriptor;
        while (true) {
            if (descriptor instanceof JavaCallableMemberDescriptor) {
                return true;
            }
            CallableDescriptor original = descriptor.getOriginal();
            if (descriptor == original) break;
            descriptor = original;
        }
        return false;
    }

    public static void pushDefaultValueOnStack(@NotNull Type type, @NotNull InstructionAdapter v) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            v.aconst(null);
        }
        else {
            pushDefaultPrimitiveValueOnStack(type, v);
        }
    }

    public static void pushDefaultPrimitiveValueOnStack(@NotNull Type type, @NotNull InstructionAdapter v) {
        if (type.getSort() == Type.FLOAT) {
            v.fconst(0);
        }
        else if (type.getSort() == Type.DOUBLE) {
            v.dconst(0);
        }
        else if (type.getSort() == Type.LONG) {
            v.lconst(0);
        }
        else {
            v.iconst(0);
        }
    }

    public static boolean isPropertyWithBackingFieldInOuterClass(@NotNull PropertyDescriptor propertyDescriptor) {
        return isPropertyWithSpecialBackingField(propertyDescriptor.getContainingDeclaration(), ClassKind.CLASS);
    }

    public static int getVisibilityForSpecialPropertyBackingField(@NotNull PropertyDescriptor propertyDescriptor, boolean isDelegate) {
        boolean isExtensionProperty = propertyDescriptor.getReceiverParameter() != null;
        if (isDelegate || isExtensionProperty) {
            return ACC_PRIVATE;
        } else {
            return areBothAccessorDefault(propertyDescriptor) ?  getVisibilityAccessFlag(descriptorForVisibility(propertyDescriptor)) : ACC_PRIVATE;
        }
    }

    private static MemberDescriptor descriptorForVisibility(@NotNull PropertyDescriptor propertyDescriptor) {
        if (!propertyDescriptor.isVar() ) {
            return propertyDescriptor;
        } else {
            return propertyDescriptor.getSetter() != null ? propertyDescriptor.getSetter() : propertyDescriptor;
        }
    }

    public static boolean isPropertyWithBackingFieldCopyInOuterClass(@NotNull PropertyDescriptor propertyDescriptor) {
        boolean isExtensionProperty = propertyDescriptor.getReceiverParameter() != null;
        return !propertyDescriptor.isVar() && !isExtensionProperty
               && isPropertyWithSpecialBackingField(propertyDescriptor.getContainingDeclaration(), ClassKind.TRAIT)
               && areBothAccessorDefault(propertyDescriptor)
               && getVisibilityForSpecialPropertyBackingField(propertyDescriptor, false) == ACC_PUBLIC;
    }

    public static boolean isClassObjectWithBackingFieldsInOuter(@NotNull DeclarationDescriptor classObject) {
        return isPropertyWithSpecialBackingField(classObject, ClassKind.CLASS);
    }

    private static boolean areBothAccessorDefault(@NotNull PropertyDescriptor propertyDescriptor) {
        return isAccessorWithEmptyBody(propertyDescriptor.getGetter())
               && (!propertyDescriptor.isVar() || isAccessorWithEmptyBody(propertyDescriptor.getSetter()));
    }

    private static boolean isAccessorWithEmptyBody(@Nullable PropertyAccessorDescriptor accessorDescriptor) {
        return accessorDescriptor == null || !accessorDescriptor.hasBody();
    }

    private static boolean isPropertyWithSpecialBackingField(@NotNull DeclarationDescriptor classObject, ClassKind kind) {
        return isClassObject(classObject) && isKindOf(classObject.getContainingDeclaration(), kind);
    }

    public static Type comparisonOperandType(Type left, Type right) {
        if (left == Type.DOUBLE_TYPE || right == Type.DOUBLE_TYPE) return Type.DOUBLE_TYPE;
        if (left == Type.FLOAT_TYPE || right == Type.FLOAT_TYPE) return Type.FLOAT_TYPE;
        if (left == Type.LONG_TYPE || right == Type.LONG_TYPE) return Type.LONG_TYPE;
        return Type.INT_TYPE;
    }

    @NotNull
    public static Type numberFunctionOperandType(@NotNull Type expectedType) {
        if (expectedType == Type.SHORT_TYPE || expectedType == Type.BYTE_TYPE) {
            return Type.INT_TYPE;
        }
        return expectedType;
    }

    public static void pop(@NotNull InstructionAdapter v, @NotNull Type type) {
        if (type.getSize() == 2) {
            v.pop2();
        }
        else {
            v.pop();
        }
    }

    public static void dup(@NotNull InstructionAdapter v, @NotNull Type type) {
        if (type.getSize() == 2) {
            v.dup2();
        }
        else {
            v.dup();
        }
    }

    @NotNull
    public static String asmDescByFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        return asmTypeByFqNameWithoutInnerClasses(fqName).getDescriptor();
    }

    @NotNull
    public static String shortNameByAsmType(@NotNull Type type) {
        String internalName = type.getInternalName();
        int lastSlash = internalName.lastIndexOf('/');
        return lastSlash < 0 ? internalName : internalName.substring(lastSlash + 1);
    }

    @NotNull
    public static Type asmTypeByFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        return Type.getObjectType(JvmClassName.byFqNameWithoutInnerClasses(fqName).getInternalName());
    }
}
