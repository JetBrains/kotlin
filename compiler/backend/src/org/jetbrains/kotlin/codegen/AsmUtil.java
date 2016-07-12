/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.protobuf.MessageLite;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.codegen.serialization.JvmStringTable;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.jvm.RuntimeAssertionInfo;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.load.java.JavaVisibilities;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.DeprecationUtilKt;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.annotations.AnnotationUtilKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.serialization.jvm.BitEncoding;
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isBoolean;
import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isPrimitiveClass;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isConstOrHasJvmFieldAnnotation;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.kotlin.types.TypeUtils.isNullableType;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class AsmUtil {
    private static final Set<Type> STRING_BUILDER_OBJECT_APPEND_ARG_TYPES = Sets.newHashSet(
            getType(String.class),
            getType(StringBuffer.class),
            getType(CharSequence.class)
    );

    private static final int NO_FLAG_LOCAL = 0;
    public static final int NO_FLAG_PACKAGE_PRIVATE = 0;

    @NotNull
    private static final Map<Visibility, Integer> visibilityToAccessFlag = ImmutableMap.<Visibility, Integer>builder()
            .put(Visibilities.PRIVATE, ACC_PRIVATE)
            .put(Visibilities.PRIVATE_TO_THIS, ACC_PRIVATE)
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
    public static Type boxType(@NotNull Type type) {
        JvmPrimitiveType jvmPrimitiveType = primitiveTypeByAsmSort.get(type.getSort());
        return jvmPrimitiveType != null ? asmTypeByFqNameWithoutInnerClasses(jvmPrimitiveType.getWrapperFqName()) : type;
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
        return isPrimitiveClass((ClassDescriptor) descriptor) && !isBoolean((ClassDescriptor) descriptor);
    }

    public static Type correctElementType(Type type) {
        String internalName = type.getInternalName();
        assert internalName.charAt(0) == '[';
        return Type.getType(internalName.substring(1));
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

    public static boolean isAbstractMethod(FunctionDescriptor functionDescriptor, OwnerKind kind) {
        return (functionDescriptor.getModality() == Modality.ABSTRACT
                || isJvmInterface(functionDescriptor.getContainingDeclaration()))
               && !isStaticMethod(kind, functionDescriptor);
    }

    public static boolean isStaticMethod(OwnerKind kind, CallableMemberDescriptor functionDescriptor) {
        return isStaticKind(kind) ||
               KotlinTypeMapper.isStaticAccessor(functionDescriptor) ||
               AnnotationUtilKt.isPlatformStaticInObjectOrClass(functionDescriptor);
    }

    public static boolean isStaticKind(OwnerKind kind) {
        return kind == OwnerKind.PACKAGE || kind == OwnerKind.DEFAULT_IMPLS;
    }

    public static int getMethodAsmFlags(FunctionDescriptor functionDescriptor, OwnerKind kind) {
        int flags = getCommonCallableFlags(functionDescriptor);

        for (AnnotationCodegen.JvmFlagAnnotation flagAnnotation : AnnotationCodegen.METHOD_FLAGS) {
            if (flagAnnotation.hasAnnotation(functionDescriptor.getOriginal())) {
                flags |= flagAnnotation.getJvmFlag();
            }
        }

        if (functionDescriptor.getOriginal().isExternal()) {
            flags |= Opcodes.ACC_NATIVE;
        }

        if (AnnotationUtilKt.isPlatformStaticInCompanionObject(functionDescriptor)) {
            // Native method will be a member of the class, the companion object method will be delegated to it
            flags &= ~Opcodes.ACC_NATIVE;
        }

        if (functionDescriptor.getModality() == Modality.FINAL && !(functionDescriptor instanceof ConstructorDescriptor)) {
            DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
            if (!(containingDeclaration instanceof ClassDescriptor) ||
                ((ClassDescriptor) containingDeclaration).getKind() != ClassKind.INTERFACE) {
                flags |= ACC_FINAL;
            }
        }

        if (isStaticMethod(kind, functionDescriptor)) {
            flags |= ACC_STATIC;
        }

        if (isAbstractMethod(functionDescriptor, kind)) {
            flags |= ACC_ABSTRACT;
        }

        if (KotlinTypeMapper.isAccessor(functionDescriptor)
            || AnnotationUtilKt.hasJvmSyntheticAnnotation(functionDescriptor)) {
            flags |= ACC_SYNTHETIC;
        }

        return flags;
    }

    private static int getCommonCallableFlags(FunctionDescriptor functionDescriptor) {
        int flags = getVisibilityAccessFlag(functionDescriptor);
        flags |= getVarargsFlag(functionDescriptor);
        flags |= getDeprecatedAccessFlag(functionDescriptor);
        if (DeprecationUtilKt.isHiddenInResolution(functionDescriptor)
            || functionDescriptor instanceof PropertyAccessorDescriptor
               && DeprecationUtilKt.isHiddenInResolution(((PropertyAccessorDescriptor) functionDescriptor).getCorrespondingProperty())) {
            flags |= ACC_SYNTHETIC;
        }
        return flags;
    }

    //TODO: move mapping logic to front-end java
    public static int getVisibilityAccessFlag(@NotNull MemberDescriptor descriptor) {
        Integer specialCase = specialCaseVisibility(descriptor);
        if (specialCase != null) {
            return specialCase;
        }
        return getDefaultVisibilityFlag(descriptor.getVisibility());
    }

    public static int getDefaultVisibilityFlag(@NotNull Visibility visibility) {
        Integer defaultMapping = visibilityToAccessFlag.get(visibility);
        if (defaultMapping == null) {
            throw new IllegalStateException(visibility + " is not a valid visibility in backend");
        }
        return defaultMapping;
    }

    /*
        Use this method to get visibility flag for class to define it in byte code (v.defineClass method).
        For other cases use getVisibilityAccessFlag(MemberDescriptor descriptor)
        Classes in byte code should be public or package private
     */
    public static int getVisibilityAccessFlagForClass(@NotNull ClassDescriptor descriptor) {
        if (descriptor instanceof SyntheticClassDescriptorForLambda) {
            return getVisibilityAccessFlagForAnonymous(descriptor);
        }
        if (descriptor.getVisibility() == Visibilities.PUBLIC ||
            descriptor.getVisibility() == Visibilities.PROTECTED ||
            // TODO: should be package private, but for now Kotlin's reflection can't access members of such classes
            descriptor.getVisibility() == Visibilities.LOCAL ||
            descriptor.getVisibility() == Visibilities.INTERNAL) {
            return ACC_PUBLIC;
        }
        return NO_FLAG_PACKAGE_PRIVATE;
    }

    private static int getVisibilityAccessFlagForAnonymous(@NotNull ClassDescriptor descriptor) {
        return InlineUtil.isInlineOrContainingInline(descriptor.getContainingDeclaration()) ? ACC_PUBLIC : NO_FLAG_PACKAGE_PRIVATE;
    }

    public static int calculateInnerClassAccessFlags(@NotNull ClassDescriptor innerClass) {
        int visibility =
                innerClass instanceof SyntheticClassDescriptorForLambda
                ? getVisibilityAccessFlagForAnonymous(innerClass)
                : innerClass.getVisibility() == Visibilities.LOCAL
                  ? ACC_PUBLIC
                  : getVisibilityAccessFlag(innerClass);
        return visibility |
               innerAccessFlagsForModalityAndKind(innerClass) |
               (innerClass.isInner() ? 0 : ACC_STATIC);
    }

    private static int innerAccessFlagsForModalityAndKind(@NotNull ClassDescriptor innerClass) {
        switch (innerClass.getKind()) {
            case INTERFACE:
                return ACC_ABSTRACT | ACC_INTERFACE;
            case ENUM_CLASS:
                return ACC_FINAL | ACC_ENUM;
            case ANNOTATION_CLASS:
                return ACC_ABSTRACT | ACC_ANNOTATION | ACC_INTERFACE;
            default:
                if (innerClass.getModality() == Modality.FINAL) {
                    return ACC_FINAL;
                }
                else if (innerClass.getModality() == Modality.ABSTRACT) {
                    return ACC_ABSTRACT;
                }
        }
        return 0;
    }

    public static int getDeprecatedAccessFlag(@NotNull MemberDescriptor descriptor) {
        if (descriptor instanceof PropertyAccessorDescriptor) {
            return KotlinBuiltIns.isDeprecated(descriptor)
                   ? ACC_DEPRECATED
                   : getDeprecatedAccessFlag(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty());
        }
        else if (KotlinBuiltIns.isDeprecated(descriptor)) {
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
        Visibility memberVisibility = memberDescriptor.getVisibility();

        if (AnnotationUtilKt.isInlineOnlyOrReified(memberDescriptor)) return ACC_PRIVATE;

        if (memberVisibility == Visibilities.LOCAL && memberDescriptor instanceof CallableMemberDescriptor) {
            return ACC_PUBLIC;
        }

        if (isEnumEntry(memberDescriptor)) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }

        if (memberDescriptor instanceof ConstructorDescriptor && isAnonymousObject(memberDescriptor.getContainingDeclaration())) {
            return getVisibilityAccessFlagForAnonymous((ClassDescriptor) memberDescriptor.getContainingDeclaration());
        }

        if (memberDescriptor instanceof SyntheticJavaPropertyDescriptor) {
            return getVisibilityAccessFlag(((SyntheticJavaPropertyDescriptor) memberDescriptor).getGetMethod());
        }
        if (memberDescriptor instanceof PropertyAccessorDescriptor) {
            PropertyDescriptor property = ((PropertyAccessorDescriptor) memberDescriptor).getCorrespondingProperty();
            if (property instanceof SyntheticJavaPropertyDescriptor) {
                FunctionDescriptor method = memberDescriptor == property.getGetter()
                                            ? ((SyntheticJavaPropertyDescriptor) property).getGetMethod()
                                            : ((SyntheticJavaPropertyDescriptor) property).getSetMethod();
                assert method != null : "No get/set method in SyntheticJavaPropertyDescriptor: " + property;
                return getVisibilityAccessFlag(method);
            }
        }

        if (memberDescriptor instanceof CallableDescriptor && memberVisibility == Visibilities.PROTECTED) {
            for (CallableDescriptor overridden : DescriptorUtils.getAllOverriddenDescriptors((CallableDescriptor) memberDescriptor)) {
                if (isJvmInterface(overridden.getContainingDeclaration())) {
                    return ACC_PUBLIC;
                }
            }
        }

        if (!Visibilities.isPrivate(memberVisibility)) {
            return null;
        }

        // the following code is only for PRIVATE visibility of member
        if (memberDescriptor instanceof ConstructorDescriptor) {
            if (isEnumEntry(containingDeclaration)) {
                return NO_FLAG_PACKAGE_PRIVATE;
            }
            if (isEnumClass(containingDeclaration)) {
                //TODO: should be ACC_PRIVATE
                // see http://youtrack.jetbrains.com/issue/KT-2680
                return ACC_PROTECTED;
            }
        }

        return null;
    }

    public static Type stringValueOfType(Type type) {
        int sort = type.getSort();
        return sort == Type.OBJECT || sort == Type.ARRAY
               ? OBJECT_TYPE
               : sort == Type.BYTE || sort == Type.SHORT ? Type.INT_TYPE : type;
    }

    private static Type stringBuilderAppendType(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
                return STRING_BUILDER_OBJECT_APPEND_ARG_TYPES.contains(type) ? type : OBJECT_TYPE;
            case Type.ARRAY:
                return OBJECT_TYPE;
            case Type.BYTE:
            case Type.SHORT:
                return Type.INT_TYPE;
            default:
                return type;
        }
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

    public static void genClosureFields(CalculatedClosure closure, ClassBuilder v, KotlinTypeMapper typeMapper) {
        List<Pair<String, Type>> allFields = new ArrayList<Pair<String, Type>>();

        ClassifierDescriptor captureThis = closure.getCaptureThis();
        if (captureThis != null) {
            allFields.add(Pair.create(CAPTURED_THIS_FIELD, typeMapper.mapType(captureThis)));
        }

        KotlinType captureReceiverType = closure.getCaptureReceiverType();
        if (captureReceiverType != null) {
            allFields.add(Pair.create(CAPTURED_RECEIVER_FIELD, typeMapper.mapType(captureReceiverType)));
        }

        allFields.addAll(closure.getRecordedFields());
        genClosureFields(allFields, v);
    }

    public static void genClosureFields(List<Pair<String, Type>> allFields, ClassBuilder builder) {
        //noinspection PointlessBitwiseExpression
        int access = NO_FLAG_PACKAGE_PRIVATE | ACC_SYNTHETIC | ACC_FINAL;
        for (Pair<String, Type> field : allFields) {
            builder.newField(JvmDeclarationOrigin.NO_ORIGIN, access, field.first, field.second.getDescriptor(), null, null);
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
        v.invokespecial("java/lang/StringBuilder", "<init>", "()V", false);
    }

    public static void genInvokeAppendMethod(InstructionAdapter v, Type type) {
        type = stringBuilderAppendType(type);
        v.invokevirtual("java/lang/StringBuilder", "append", "(" + type.getDescriptor() + ")Ljava/lang/StringBuilder;", false);
    }

    public static StackValue genToString(final StackValue receiver, final Type receiverType) {
        return StackValue.operation(JAVA_STRING_TYPE, new Function1<InstructionAdapter, Unit>() {
            @Override
            public Unit invoke(InstructionAdapter v) {
                Type type = stringValueOfType(receiverType);
                receiver.put(type, v);
                v.invokestatic("java/lang/String", "valueOf", "(" + type.getDescriptor() + ")Ljava/lang/String;", false);
                return null;
            }
        });
    }

    static void genHashCode(MethodVisitor mv, InstructionAdapter iv, Type type) {
        if (type.getSort() == Type.ARRAY) {
            Type elementType = correctElementType(type);
            if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                iv.invokestatic("java/util/Arrays", "hashCode", "([Ljava/lang/Object;)I", false);
            }
            else {
                iv.invokestatic("java/util/Arrays", "hashCode", "(" + type.getDescriptor() + ")I", false);
            }
        }
        else if (type.getSort() == Type.OBJECT) {
            iv.invokevirtual("java/lang/Object", "hashCode", "()I", false);
        }
        else if (type.getSort() == Type.LONG) {
            genLongHashCode(mv, iv);
        }
        else if (type.getSort() == Type.DOUBLE) {
            iv.invokestatic("java/lang/Double", "doubleToLongBits", "(D)J", false);
            genLongHashCode(mv, iv);
        }
        else if (type.getSort() == Type.FLOAT) {
            iv.invokestatic("java/lang/Float", "floatToIntBits", "(F)I", false);
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

    @NotNull
    public static StackValue genEqualsForExpressionsOnStack(
            final @NotNull IElementType opToken,
            final @NotNull StackValue left,
            final @NotNull StackValue right
    ) {
        final Type leftType = left.type;
        final Type rightType = right.type;
        if (isPrimitive(leftType) && leftType == rightType) {
            return StackValue.cmp(opToken, leftType, left, right);
        }

        return StackValue.operation(Type.BOOLEAN_TYPE, new Function1<InstructionAdapter, Unit>() {
            @Override
            public Unit invoke(InstructionAdapter v) {
                left.put(leftType, v);
                right.put(rightType, v);
                genAreEqualCall(v);

                if (opToken == KtTokens.EXCLEQ || opToken == KtTokens.EXCLEQEQEQ) {
                    genInvertBoolean(v);
                }
                return Unit.INSTANCE;
            }
        });
    }

    public static void genAreEqualCall(InstructionAdapter v) {
        v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "areEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
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

    public static void genIncrement(Type baseType, int myDelta, InstructionAdapter v) {
        Type operationType = numberFunctionOperandType(baseType);
        numConst(myDelta, operationType, v);
        v.add(operationType);
        StackValue.coerce(operationType, baseType, v);
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

    public static void genNotNullAssertionsForParameters(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor descriptor,
            @NotNull FrameMap frameMap
    ) {
        if (state.isParamAssertionsDisabled()) return;

        // Private method is not accessible from other classes, no assertions needed
        if (getVisibilityAccessFlag(descriptor) == ACC_PRIVATE) return;

        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        if (receiverParameter != null) {
            genParamAssertion(v, state.getTypeMapper(), frameMap, receiverParameter, "$receiver");
        }

        for (ValueParameterDescriptor parameter : descriptor.getValueParameters()) {
            genParamAssertion(v, state.getTypeMapper(), frameMap, parameter, parameter.getName().asString());
        }
    }

    private static void genParamAssertion(
            @NotNull InstructionAdapter v,
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull FrameMap frameMap,
            @NotNull CallableDescriptor parameter,
            @NotNull String name
    ) {
        KotlinType type = parameter.getReturnType();
        if (type == null || isNullableType(type)) return;
        
        int index = frameMap.getIndex(parameter);
        Type asmType = typeMapper.mapType(type);
        if (asmType.getSort() == Type.OBJECT || asmType.getSort() == Type.ARRAY) {
            v.load(index, asmType);
            v.visitLdcInsn(name);
            v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "checkParameterIsNotNull",
                           "(Ljava/lang/Object;Ljava/lang/String;)V", false);
        }
    }

    @NotNull
    public static StackValue genNotNullAssertions(
            @NotNull GenerationState state,
            @NotNull final StackValue stackValue,
            @Nullable final RuntimeAssertionInfo runtimeAssertionInfo
    ) {
        if (state.isCallAssertionsDisabled()) return stackValue;
        if (runtimeAssertionInfo == null || !runtimeAssertionInfo.getNeedNotNullAssertion()) return stackValue;

        return new StackValue(stackValue.type) {

            @Override
            public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
                stackValue.put(type, v);
                if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                    v.dup();
                    v.visitLdcInsn(runtimeAssertionInfo.getMessage());
                    v.invokestatic("kotlin/jvm/internal/Intrinsics", "checkExpressionValueIsNotNull",
                                   "(Ljava/lang/Object;Ljava/lang/String;)V", false);
                }
            }
        };
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

    public static boolean isInstancePropertyWithStaticBackingField(@NotNull PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE &&
               isObject(propertyDescriptor.getContainingDeclaration());
    }

    public static int getVisibilityForBackingField(@NotNull PropertyDescriptor propertyDescriptor, boolean isDelegate) {
        boolean isExtensionProperty = propertyDescriptor.getExtensionReceiverParameter() != null;
        if (isDelegate || isExtensionProperty) {
            return ACC_PRIVATE;
        }
        else {
            return propertyDescriptor.isLateInit() || isConstOrHasJvmFieldAnnotation(propertyDescriptor)
                   ? getVisibilityAccessFlag(descriptorForVisibility(propertyDescriptor))
                   : ACC_PRIVATE;
        }
    }

    private static MemberDescriptor descriptorForVisibility(@NotNull PropertyDescriptor propertyDescriptor) {
        if (!propertyDescriptor.isVar()) {
            return propertyDescriptor;
        }
        else {
            return propertyDescriptor.getSetter() != null ? propertyDescriptor.getSetter() : propertyDescriptor;
        }
    }

    public static boolean isPropertyWithBackingFieldCopyInOuterClass(@NotNull PropertyDescriptor propertyDescriptor) {
        DeclarationDescriptor propertyContainer = propertyDescriptor.getContainingDeclaration();
        return propertyDescriptor.isConst()
               && isCompanionObject(propertyContainer) && isInterface(propertyContainer.getContainingDeclaration())
               && getVisibilityForBackingField(propertyDescriptor, false) == ACC_PUBLIC;
    }

    public static Type comparisonOperandType(Type left, Type right) {
        if (left == Type.DOUBLE_TYPE || right == Type.DOUBLE_TYPE) return Type.DOUBLE_TYPE;
        if (left == Type.FLOAT_TYPE || right == Type.FLOAT_TYPE) return Type.FLOAT_TYPE;
        if (left == Type.LONG_TYPE || right == Type.LONG_TYPE) return Type.LONG_TYPE;
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
            @NotNull AnnotationVisitor av,
            @NotNull DescriptorSerializer serializer,
            @NotNull MessageLite message
    ) {
        byte[] bytes = serializer.serialize(message);

        AnnotationVisitor data = av.visitArray(JvmAnnotationNames.METADATA_DATA_FIELD_NAME);
        for (String string : BitEncoding.encodeBytes(bytes)) {
            data.visit(null, string);
        }
        data.visitEnd();

        AnnotationVisitor strings = av.visitArray(JvmAnnotationNames.METADATA_STRINGS_FIELD_NAME);
        for (String string : ((JvmStringTable) serializer.getStringTable()).getStrings()) {
            strings.visit(null, string);
        }
        strings.visitEnd();
    }

    @NotNull
    public static Type asmTypeByFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        return Type.getObjectType(internalNameByFqNameWithoutInnerClasses(fqName));
    }

    @NotNull
    public static String internalNameByFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        return JvmClassName.byFqNameWithoutInnerClasses(fqName).getInternalName();
    }

    @NotNull
    public static String getSimpleInternalName(@NotNull String internalName) {
        int lastSlash = internalName.lastIndexOf('/');
        if (lastSlash >= 0) {
            return internalName.substring(lastSlash + 1);
        }
        else {
            return internalName;
        }
    }

    public static void putJavaLangClassInstance(@NotNull InstructionAdapter v, @NotNull Type type) {
        if (isPrimitive(type)) {
            v.getstatic(boxType(type).getInternalName(), "TYPE", "Ljava/lang/Class;");
        }
        else {
            v.aconst(type);
        }
    }

    public static void wrapJavaClassIntoKClass(@NotNull InstructionAdapter v) {
        v.invokestatic(REFLECTION, "getOrCreateKotlinClass", Type.getMethodDescriptor(K_CLASS_TYPE, getType(Class.class)), false);
    }

    public static void wrapJavaClassesIntoKClasses(@NotNull InstructionAdapter v) {
        v.invokestatic(REFLECTION, "getOrCreateKotlinClasses", Type.getMethodDescriptor(K_CLASS_ARRAY_TYPE, getType(Class[].class)), false);
    }

    public static int getReceiverIndex(@NotNull CodegenContext context, @NotNull CallableMemberDescriptor descriptor) {
        OwnerKind kind = context.getContextKind();
        //Trait always should have this descriptor
        return kind != OwnerKind.DEFAULT_IMPLS && isStaticMethod(kind, descriptor) ? 0 : 1;
    }
}
