/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.psi.tree.IElementType;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsnsKt;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase;
import org.jetbrains.kotlin.codegen.state.StaticTypeMapperForOldBackend;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.model.KotlinTypeMarker;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.function.Consumer;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public abstract class StackValue {

    private static final String NULLABLE_BYTE_TYPE_NAME = "java/lang/Byte";
    private static final String NULLABLE_SHORT_TYPE_NAME = "java/lang/Short";
    private static final String NULLABLE_LONG_TYPE_NAME = "java/lang/Long";

    public static final StackValue.Local LOCAL_0 = local(0, OBJECT_TYPE);
    private static final StackValue UNIT = operation(UNIT_TYPE, v -> {
        v.visitFieldInsn(GETSTATIC, UNIT_TYPE.getInternalName(), JvmAbi.INSTANCE_FIELD, UNIT_TYPE.getDescriptor());
        return null;
    });

    @NotNull
    public final Type type;
    @Nullable
    public final KotlinType kotlinType;
    private final boolean canHaveSideEffects;

    protected StackValue(@NotNull Type type) {
        this(type, null, true);
    }

    protected StackValue(@NotNull Type type, boolean canHaveSideEffects) {
        this(type, null, canHaveSideEffects);
    }

    protected StackValue(@NotNull Type type, @Nullable KotlinType kotlinType) {
        this(type, kotlinType, true);
    }

    protected StackValue(@NotNull Type type, @Nullable KotlinType kotlinType, boolean canHaveSideEffects) {
        this.type = type;
        this.kotlinType = kotlinType;
        this.canHaveSideEffects = canHaveSideEffects;
    }

    public void put(@NotNull InstructionAdapter v) {
        put(type, null, v, false);
    }

    public void put(@NotNull Type type, @NotNull InstructionAdapter v) {
        put(type, null, v, false);
    }

    public void put(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
        put(type, kotlinType, v, false);
    }

    public void put(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v, boolean skipReceiver) {
        if (!skipReceiver) {
            putReceiver(v, true);
        }
        putSelector(type, kotlinType, v);
    }

    public abstract void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v);

    public boolean isNonStaticAccess(boolean isRead) {
        return false;
    }

    public void putReceiver(@NotNull InstructionAdapter v, boolean isRead) {
        //by default there is no receiver
        //if you have it inherit StackValueWithSimpleReceiver
    }

    public void dup(@NotNull InstructionAdapter v, boolean withReceiver) {
        if (!Type.VOID_TYPE.equals(type)) {
            AsmUtil.dup(v, type);
        }
    }

    public void store(@NotNull StackValue value, @NotNull InstructionAdapter v) {
        store(value, v, false);
    }

    public boolean canHaveSideEffects() {
        return canHaveSideEffects;
    }

    public void store(@NotNull StackValue value, @NotNull InstructionAdapter v, boolean skipReceiver) {
        if (!skipReceiver) {
            putReceiver(v, false);
        }
        value.put(value.type, value.kotlinType, v);
        storeSelector(value.type, value.kotlinType, v);
    }

    protected void storeSelector(@NotNull Type topOfStackType, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
        throw new UnsupportedOperationException("Cannot store to value " + this);
    }

    @NotNull
    public static Local local(int index, @NotNull Type type) {
        return new Local(index, type);
    }

    public static Local local(int index, @NotNull Type type, @Nullable KotlinType kotlinType) {
        return new Local(index, type, kotlinType);
    }

    @NotNull
    public static StackValue local(int index, @NotNull Type type, @NotNull VariableDescriptor descriptor) {
        return local(index, type, descriptor, null);
    }

    @NotNull
    public static StackValue local(int index, @NotNull Type type, @NotNull VariableDescriptor descriptor, @Nullable KotlinType delegateKotlinType) {
        if (descriptor.isLateInit()) {
            assert delegateKotlinType == null :
                    "Delegated property can't be lateinit: " + descriptor + ", delegate type: " + delegateKotlinType;
            return new LateinitLocal(index, type, descriptor.getType(), descriptor.getName());
        }
        else {
            return new Local(
                    index, type,
                    delegateKotlinType != null ? delegateKotlinType : descriptor.getType()
            );
        }
    }

    @NotNull
    public static StackValue shared(int index, @NotNull Type type, @NotNull VariableDescriptor descriptor) {
        return shared(index, type, descriptor, null);
    }

    @NotNull
    public static StackValue shared(int index, @NotNull Type type, @NotNull VariableDescriptor descriptor, @Nullable KotlinType delegateKotlinType) {
        return new Shared(
                index, type,
                delegateKotlinType != null ? delegateKotlinType : descriptor.getType(),
                descriptor.isLateInit(), descriptor.getName()
        );
    }

    @NotNull
    public static StackValue onStack(@NotNull Type type) {
        return onStack(type, null);
    }

    @NotNull
    public static StackValue onStack(@NotNull Type type, @Nullable KotlinType kotlinType) {
        return type == Type.VOID_TYPE ? none() : new OnStack(type, kotlinType);
    }

    @NotNull
    public static StackValue constant(int value) {
        return constant(value, Type.INT_TYPE);
    }

    @NotNull
    public static StackValue constant(@Nullable Object value, @NotNull Type type) {
        return constant(value, type, null);
    }

    @NotNull
    public static StackValue constant(@Nullable Object value, @NotNull Type type, @Nullable KotlinType kotlinType) {
        if (type == Type.BOOLEAN_TYPE) {
            assert value instanceof Boolean : "Value for boolean constant should have boolean type: " + value;
            return BranchedValue.Companion.booleanConstant((Boolean) value);
        }
        else {
            return new Constant(value, type, kotlinType);
        }
    }

    public static StackValue createDefaultValue(@NotNull Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return constant(null, type);
        }
        else {
            return createDefaultPrimitiveValue(type);
        }
    }

    private static StackValue createDefaultPrimitiveValue(@NotNull Type type) {
        assert Type.BOOLEAN <= type.getSort() && type.getSort() <= Type.DOUBLE :
                "'createDefaultPrimitiveValue' method should be called only for primitive types, but " + type;
        Object value = 0;
        if (type.getSort() == Type.BOOLEAN) {
            value = Boolean.FALSE;
        }
        else if (type.getSort() == Type.FLOAT) {
            value = new Float(0.0);
        }
        else if (type.getSort() == Type.DOUBLE) {
            value = new Double(0.0);
        }
        else if (type.getSort() == Type.LONG) {
            value = new Long(0);
        }

        return constant(value, type);
    }

    @NotNull
    public static StackValue cmp(@NotNull IElementType opToken, @NotNull Type type, StackValue left, StackValue right) {
        return BranchedValue.Companion.cmp(opToken, type, left, right);
    }

    @NotNull
    public static Field field(@NotNull Type type, @NotNull Type owner, @NotNull String name, boolean isStatic, @NotNull StackValue receiver) {
        return new Field(type, null, owner, name, isStatic, receiver, null);
    }

    private static void box(Type type, Type toType, InstructionAdapter v) {
        if (type == Type.INT_TYPE) {
            if (toType.getInternalName().equals(NULLABLE_BYTE_TYPE_NAME)) {
                type = Type.BYTE_TYPE;
            }
            else if (toType.getInternalName().equals(NULLABLE_SHORT_TYPE_NAME)) {
                type = Type.SHORT_TYPE;
            }
            else if (toType.getInternalName().equals(NULLABLE_LONG_TYPE_NAME)) {
                type = Type.LONG_TYPE;
            }
            v.cast(Type.INT_TYPE, type);
        }

        Type boxedType = AsmUtil.boxType(type);
        if (boxedType == type) return;

        v.invokestatic(boxedType.getInternalName(), "valueOf", Type.getMethodDescriptor(boxedType, type), false);
        coerce(boxedType, toType,  v);
    }

    private static void unbox(Type methodOwner, Type type, InstructionAdapter v) {
        assert isPrimitive(type) : "Unboxing should be performed to primitive type, but " + type.getClassName();
        v.invokevirtual(methodOwner.getInternalName(), type.getClassName() + "Value", "()" + type.getDescriptor(), false);
    }

    public static void boxInlineClass(
            @NotNull KotlinTypeMarker kotlinType, @NotNull InstructionAdapter v, @NotNull KotlinTypeMapperBase typeMapper
    ) {
        Type boxed = typeMapper.mapTypeCommon(kotlinType, TypeMappingMode.CLASS_DECLARATION);
        Type unboxed = KotlinTypeMapper.mapUnderlyingTypeOfInlineClassType(kotlinType, typeMapper);
        boolean isNullable = typeMapper.getTypeSystem().isNullableType(kotlinType) && !isPrimitive(unboxed);
        boxInlineClass(unboxed, boxed, isNullable, v);
    }

    public static void boxInlineClass(
            @NotNull Type unboxed, @NotNull Type boxed, boolean isNullable, @NotNull InstructionAdapter v
    ) {
        if (isNullable) {
            boxOrUnboxWithNullCheck(v, vv -> invokeBoxMethod(vv, boxed, unboxed));
        } else {
            invokeBoxMethod(v, boxed, unboxed);
        }
    }

    private static void invokeBoxMethod(
            @NotNull InstructionAdapter v,
            @NotNull Type boxedType,
            @NotNull Type underlyingType
    ) {
        v.invokestatic(
                boxedType.getInternalName(),
                KotlinTypeMapper.BOX_JVM_METHOD_NAME,
                Type.getMethodDescriptor(boxedType, underlyingType),
                false
        );
    }

    public static void unboxInlineClass(
            @NotNull Type type,
            @NotNull KotlinTypeMarker targetInlineClassType,
            @NotNull InstructionAdapter v,
            @NotNull KotlinTypeMapperBase typeMapper
    ) {
        Type boxed = typeMapper.mapTypeCommon(targetInlineClassType, TypeMappingMode.CLASS_DECLARATION);
        Type unboxed = KotlinTypeMapper.mapUnderlyingTypeOfInlineClassType(targetInlineClassType, typeMapper);
        boolean isNullable = typeMapper.getTypeSystem().isNullableType(targetInlineClassType) && !isPrimitive(unboxed);
        unboxInlineClass(type, boxed, unboxed, isNullable, v);
    }

    public static void unboxInlineClass(
            @NotNull Type type, @NotNull Type boxed, @NotNull Type unboxed, boolean isNullable, @NotNull InstructionAdapter v
    ) {
        coerce(type, boxed, v);
        if (isNullable) {
            boxOrUnboxWithNullCheck(v, vv -> invokeUnboxMethod(vv, boxed, unboxed));
        } else {
            invokeUnboxMethod(v, boxed, unboxed);
        }
    }

    private static void invokeUnboxMethod(
            @NotNull InstructionAdapter v,
            @NotNull Type owner,
            @NotNull Type resultType
    ) {
        v.invokevirtual(
                owner.getInternalName(),
                KotlinTypeMapper.UNBOX_JVM_METHOD_NAME,
                "()" + resultType.getDescriptor(),
                false
        );
    }

    private static void boxOrUnboxWithNullCheck(@NotNull InstructionAdapter v, @NotNull Consumer<InstructionAdapter> body) {
        Label lNull = new Label();
        Label lDone = new Label();
        // NB The following piece of code looks sub-optimal (we have a 'null' value on stack and could just keep it there),
        // but it is required, because bytecode verifier doesn't take into account null checks,
        // and sees null-checked value on the top of the stack as a value of the source type (e.g., Ljava/lang/String;),
        // which is not assignable to the expected type (destination type, e.g., LStr;).
        v.dup();
        v.ifnull(lNull);
        body.accept(v);
        v.goTo(lDone);
        v.mark(lNull);
        v.pop();
        v.aconst(null);
        v.mark(lDone);
    }

    protected void coerceTo(@NotNull Type toType, @Nullable KotlinType toKotlinType, @NotNull InstructionAdapter v) {
        coerce(this.type, this.kotlinType, toType, toKotlinType, v);
    }

    protected void coerceFrom(@NotNull Type topOfStackType, @Nullable KotlinType topOfStackKotlinType, @NotNull InstructionAdapter v) {
        coerce(topOfStackType, topOfStackKotlinType, this.type, this.kotlinType, v);
    }

    public static void coerce(
            @NotNull Type fromType,
            @Nullable KotlinType fromKotlinType,
            @NotNull Type toType,
            @Nullable KotlinType toKotlinType,
            @NotNull InstructionAdapter v
    ) {
        if (coerceInlineClasses(fromType, fromKotlinType, toType, toKotlinType, v, StaticTypeMapperForOldBackend.INSTANCE)) return;
        coerce(fromType, toType, v);
    }

    public static boolean requiresInlineClassBoxingOrUnboxing(
            @NotNull Type fromType,
            @Nullable KotlinType fromKotlinType,
            @NotNull Type toType,
            @Nullable KotlinType toKotlinType
    ) {
        // NB see also coerceInlineClasses below

        if (fromKotlinType == null || toKotlinType == null) return false;

        boolean isFromTypeInlineClass = InlineClassesUtilsKt.isInlineClassType(fromKotlinType);
        boolean isToTypeInlineClass = InlineClassesUtilsKt.isInlineClassType(toKotlinType);

        if (!isFromTypeInlineClass && !isToTypeInlineClass) return false;

        boolean isFromTypeUnboxed = isFromTypeInlineClass && isUnboxedInlineClass(fromKotlinType, fromType);
        boolean isToTypeUnboxed = isToTypeInlineClass && isUnboxedInlineClass(toKotlinType, toType);

        if (isFromTypeInlineClass && isToTypeInlineClass) {
            return isFromTypeUnboxed != isToTypeUnboxed;
        }
        else {
            return isFromTypeInlineClass /* && !isToTypeInlineClass */ && isFromTypeUnboxed ||
                   isToTypeInlineClass /* && !isFromTypeInlineClass */ && isToTypeUnboxed;
        }
    }

    private static boolean coerceInlineClasses(
            @NotNull Type fromType,
            @Nullable KotlinType fromKotlinType,
            @NotNull Type toType,
            @Nullable KotlinType toKotlinType,
            @NotNull InstructionAdapter v,
            @NotNull KotlinTypeMapperBase typeMapper
    ) {
        // NB see also requiresInlineClassBoxingOrUnboxing above

        if (fromKotlinType == null || toKotlinType == null) return false;

        boolean isFromTypeInlineClass = InlineClassesUtilsKt.isInlineClassType(fromKotlinType);
        boolean isToTypeInlineClass = InlineClassesUtilsKt.isInlineClassType(toKotlinType);

        if (!isFromTypeInlineClass && !isToTypeInlineClass) return false;

        if (fromKotlinType.equals(toKotlinType) && fromType.equals(toType)) return true;

        /*
        * Preconditions: one of the types is definitely inline class type and types are not equal
        * Consider the following situations:
        *  - both types are inline class types: we do box/unbox only if they are not both boxed or unboxed
        *  - from type is inline class type: we should do box, because target type can be only "subtype" of inline class type (like Any)
        *  - target type is inline class type: we should do unbox, because from type can come from some 'is' check for object type
        *
        *  "return true" means that types were coerced successfully and usual coercion shouldn't be evaluated
        * */

        if (isFromTypeInlineClass && isToTypeInlineClass) {
            boolean isFromTypeUnboxed = isUnboxedInlineClass(fromKotlinType, fromType);
            boolean isToTypeUnboxed = isUnboxedInlineClass(toKotlinType, toType);
            if (isFromTypeUnboxed && !isToTypeUnboxed) {
                boxInlineClass(fromKotlinType, v, typeMapper);
                return true;
            }
            else if (!isFromTypeUnboxed && isToTypeUnboxed) {
                unboxInlineClass(fromType, toKotlinType, v, typeMapper);
                return true;
            }
        }
        else if (isFromTypeInlineClass) {
            if (isUnboxedInlineClass(fromKotlinType, fromType)) {
                boxInlineClass(fromKotlinType, v, typeMapper);
                return true;
            }
        }
        else { // isToTypeInlineClass is `true`
            if (isUnboxedInlineClass(toKotlinType, toType)) {
                unboxInlineClass(fromType, toKotlinType, v, typeMapper);
                return true;
            }
        }

        return false;
    }

    public static boolean isUnboxedInlineClass(@NotNull KotlinType kotlinType, @NotNull Type actualType) {
        return KotlinTypeMapper.mapUnderlyingTypeOfInlineClassType(kotlinType, StaticTypeMapperForOldBackend.INSTANCE).equals(actualType);
    }

    public static void coerce(@NotNull Type fromType, @NotNull Type toType, @NotNull InstructionAdapter v) {
        coerce(fromType, toType, v, false);
    }

    public static void coerce(@NotNull Type fromType, @NotNull Type toType, @NotNull InstructionAdapter v, boolean forceSelfCast) {
        if (toType.equals(fromType) && !forceSelfCast) return;

        if (toType.getSort() == Type.VOID) {
            pop(v, fromType);
        }
        else if (fromType.getSort() == Type.VOID) {
            if (toType.equals(UNIT_TYPE) || toType.equals(OBJECT_TYPE)) {
                putUnitInstance(v);
            }
            else {
                pushDefaultValueOnStack(toType, v);
            }
        }
        else if (toType.equals(UNIT_TYPE)) {
            if (fromType.equals(getType(Object.class))) {
                v.checkcast(UNIT_TYPE);
            }
            else if (!fromType.equals(getType(Void.class))) {
                pop(v, fromType);
                putUnitInstance(v);
            }
        }
        else if (toType.getSort() == Type.ARRAY) {
            if (fromType.getSort() != Type.ARRAY) {
                v.checkcast(toType);
            }
            else if (toType.getDimensions() != fromType.getDimensions()) {
                v.checkcast(toType);
            }
            else if (!toType.getElementType().equals(OBJECT_TYPE)) {
                v.checkcast(toType);
            }
        }
        else if (toType.getSort() == Type.OBJECT) {
            if (fromType.getSort() == Type.OBJECT || fromType.getSort() == Type.ARRAY) {
                if (!toType.equals(OBJECT_TYPE)) {
                    v.checkcast(toType);
                }
            }
            else {
                box(fromType, toType, v);
            }
        }
        else if (fromType.getSort() == Type.OBJECT || fromType.getSort() == Type.ARRAY) {
            // here toType is primitive and fromType is reference (object or array)
            Type unboxedType = unboxPrimitiveTypeOrNull(fromType);
            if (unboxedType != null) {
                unbox(fromType, unboxedType, v);
                coerce(unboxedType, toType, v);
            }
            else if (toType.getSort() == Type.BOOLEAN) {
                coerce(fromType, BOOLEAN_WRAPPER_TYPE, v);
                unbox(BOOLEAN_WRAPPER_TYPE, Type.BOOLEAN_TYPE, v);
            }
            else if (toType.getSort() == Type.CHAR) {
                if (fromType.equals(NUMBER_TYPE)) {
                    unbox(NUMBER_TYPE, Type.INT_TYPE, v);
                    v.visitInsn(Opcodes.I2C);
                }
                else {
                    coerce(fromType, CHARACTER_WRAPPER_TYPE, v);
                    unbox(CHARACTER_WRAPPER_TYPE, Type.CHAR_TYPE, v);
                }
            }
            else {
                coerce(fromType, NUMBER_TYPE, v);
                unbox(NUMBER_TYPE, toType, v);
            }
        }
        else {
            v.cast(fromType, toType);
        }
    }

    public static void putUnitInstance(@NotNull InstructionAdapter v) {
        unit().put(UNIT_TYPE, null, v);
    }

    public static StackValue unit() {
        return UNIT;
    }

    public static StackValue none() {
        return None.INSTANCE;
    }

    public static StackValue operation(Type type, Function1<InstructionAdapter, Unit> lambda) {
        return new StackValue(type, null) {
            @Override
            public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
                lambda.invoke(v);
                coerceTo(type, null, v);
            }
        };
    }

    private static class None extends StackValue {
        public static final None INSTANCE = new None();

        private None() {
            super(Type.VOID_TYPE, false);
        }

        @Override
        public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
            coerceTo(type, kotlinType, v);
        }
    }

    public static class Local extends StackValue {
        public final int index;

        private Local(int index, Type type, KotlinType kotlinType) {
            super(type, kotlinType, false);

            if (index < 0) {
                throw new IllegalStateException("local variable index must be non-negative");
            }

            this.index = index;
        }

        private Local(int index, Type type) {
            this(index, type, null);
        }

        @Override
        public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
            v.load(index, this.type);
            coerceTo(type, kotlinType, v);
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @Nullable KotlinType topOfStackKotlinType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, topOfStackKotlinType, v);
            v.store(index, this.type);
        }
    }

    public static class LateinitLocal extends StackValue {
        public final int index;
        private final Name name;

        private LateinitLocal(int index, Type type, KotlinType kotlinType, Name name) {
            super(type, kotlinType, false);

            if (index < 0) {
                throw new IllegalStateException("local variable index must be non-negative");
            }

            if (name == null) {
                throw new IllegalArgumentException("Lateinit local variable should have name: #" + index + " " + type.getDescriptor());
            }

            this.index = index;
            this.name = name;
        }

        @Override
        public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
            v.load(index, this.type);
            StackValue.genNonNullAssertForLateinit(v, name.asString());
            coerceTo(type, kotlinType, v);
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @Nullable KotlinType topOfStackKotlinType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, topOfStackKotlinType, v);
            v.store(index, this.type);
            PseudoInsnsKt.storeNotNull(v);
        }
    }

    public static class OnStack extends StackValue {
        public OnStack(Type type, KotlinType kotlinType) {
            super(type, kotlinType);
        }

        @Override
        public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
            coerceTo(type, kotlinType, v);
        }
    }

    public static class Constant extends StackValue {
        @Nullable
        public final Object value;

        public Constant(@Nullable Object value, Type type, KotlinType kotlinType) {
            super(type, kotlinType, false);
            assert !Type.BOOLEAN_TYPE.equals(type) : "Boolean constants should be created via 'StackValue.constant'";
            this.value = value;
        }

        @Override
        public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
            if (value instanceof Integer || value instanceof Byte || value instanceof Short) {
                v.iconst(((Number) value).intValue());
            }
            else if (value instanceof Character) {
                v.iconst(((Character) value).charValue());
            }
            else if (value instanceof Long) {
                v.lconst((Long) value);
            }
            else if (value instanceof Float) {
                v.fconst((Float) value);
            }
            else if (value instanceof Double) {
                v.dconst((Double) value);
            }
            else {
                v.aconst(value);
            }

            if (value != null || AsmUtil.isPrimitive(type)) {
                coerceTo(type, kotlinType, v);
            }
        }
    }

    public static class Field extends StackValueWithSimpleReceiver {
        public final Type owner;
        public final String name;
        public final DeclarationDescriptor descriptor;

        public Field(
                @NotNull Type type,
                @Nullable KotlinType kotlinType,
                @NotNull Type owner,
                @NotNull String name,
                boolean isStatic,
                @NotNull StackValue receiver,
                @Nullable DeclarationDescriptor descriptor
        ) {
            super(type, kotlinType, isStatic, isStatic, receiver, receiver.canHaveSideEffects());
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
            v.visitFieldInsn(isStaticPut ? GETSTATIC : GETFIELD, owner.getInternalName(), name, this.type.getDescriptor());
            coerceTo(type, kotlinType, v);
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @Nullable KotlinType topOfStackKotlinType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, topOfStackKotlinType, v);
            v.visitFieldInsn(isStaticStore ? PUTSTATIC : PUTFIELD, owner.getInternalName(), name, this.type.getDescriptor());
        }
    }

    private static void genNonNullAssertForLateinit(@NotNull InstructionAdapter v, @NotNull String name) {
        v.dup();
        Label ok = new Label();
        v.ifnonnull(ok);
        v.visitLdcInsn(name);
        v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "throwUninitializedPropertyAccessException", "(Ljava/lang/String;)V", false);
        v.mark(ok);
    }

    public static class Shared extends StackValueWithSimpleReceiver {
        private final int index;
        private final boolean isLateinit;
        private final Name name;

        public Shared(int index, Type type, KotlinType kotlinType, boolean isLateinit, Name name) {
            super(type, kotlinType, false, false, local(index, OBJECT_TYPE), false);
            this.index = index;

            if (isLateinit && name == null) {
                throw new IllegalArgumentException("Lateinit shared local variable should have name: #" + index + " " + type.getDescriptor());
            }

            this.isLateinit = isLateinit;
            this.name = name;
        }

        public Shared(int index, Type type) {
            this(index, type, null, false, null);
        }

        public int getIndex() {
            return index;
        }

        @Override
        public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
            Type refType = refType(this.type);
            Type sharedType = sharedTypeForType(this.type);
            v.visitFieldInsn(GETFIELD, sharedType.getInternalName(), "element", refType.getDescriptor());
            if (isLateinit) {
                StackValue.genNonNullAssertForLateinit(v, name.asString());
            }
            coerceFrom(refType, null, v);
            coerceTo(type, kotlinType, v);
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @Nullable KotlinType topOfStackKotlinType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, topOfStackKotlinType, v);
            Type refType = refType(this.type);
            Type sharedType = sharedTypeForType(this.type);
            v.visitFieldInsn(PUTFIELD, sharedType.getInternalName(), "element", refType.getDescriptor());
        }
    }

    @NotNull
    public static Type sharedTypeForType(@NotNull Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                return OBJECT_REF_TYPE;
            default:
                PrimitiveType primitiveType = AsmUtil.asmPrimitiveTypeToLangPrimitiveType(type);
                if (primitiveType == null) throw new UnsupportedOperationException();
                return sharedTypeForPrimitive(primitiveType);
        }
    }

    public static Type refType(Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return OBJECT_TYPE;
        }

        return type;
    }

    public abstract static class StackValueWithSimpleReceiver extends StackValue {

        public final boolean isStaticPut;

        public final boolean isStaticStore;
        @NotNull
        public final StackValue receiver;

        public StackValueWithSimpleReceiver(
                @NotNull Type type,
                @Nullable KotlinType kotlinType,
                boolean isStaticPut,
                boolean isStaticStore,
                @NotNull StackValue receiver,
                boolean canHaveSideEffects
        ) {
            super(type, kotlinType, canHaveSideEffects);
            this.receiver = receiver;
            this.isStaticPut = isStaticPut;
            this.isStaticStore = isStaticStore;
        }

        @Override
        public void putReceiver(@NotNull InstructionAdapter v, boolean isRead) {
            boolean hasReceiver = isNonStaticAccess(isRead);
            if (hasReceiver || receiver.canHaveSideEffects()) {
                receiver.put(
                        hasReceiver ? receiver.type : Type.VOID_TYPE,
                        hasReceiver ? receiver.kotlinType : null,
                        v
                );
            }
        }

        @Override
        public boolean isNonStaticAccess(boolean isRead) {
            return isRead ? !isStaticPut : !isStaticStore;
        }

        public int receiverSize() {
            return receiver.type.getSize();
        }

        @Override
        public void dup(@NotNull InstructionAdapter v, boolean withWriteReceiver) {
            if (!withWriteReceiver) {
                super.dup(v, false);
            }
            else {
                int receiverSize = isNonStaticAccess(false) ? receiverSize() : 0;
                switch (receiverSize) {
                    case 0:
                        AsmUtil.dup(v, type);
                        break;

                    case 1:
                        if (type.getSize() == 2) {
                            v.dup2X1();
                        }
                        else {
                            v.dupX1();
                        }
                        break;

                    case 2:
                        if (type.getSize() == 2) {
                            v.dup2X2();
                        }
                        else {
                            v.dupX2();
                        }
                        break;

                    case -1:
                        throw new UnsupportedOperationException();
                }
            }
        }

        @Override
        public void store(
                @NotNull StackValue rightSide, @NotNull InstructionAdapter v, boolean skipReceiver
        ) {
            if (!skipReceiver) {
                putReceiver(v, false);
            }
            rightSide.put(rightSide.type, rightSide.kotlinType, v);
            storeSelector(rightSide.type, rightSide.kotlinType, v);
        }
    }
}

