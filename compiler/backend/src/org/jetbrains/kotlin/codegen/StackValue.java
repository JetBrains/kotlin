/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.psi.tree.IElementType;
import kotlin.Unit;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsnsKt;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

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
    private final boolean canHaveSideEffects;

    protected StackValue(@NotNull Type type) {
        this(type, true);
    }

    protected StackValue(@NotNull Type type, boolean canHaveSideEffects) {
        this.type = type;
        this.canHaveSideEffects = canHaveSideEffects;
    }

    /**
     * This method is called to put the value on the top of the JVM stack if <code>depth</code> other values have been put on the
     * JVM stack after this value was generated.
     *
     * @param type  the type as which the value should be put
     * @param v     the visitor used to genClassOrObject the instructions
     * @param depth the number of new values put onto the stack
     */
    public void moveToTopOfStack(@NotNull Type type, @NotNull InstructionAdapter v, int depth) {
        put(type, v);
    }

    public void put(@NotNull Type type, @NotNull InstructionAdapter v) {
        put(type, v, false);
    }

    public void put(@NotNull Type type, @NotNull InstructionAdapter v, boolean skipReceiver) {
        if (!skipReceiver) {
            putReceiver(v, true);
        }
        putSelector(type, v);
    }

    public abstract void putSelector(@NotNull Type type, @NotNull InstructionAdapter v);

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
        value.put(value.type, v);
        storeSelector(value.type, v);
    }

    protected void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
        throw new UnsupportedOperationException("Cannot store to value " + this);
    }

    @NotNull
    public static Local local(int index, @NotNull Type type) {
        return new Local(index, type);
    }

    @NotNull
    public static Local local(int index, @NotNull Type type, @NotNull VariableDescriptor descriptor) {
        return new Local(index, type, descriptor.isLateInit(), descriptor.getName());
    }

    @NotNull
    public static Delegate delegate(
            @NotNull Type type,
            @NotNull StackValue delegateValue,
            @NotNull StackValue metadataValue,
            @NotNull VariableDescriptorWithAccessors variableDescriptor,
            @NotNull ExpressionCodegen codegen
    ) {
        return new Delegate(type, delegateValue, metadataValue, variableDescriptor, codegen);
    }

    @NotNull
    public static StackValue shared(int index, @NotNull Type type) {
        return new Shared(index, type);
    }

    @NotNull
    public static StackValue shared(int index, @NotNull Type type, @NotNull VariableDescriptor descriptor) {
        return new Shared(index, type, descriptor.isLateInit(), descriptor.getName());
    }

    @NotNull
    public static StackValue onStack(@NotNull Type type) {
        return type == Type.VOID_TYPE ? none() : new OnStack(type);
    }

    @NotNull
    public static StackValue integerConstant(int value, @NotNull Type type) {
        if (type == Type.LONG_TYPE) {
            return constant(Long.valueOf(value), type);
        }
        else if (type == Type.BYTE_TYPE || type == Type.SHORT_TYPE || type == Type.INT_TYPE) {
            return constant(Integer.valueOf(value), type);
        }
        else if (type == Type.CHAR_TYPE) {
            return constant(Character.valueOf((char) value), type);
        }
        else {
            throw new AssertionError("Unexpected integer type: " + type);
        }
    }

    @NotNull
    public static StackValue constant(@Nullable Object value, @NotNull Type type) {
        if (type == Type.BOOLEAN_TYPE) {
            assert value instanceof Boolean : "Value for boolean constant should have boolean type: " + value;
            return BranchedValue.Companion.booleanConstant((Boolean) value);
        }
        else {
            return new Constant(value, type);
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
    public static StackValue not(@NotNull StackValue stackValue) {
        return BranchedValue.Companion.createInvertValue(stackValue);
    }

    public static StackValue or(@NotNull StackValue left, @NotNull StackValue right) {
        return new Or(left, right);
    }

    public static StackValue and(@NotNull StackValue left, @NotNull StackValue right) {
        return new And(left, right);
    }

    public static StackValue compareIntWithZero(@NotNull StackValue argument, int operation) {
        return new BranchedValue(argument, null, Type.INT_TYPE, operation);
    }

    public static StackValue compareWithNull(@NotNull StackValue argument, int operation) {
        return new BranchedValue(argument, null, AsmTypes.OBJECT_TYPE, operation);
    }

    @NotNull
    public static StackValue arrayElement(@NotNull Type type, StackValue array, StackValue index) {
        return new ArrayElement(type, array, index);
    }

    @NotNull
    public static StackValue collectionElement(
            CollectionElementReceiver collectionElementReceiver,
            Type type,
            ResolvedCall<FunctionDescriptor> getter,
            ResolvedCall<FunctionDescriptor> setter,
            ExpressionCodegen codegen
    ) {
        return new CollectionElement(collectionElementReceiver, type, getter, setter, codegen);
    }

    @NotNull
    public static Field field(@NotNull Type type, @NotNull Type owner, @NotNull String name, boolean isStatic, @NotNull StackValue receiver) {
        return field(type, owner, name, isStatic, receiver, null);
    }

    @NotNull
    public static Field field(
            @NotNull Type type,
            @NotNull Type owner,
            @NotNull String name,
            boolean isStatic,
            @NotNull StackValue receiver,
            @Nullable DeclarationDescriptor descriptor
    ) {
        return new Field(type, owner, name, isStatic, receiver, descriptor);
    }

    @NotNull
    public static Field field(@NotNull StackValue.Field field, @NotNull StackValue newReceiver) {
        return field(field.type, field.owner, field.name, field.isStaticPut, newReceiver, field.descriptor);
    }

    @NotNull
    public static Field field(@NotNull FieldInfo info, @NotNull StackValue receiver) {
        return field(info.getFieldType(), Type.getObjectType(info.getOwnerInternalName()), info.getFieldName(), info.isStatic(), receiver);
    }

    @NotNull
    public static StackValue changeReceiverForFieldAndSharedVar(@NotNull StackValueWithSimpleReceiver stackValue, @Nullable StackValue newReceiver) {
        //TODO static check
        if (newReceiver == null || stackValue.isStaticPut) return stackValue;
        return stackValue.changeReceiver(newReceiver);
    }

    @NotNull
    public static Property property(
            @NotNull PropertyDescriptor descriptor,
            @Nullable Type backingFieldOwner,
            @NotNull Type type,
            boolean isStaticBackingField,
            @Nullable String fieldName,
            @Nullable CallableMethod getter,
            @Nullable CallableMethod setter,
            @NotNull StackValue receiver,
            @NotNull ExpressionCodegen codegen,
            @Nullable ResolvedCall resolvedCall,
            boolean skipLateinitAssertion
    ) {
        return new Property(descriptor, backingFieldOwner, getter, setter, isStaticBackingField, fieldName, type, receiver, codegen,
                            resolvedCall, skipLateinitAssertion);
    }

    @NotNull
    public static StackValue expression(Type type, KtExpression expression, ExpressionCodegen generator) {
        return new Expression(type, expression, generator);
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

    protected void coerceTo(@NotNull Type toType, @NotNull InstructionAdapter v) {
        coerce(this.type, toType, v);
    }

    protected void coerceFrom(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
        coerce(topOfStackType, this.type, v);
    }

    public static void coerce(@NotNull Type fromType, @NotNull Type toType, @NotNull InstructionAdapter v) {
        if (toType.equals(fromType)) return;

        if (toType.getSort() == Type.VOID) {
            pop(v, fromType);
        }
        else if (fromType.getSort() == Type.VOID) {
            if (toType.equals(UNIT_TYPE) || toType.equals(OBJECT_TYPE)) {
                putUnitInstance(v);
            }
            else if (toType.getSort() == Type.OBJECT || toType.getSort() == Type.ARRAY) {
                v.aconst(null);
            }
            else {
                pushDefaultPrimitiveValueOnStack(toType, v);
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
            v.checkcast(toType);
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
        else if (fromType.getSort() == Type.OBJECT) {
            //toType is primitive here
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
        unit().put(UNIT_TYPE, v);
    }

    public static StackValue unit() {
        return UNIT;
    }

    public static StackValue none() {
        return None.INSTANCE;
    }

    public static Field receiverWithRefWrapper(
            @NotNull Type localType,
            @NotNull Type classType,
            @NotNull String fieldName,
            @NotNull StackValue receiver,
            @Nullable DeclarationDescriptor descriptor
    ) {
        return field(sharedTypeForType(localType), classType, fieldName, false, receiver, descriptor);
    }

    public static FieldForSharedVar fieldForSharedVar(
            @NotNull Type localType,
            @NotNull Type classType,
            @NotNull String fieldName,
            @NotNull Field refWrapper,
            @NotNull VariableDescriptor variableDescriptor
    ) {
        return new FieldForSharedVar(localType, classType, fieldName, refWrapper,
                                     variableDescriptor.isLateInit(), variableDescriptor.getName());
    }

    @NotNull
    public static FieldForSharedVar fieldForSharedVar(@NotNull FieldForSharedVar field, @NotNull StackValue newReceiver) {
        Field oldReceiver = (Field) field.receiver;
        Field newSharedVarReceiver = field(oldReceiver, newReceiver);
        return new FieldForSharedVar(field.type, field.owner, field.name, newSharedVarReceiver, field.isLateinit, field.variableName);
    }

    public static StackValue coercion(@NotNull StackValue value, @NotNull Type castType) {
        if (value.type.equals(castType)) {
            return value;
        }
        return new CoercionValue(value, castType);
    }

    @NotNull
    public static StackValue thisOrOuter(
            @NotNull ExpressionCodegen codegen,
            @NotNull ClassDescriptor descriptor,
            boolean isSuper,
            boolean castReceiver
    ) {
        // Coerce 'this' for the case when it is smart cast.
        // Do not coerce for other cases due to the 'protected' access issues (JVMS 7, 4.9.2 Structural Constraints).
        boolean coerceType = descriptor.getKind() == ClassKind.INTERFACE || (castReceiver && !isSuper);
        return new ThisOuter(codegen, descriptor, isSuper, coerceType);
    }

    public static StackValue postIncrement(int index, int increment) {
        return new PostIncrement(index, increment);
    }

    public static StackValue preIncrementForLocalVar(int index, int increment) {
        return new PreIncrementForLocalVar(index, increment);
    }

    public static StackValue preIncrement(
            @NotNull Type type,
            @NotNull StackValue stackValue,
            int delta,
            ResolvedCall resolvedCall,
            @NotNull ExpressionCodegen codegen
    ) {
        if (stackValue instanceof StackValue.Local && Type.INT_TYPE == stackValue.type) {
            return preIncrementForLocalVar(((StackValue.Local) stackValue).index, delta);
        }
        return new PrefixIncrement(type, stackValue, resolvedCall, codegen);
    }

    public static StackValue receiver(
            ResolvedCall<?> resolvedCall,
            StackValue receiver,
            ExpressionCodegen codegen,
            @Nullable Callable callableMethod
    ) {
        ReceiverValue callDispatchReceiver = resolvedCall.getDispatchReceiver();
        CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();
        if (descriptor instanceof SyntheticFieldDescriptor) {
            callDispatchReceiver = ((SyntheticFieldDescriptor) descriptor).getDispatchReceiverForBackend();
        }

        ReceiverValue callExtensionReceiver = resolvedCall.getExtensionReceiver();

        boolean isImportedObjectMember = false;
        if (descriptor instanceof ImportedFromObjectCallableDescriptor) {
            isImportedObjectMember = true;
            descriptor = ((ImportedFromObjectCallableDescriptor) descriptor).getCallableFromObject();
        }

        if (callDispatchReceiver != null || callExtensionReceiver != null
            || isLocalFunCall(callableMethod) || isImportedObjectMember) {
            ReceiverParameterDescriptor dispatchReceiverParameter = descriptor.getDispatchReceiverParameter();
            ReceiverParameterDescriptor extensionReceiverParameter = descriptor.getExtensionReceiverParameter();

            if (descriptor instanceof SyntheticFieldDescriptor) {
                dispatchReceiverParameter = ((SyntheticFieldDescriptor) descriptor).getDispatchReceiverParameterForBackend();
            }

            boolean hasExtensionReceiver = callExtensionReceiver != null;
            StackValue dispatchReceiver = platformStaticCallIfPresent(
                    genReceiver(hasExtensionReceiver ? none() : receiver, codegen, descriptor, callableMethod, callDispatchReceiver, false),
                    descriptor
            );
            StackValue extensionReceiver = genReceiver(receiver, codegen, descriptor, callableMethod, callExtensionReceiver, true);
            return CallReceiver.generateCallReceiver(
                    resolvedCall, codegen, callableMethod,
                    dispatchReceiverParameter, dispatchReceiver,
                    extensionReceiverParameter, extensionReceiver
            );
        }
        return receiver;
    }

    private static StackValue genReceiver(
            @NotNull StackValue receiver,
            @NotNull ExpressionCodegen codegen,
            @NotNull CallableDescriptor descriptor,
            @Nullable Callable callableMethod,
            @Nullable ReceiverValue receiverValue,
            boolean isExtension
    ) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (receiver == none()) {
            if (receiverValue != null) {
                return codegen.generateReceiverValue(receiverValue, false);
            }
            else if (isLocalFunCall(callableMethod) && !isExtension) {
                StackValue value = codegen.findLocalOrCapturedValue(descriptor.getOriginal());
                assert value != null : "Local fun should be found in locals or in captured params: " + descriptor;
                return value;
            }
            else if (!isExtension && DescriptorUtils.isObject(containingDeclaration)) {
                // Object member could be imported by name, in which case it has no explicit dispatch receiver
                return singleton((ClassDescriptor) containingDeclaration, codegen.typeMapper);
            }
        }
        else if (receiverValue != null) {
            return receiver;
        }
        return none();
    }

    private static StackValue platformStaticCallIfPresent(@NotNull StackValue resultReceiver, @NotNull CallableDescriptor descriptor) {
        if (CodegenUtilKt.isJvmStaticInObjectOrClass(descriptor)) {
            if (resultReceiver.canHaveSideEffects()) {
                return coercion(resultReceiver, Type.VOID_TYPE);
            }
            else {
                return none();
            }
        }
        return resultReceiver;
    }

    @Contract("null -> false")
    static boolean isLocalFunCall(@Nullable Callable callableMethod) {
        return callableMethod != null && callableMethod.getGenerateCalleeType() != null;
    }

    public static StackValue receiverWithoutReceiverArgument(StackValue receiverWithParameter) {
        if (receiverWithParameter instanceof CallReceiver) {
            return ((CallReceiver) receiverWithParameter).withoutReceiverArgument();
        }
        return receiverWithParameter;
    }

    @NotNull
    public static Field enumEntry(@NotNull ClassDescriptor descriptor, @NotNull KotlinTypeMapper typeMapper) {
        DeclarationDescriptor enumClass = descriptor.getContainingDeclaration();
        assert DescriptorUtils.isEnumClass(enumClass) : "Enum entry should be declared in enum class: " + descriptor;
        Type type = typeMapper.mapType((ClassDescriptor) enumClass);
        return field(type, type, descriptor.getName().asString(), true, none(), descriptor);
    }

    @NotNull
    public static Field singleton(@NotNull ClassDescriptor classDescriptor, @NotNull KotlinTypeMapper typeMapper) {
        return field(FieldInfo.createForSingleton(classDescriptor, typeMapper), none());
    }

    public static Field createSingletonViaInstance(@NotNull ClassDescriptor classDescriptor, @NotNull KotlinTypeMapper typeMapper, @NotNull String name) {
        return field(FieldInfo.createSingletonViaInstance(classDescriptor, typeMapper, name), none());
    }

    public static StackValue operation(Type type, Function1<InstructionAdapter, Unit> lambda) {
        return new OperationStackValue(type, lambda);
    }

    public static StackValue functionCall(Type type, Function1<InstructionAdapter, Unit> lambda) {
        return new FunctionCallStackValue(type, lambda);
    }

    public static boolean couldSkipReceiverOnStaticCall(StackValue value) {
        return value instanceof Local || value instanceof Constant;
    }

    private static class None extends StackValue {
        public static final None INSTANCE = new None();

        private None() {
            super(Type.VOID_TYPE, false);
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            coerceTo(type, v);
        }
    }

    public static class Local extends StackValue {
        public final int index;
        private final boolean isLateinit;
        private final Name name;

        private Local(int index, Type type, boolean isLateinit, Name name) {
            super(type, false);

            if (index < 0) {
                throw new IllegalStateException("local variable index must be non-negative");
            }

            if (isLateinit && name == null) {
                throw new IllegalArgumentException("Lateinit local variable should have name: #" + index + " " + type.getDescriptor());
            }

            this.index = index;
            this.isLateinit = isLateinit;
            this.name = name;
        }

        private Local(int index, Type type) {
            this(index, type, false, null);
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            v.load(index, this.type);
            if (isLateinit) {
                StackValue.genNonNullAssertForLateinit(v, name.asString());
            }
            coerceTo(type, v);
            // TODO unbox
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.store(index, this.type);
            if (isLateinit) {
                PseudoInsnsKt.storeNotNull(v);
            }
        }
    }

    public static class Delegate extends StackValue {
        @NotNull
        private final StackValue delegateValue;
        @NotNull
        private final StackValue metadataValue;
        @NotNull
        private final VariableDescriptorWithAccessors variableDescriptor;
        @NotNull
        private final ExpressionCodegen codegen;

        private Delegate(
                @NotNull Type type,
                @NotNull StackValue delegateValue,
                @NotNull StackValue metadataValue,
                @NotNull VariableDescriptorWithAccessors variableDescriptor,
                @NotNull ExpressionCodegen codegen
        ) {
            super(type);
            this.delegateValue = delegateValue;
            this.metadataValue = metadataValue;
            this.variableDescriptor = variableDescriptor;
            this.codegen = codegen;
        }


        private ResolvedCall<FunctionDescriptor> getResolvedCall(boolean isGetter) {
            BindingContext bindingContext = codegen.getState().getBindingContext();
            VariableAccessorDescriptor accessor = isGetter ? variableDescriptor.getGetter(): variableDescriptor.getSetter();
            assert accessor != null : "Accessor descriptor for delegated local property should be present " + variableDescriptor;
            ResolvedCall<FunctionDescriptor> resolvedCall = bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor);
            assert resolvedCall != null : "Resolve call should be recorded for delegate call " + variableDescriptor;
            return resolvedCall;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCall(true);
            List<? extends ValueArgument> arguments = resolvedCall.getCall().getValueArguments();
            assert arguments.size() == 2 : "Resolved call for 'getValue' should have 2 arguments, but was " +
                                           arguments.size() + ": " + resolvedCall;

            codegen.tempVariables.put(arguments.get(0).asElement(), StackValue.constant(null, OBJECT_TYPE));
            codegen.tempVariables.put(arguments.get(1).asElement(), metadataValue);
            StackValue lastValue = codegen.invokeFunction(resolvedCall, delegateValue);
            lastValue.put(type, v);

            codegen.tempVariables.remove(arguments.get(0).asElement());
            codegen.tempVariables.remove(arguments.get(1).asElement());
        }

        @Override
        public void store(@NotNull StackValue rightSide, @NotNull InstructionAdapter v, boolean skipReceiver) {
            ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCall(false);
            List<? extends ValueArgument> arguments = resolvedCall.getCall().getValueArguments();
            assert arguments.size() == 3 : "Resolved call for 'setValue' should have 3 arguments, but was " +
                                           arguments.size() + ": " + resolvedCall;

            codegen.tempVariables.put(arguments.get(0).asElement(), StackValue.constant(null, OBJECT_TYPE));
            codegen.tempVariables.put(arguments.get(1).asElement(), metadataValue);
            codegen.tempVariables.put(arguments.get(2).asElement(), rightSide);
            StackValue lastValue = codegen.invokeFunction(resolvedCall, delegateValue);
            lastValue.put(Type.VOID_TYPE, v);

            codegen.tempVariables.remove(arguments.get(0).asElement());
            codegen.tempVariables.remove(arguments.get(1).asElement());
            codegen.tempVariables.remove(arguments.get(2).asElement());
        }
    }

    public static class OnStack extends StackValue {
        public OnStack(Type type) {
            super(type);
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            coerceTo(type, v);
        }

        @Override
        public void moveToTopOfStack(@NotNull Type type, @NotNull InstructionAdapter v, int depth) {
            if (depth == 0) {
                put(type, v);
            }
            else if (depth == 1) {
                int size = this.type.getSize();
                if (size == 1) {
                    v.swap();
                }
                else if (size == 2) {
                    v.dupX2();
                    v.pop();
                }
                else {
                    throw new UnsupportedOperationException("don't know how to move type " + type + " to top of stack");
                }

                coerceTo(type, v);
            }
            else if (depth == 2) {
                int size = this.type.getSize();
                if (size == 1) {
                    v.dup2X1();
                    v.pop2();
                }
                else if (size == 2) {
                    v.dup2X2();
                    v.pop2();
                }
                else {
                    throw new UnsupportedOperationException("don't know how to move type " + type + " to top of stack");
                }

                coerceTo(type, v);
            }
            else {
                throw new UnsupportedOperationException("unsupported move-to-top depth " + depth);
            }
        }
    }

    public static class Constant extends StackValue {
        @Nullable
        public final Object value;

        public Constant(@Nullable Object value, Type type) {
            super(type, false);
            assert !Type.BOOLEAN_TYPE.equals(type) : "Boolean constants should be created via 'StackValue.constant'";
            this.value = value;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
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
                coerceTo(type, v);
            }
        }
    }

    private static class ArrayElement extends StackValueWithSimpleReceiver {
        private final Type type;

        public ArrayElement(Type type, StackValue array, StackValue index) {
            super(type, false, false, new Receiver(Type.LONG_TYPE, array, index), true);
            this.type = type;
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.astore(this.type);
        }

        @Override
        public int receiverSize() {
            return 2;
        }

        @Override
        public void putSelector(
                @NotNull Type type, @NotNull InstructionAdapter v
        ) {
            v.aload(this.type);    // assumes array and index are on the stack
            coerceTo(type, v);
        }
    }

    public static class CollectionElementReceiver extends StackValue {
        private final Callable callable;
        private final boolean isGetter;
        private final ExpressionCodegen codegen;
        private final List<ResolvedValueArgument> valueArguments;
        private final FrameMap frame;
        private final StackValue receiver;
        private final ResolvedCall<FunctionDescriptor> resolvedGetCall;
        private final ResolvedCall<FunctionDescriptor> resolvedSetCall;
        private DefaultCallArgs defaultArgs;
        private CallGenerator callGenerator;
        boolean isComplexOperationWithDup;

        public CollectionElementReceiver(
                @NotNull Callable callable,
                @NotNull StackValue receiver,
                ResolvedCall<FunctionDescriptor> resolvedGetCall,
                ResolvedCall<FunctionDescriptor> resolvedSetCall,
                boolean isGetter,
                @NotNull ExpressionCodegen codegen,
                List<ResolvedValueArgument> valueArguments
        ) {
            super(OBJECT_TYPE);
            this.callable = callable;

            this.isGetter = isGetter;
            this.receiver = receiver;
            this.resolvedGetCall = resolvedGetCall;
            this.resolvedSetCall = resolvedSetCall;
            this.valueArguments = valueArguments;
            this.codegen = codegen;
            this.frame = codegen.myFrameMap;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            ResolvedCall<?> call = isGetter ? resolvedGetCall : resolvedSetCall;
            StackValue newReceiver = StackValue.receiver(call, receiver, codegen, callable);
            ArgumentGenerator generator = createArgumentGenerator();
            newReceiver.put(newReceiver.type, v);
            callGenerator.processAndPutHiddenParameters(false);

            defaultArgs = generator.generate(valueArguments, valueArguments, call.getResultingDescriptor());
        }

        private ArgumentGenerator createArgumentGenerator() {
            assert callGenerator == null :
                    "'putSelector' and 'createArgumentGenerator' methods should be called once for CollectionElementReceiver: " + callable;
            ResolvedCall<FunctionDescriptor> resolvedCall = isGetter ? resolvedGetCall : resolvedSetCall;
            assert resolvedCall != null : "Resolved call should be non-null: " + callable;
            callGenerator =
                    !isComplexOperationWithDup ? codegen.getOrCreateCallGenerator(resolvedCall) : codegen.defaultCallGenerator;
            return new CallBasedArgumentGenerator(
                    codegen,
                    callGenerator,
                    resolvedCall.getResultingDescriptor().getValueParameters(), callable.getValueParameterTypes()
            );
        }

        @Override
        public void dup(@NotNull InstructionAdapter v, boolean withReceiver) {
            dupReceiver(v);
        }

        public void dupReceiver(@NotNull InstructionAdapter v) {
            if (CollectionElement.isStandardStack(codegen.typeMapper, resolvedGetCall, 1) &&
                CollectionElement.isStandardStack(codegen.typeMapper, resolvedSetCall, 2)) {
                v.dup2();   // collection and index
                return;
            }

            FrameMap.Mark mark = frame.mark();

            // indexes
            List<ValueParameterDescriptor> valueParameters = resolvedGetCall.getResultingDescriptor().getValueParameters();
            int firstParamIndex = -1;
            for (int i = valueParameters.size() - 1; i >= 0; --i) {
                Type type = codegen.typeMapper.mapType(valueParameters.get(i).getType());
                firstParamIndex = frame.enterTemp(type);
                v.store(firstParamIndex, type);
            }

            ReceiverValue receiverParameter = resolvedGetCall.getExtensionReceiver();
            int receiverIndex = -1;
            if (receiverParameter != null) {
                Type type = codegen.typeMapper.mapType(receiverParameter.getType());
                receiverIndex = frame.enterTemp(type);
                v.store(receiverIndex, type);
            }

            ReceiverValue dispatchReceiver = resolvedGetCall.getDispatchReceiver();
            int thisIndex = -1;
            if (dispatchReceiver != null) {
                thisIndex = frame.enterTemp(OBJECT_TYPE);
                v.store(thisIndex, OBJECT_TYPE);
            }

            // for setter

            int realReceiverIndex;
            Type realReceiverType;
            if (receiverIndex != -1) {
                realReceiverType = codegen.typeMapper.mapType(receiverParameter.getType());
                realReceiverIndex = receiverIndex;
            }
            else if (thisIndex != -1) {
                realReceiverType = OBJECT_TYPE;
                realReceiverIndex = thisIndex;
            }
            else {
                throw new UnsupportedOperationException();
            }

            if (resolvedSetCall.getDispatchReceiver() != null) {
                if (resolvedSetCall.getExtensionReceiver() != null) {
                    codegen.generateReceiverValue(resolvedSetCall.getDispatchReceiver(), false).put(OBJECT_TYPE, v);
                }
                v.load(realReceiverIndex, realReceiverType);
            }
            else {
                if (resolvedSetCall.getExtensionReceiver() != null) {
                    v.load(realReceiverIndex, realReceiverType);
                }
                else {
                    throw new UnsupportedOperationException();
                }
            }

            int index = firstParamIndex;
            for (ValueParameterDescriptor valueParameter : valueParameters) {
                Type type = codegen.typeMapper.mapType(valueParameter.getType());
                v.load(index, type);
                index -= type.getSize();
            }

            // restoring original
            if (thisIndex != -1) {
                v.load(thisIndex, OBJECT_TYPE);
            }

            if (receiverIndex != -1) {
                v.load(receiverIndex, realReceiverType);
            }

            index = firstParamIndex;
            for (ValueParameterDescriptor valueParameter : valueParameters) {
                Type type = codegen.typeMapper.mapType(valueParameter.getType());
                v.load(index, type);
                index -= type.getSize();
            }

            mark.dropTo();
        }
    }

    public static class CollectionElement extends StackValueWithSimpleReceiver {
        private final Callable getter;
        private final Callable setter;
        private final ExpressionCodegen codegen;
        private final ResolvedCall<FunctionDescriptor> resolvedGetCall;
        private final ResolvedCall<FunctionDescriptor> resolvedSetCall;

        public CollectionElement(
                @NotNull CollectionElementReceiver collectionElementReceiver,
                @NotNull Type type,
                @Nullable ResolvedCall<FunctionDescriptor> resolvedGetCall,
                @Nullable ResolvedCall<FunctionDescriptor> resolvedSetCall,
                @NotNull ExpressionCodegen codegen
        ) {
            super(type, false, false, collectionElementReceiver, true);
            this.resolvedGetCall = resolvedGetCall;
            this.resolvedSetCall = resolvedSetCall;
            this.setter = resolvedSetCall == null ? null :
                          codegen.resolveToCallable(codegen.accessibleFunctionDescriptor(resolvedSetCall), false, resolvedSetCall);
            this.getter = resolvedGetCall == null ? null :
                          codegen.resolveToCallable(codegen.accessibleFunctionDescriptor(resolvedGetCall), false, resolvedGetCall);
            this.codegen = codegen;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            if (getter == null) {
                throw new UnsupportedOperationException("no getter specified");
            }
            CallGenerator callGenerator = getCallGenerator();
            callGenerator.genCall(getter, resolvedGetCall, genDefaultMaskIfPresent(callGenerator), codegen);
            coerceTo(type, v);
        }

        private boolean genDefaultMaskIfPresent(CallGenerator callGenerator) {
            DefaultCallArgs defaultArgs = ((CollectionElementReceiver) receiver).defaultArgs;
            return defaultArgs.generateOnStackIfNeeded(callGenerator, true);
        }

        private CallGenerator getCallGenerator() {
            CallGenerator generator = ((CollectionElementReceiver) receiver).callGenerator;
            assert generator != null :
                    "CollectionElementReceiver should be putted on stack before CollectionElement:" +
                    " getCall = " + resolvedGetCall + ",  setCall = " + resolvedSetCall;
            return generator;
        }

        @Override
        public int receiverSize() {
            if (isStandardStack(codegen.typeMapper, resolvedGetCall, 1) && isStandardStack(codegen.typeMapper, resolvedSetCall, 2)) {
                return 2;
            }
            else {
                return -1;
            }
        }

        public static boolean isStandardStack(@NotNull KotlinTypeMapper typeMapper, @Nullable ResolvedCall<?> call, int valueParamsSize) {
            if (call == null) {
                return true;
            }

            List<ValueParameterDescriptor> valueParameters = call.getResultingDescriptor().getValueParameters();
            if (valueParameters.size() != valueParamsSize) {
                return false;
            }

            for (ValueParameterDescriptor valueParameter : valueParameters) {
                if (typeMapper.mapType(valueParameter.getType()).getSize() != 1) {
                    return false;
                }
            }

            if (call.getDispatchReceiver() != null) {
                if (call.getExtensionReceiver() != null) {
                    return false;
                }
            }
            else {
                //noinspection ConstantConditions
                if (typeMapper.mapType(call.getResultingDescriptor().getExtensionReceiverParameter().getType()).getSize() != 1) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            if (setter == null) {
                throw new UnsupportedOperationException("no setter specified");
            }

            Type lastParameterType = ArraysKt.last(setter.getParameterTypes());
            coerce(topOfStackType, lastParameterType, v);

            getCallGenerator().putValueIfNeeded(lastParameterType, StackValue.onStack(lastParameterType));

            //Convention setter couldn't have default parameters, just getter can have it at last positions
            //We should remove default parameters of getter from stack*/
            //Note that it works only for non-inline case
            CollectionElementReceiver collectionElementReceiver = (CollectionElementReceiver) receiver;
            if (collectionElementReceiver.isGetter) {
                List<ResolvedValueArgument> arguments = collectionElementReceiver.valueArguments;
                List<Type> types = getter.getValueParameterTypes();
                for (int i = arguments.size() - 1; i >= 0; i--) {
                    ResolvedValueArgument argument = arguments.get(i);
                    if (argument instanceof DefaultValueArgument) {
                        Type defaultType = types.get(i);
                        AsmUtil.swap(v, lastParameterType, defaultType);
                        AsmUtil.pop(v, defaultType);
                    }
                }
            }

            getCallGenerator().genCall(setter, resolvedSetCall, false, codegen);
            Type returnType = setter.getReturnType();
            if (returnType != Type.VOID_TYPE) {
                pop(v, returnType);
            }
        }
    }


    public static class Field extends StackValueWithSimpleReceiver {
        public final Type owner;
        public final String name;
        public final DeclarationDescriptor descriptor;

        public Field(
                @NotNull Type type,
                @NotNull Type owner,
                @NotNull String name,
                boolean isStatic,
                @NotNull StackValue receiver,
                @Nullable DeclarationDescriptor descriptor
        ) {
            super(type, isStatic, isStatic, receiver, receiver.canHaveSideEffects());
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            v.visitFieldInsn(isStaticPut ? GETSTATIC : GETFIELD, owner.getInternalName(), name, this.type.getDescriptor());
            coerceTo(type, v);
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.visitFieldInsn(isStaticStore ? PUTSTATIC : PUTFIELD, owner.getInternalName(), name, this.type.getDescriptor());
        }

        @Override
        protected StackValueWithSimpleReceiver changeReceiver(@NotNull StackValue newReceiver) {
            return field(this, newReceiver);
        }
    }

    public static class Property extends StackValueWithSimpleReceiver {
        private final CallableMethod getter;
        private final CallableMethod setter;
        private final Type backingFieldOwner;
        private final PropertyDescriptor descriptor;
        private final String fieldName;
        private final ExpressionCodegen codegen;
        private final ResolvedCall resolvedCall;
        private final boolean skipLateinitAssertion;

        public Property(
                @NotNull PropertyDescriptor descriptor, @Nullable Type backingFieldOwner, @Nullable CallableMethod getter,
                @Nullable CallableMethod setter, boolean isStaticBackingField, @Nullable String fieldName, @NotNull Type type,
                @NotNull StackValue receiver, @NotNull ExpressionCodegen codegen, @Nullable ResolvedCall resolvedCall,
                boolean skipLateinitAssertion
        ) {
            super(type, isStatic(isStaticBackingField, getter), isStatic(isStaticBackingField, setter), receiver, true);
            this.backingFieldOwner = backingFieldOwner;
            this.getter = getter;
            this.setter = setter;
            this.descriptor = descriptor;
            this.fieldName = fieldName;
            this.codegen = codegen;
            this.resolvedCall = resolvedCall;
            this.skipLateinitAssertion = skipLateinitAssertion;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            if (getter == null) {
                assert fieldName != null : "Property should have either a getter or a field name: " + descriptor;
                assert backingFieldOwner != null : "Property should have either a getter or a backingFieldOwner: " + descriptor;
                if (inlineConstantIfNeeded(type, v)) return;

                v.visitFieldInsn(isStaticPut ? GETSTATIC : GETFIELD,
                                 backingFieldOwner.getInternalName(), fieldName, this.type.getDescriptor());
                if (!skipLateinitAssertion) {
                    genNotNullAssertionForLateInitIfNeeded(v);
                }
                coerceTo(type, v);
            }
            else {
                PropertyGetterDescriptor getterDescriptor = descriptor.getGetter();
                assert getterDescriptor != null : "Getter descriptor should be not null for " + descriptor;
                if (resolvedCall != null && getterDescriptor.isInline()) {
                    CallGenerator callGenerator = codegen.getOrCreateCallGenerator(resolvedCall, getterDescriptor);
                    callGenerator.processAndPutHiddenParameters(false);
                    callGenerator.genCall(getter, resolvedCall, false, codegen);
                }
                else {
                    getter.genInvokeInstruction(v);
                }

                Type typeOfValueOnStack = getter.getReturnType();
                if (DescriptorUtils.isAnnotationClass(descriptor.getContainingDeclaration())) {
                    if (this.type.equals(K_CLASS_TYPE)) {
                        wrapJavaClassIntoKClass(v);
                        typeOfValueOnStack = K_CLASS_TYPE;
                    }
                    else if (this.type.equals(K_CLASS_ARRAY_TYPE)) {
                        wrapJavaClassesIntoKClasses(v);
                        typeOfValueOnStack = K_CLASS_ARRAY_TYPE;
                    }
                }

                coerce(typeOfValueOnStack, type, v);

                KotlinType returnType = descriptor.getReturnType();
                if (returnType != null && KotlinBuiltIns.isNothing(returnType)) {
                    v.aconst(null);
                    v.athrow();
                }
            }
        }

        private boolean inlineConstantIfNeeded(@NotNull Type type, @NotNull InstructionAdapter v) {
            if (JvmCodegenUtil.isInlinedJavaConstProperty(descriptor)) {
                return inlineConstant(type, v);
            }

            if (descriptor.isConst() && codegen.getState().getShouldInlineConstVals()) {
                return inlineConstant(type, v);
            }

            return false;
        }

        private boolean inlineConstant(@NotNull Type type, @NotNull InstructionAdapter v) {
            assert AsmUtil.isPrimitive(this.type) || AsmTypes.JAVA_STRING_TYPE.equals(this.type) :
                    "Const property should have primitive or string type: " + descriptor;
            assert isStaticPut : "Const property should be static" + descriptor;

            ConstantValue<?> constantValue = descriptor.getCompileTimeInitializer();
            if (constantValue == null) return false;

            Object value = constantValue.getValue();
            if (this.type == Type.FLOAT_TYPE && value instanceof Double) {
                value = ((Double) value).floatValue();
            }

            StackValue.constant(value, this.type).putSelector(type, v);

            return true;
        }

        private void genNotNullAssertionForLateInitIfNeeded(@NotNull InstructionAdapter v) {
            if (!descriptor.isLateInit()) return;

            StackValue.genNonNullAssertForLateinit(v, descriptor.getName().asString());
        }

        @Override
        public void store(@NotNull StackValue rightSide, @NotNull InstructionAdapter v, boolean skipReceiver) {
            PropertySetterDescriptor setterDescriptor = descriptor.getSetter();
            if (resolvedCall != null && setterDescriptor != null && setterDescriptor.isInline()) {
                assert setter != null : "Setter should be not null for " + descriptor;
                CallGenerator callGenerator = codegen.getOrCreateCallGenerator(resolvedCall, setterDescriptor);
                if (!skipReceiver) {
                    putReceiver(v, false);
                }
                callGenerator.processAndPutHiddenParameters(true);
                callGenerator.putValueIfNeeded(rightSide.type, rightSide);
                callGenerator.putHiddenParamsIntoLocals();
                callGenerator.genCall(setter, resolvedCall, false, codegen);
            }
            else {
                super.store(rightSide, v, skipReceiver);
            }
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            if (setter == null) {
                coerceFrom(topOfStackType, v);
                assert fieldName != null : "Property should have either a setter or a field name: " + descriptor;
                assert backingFieldOwner != null : "Property should have either a setter or a backingFieldOwner: " + descriptor;
                v.visitFieldInsn(isStaticStore ? PUTSTATIC : PUTFIELD, backingFieldOwner.getInternalName(), fieldName, this.type.getDescriptor());
            }
            else {
                coerce(topOfStackType, ArraysKt.last(setter.getParameterTypes()), v);
                setter.genInvokeInstruction(v);

                Type returnType = setter.getReturnType();
                if (returnType != Type.VOID_TYPE) {
                    pop(v, returnType);
                }
            }
        }

        private static boolean isStatic(boolean isStaticBackingField, @Nullable CallableMethod callable) {
            if (isStaticBackingField && callable == null) {
                return true;
            }

            if (callable != null && callable.isStaticCall()) {
                List<JvmMethodParameterSignature> parameters = callable.getValueParameters();
                for (JvmMethodParameterSignature parameter : parameters) {
                    JvmMethodParameterKind kind = parameter.getKind();
                    if (kind == JvmMethodParameterKind.VALUE) {
                        break;
                    }
                    if (kind == JvmMethodParameterKind.RECEIVER || kind == JvmMethodParameterKind.THIS) {
                        return false;
                    }
                }
                return true;
            }

            return false;
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

    private static class Expression extends StackValue {
        private final KtExpression expression;
        private final ExpressionCodegen generator;

        public Expression(Type type, KtExpression expression, ExpressionCodegen generator) {
            super(type);
            this.expression = expression;
            this.generator = generator;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            generator.gen(expression, type);
        }
    }

    public static class Shared extends StackValueWithSimpleReceiver {
        private final int index;
        private final boolean isLateinit;
        private final Name name;

        public Shared(int index, Type type, boolean isLateinit, Name name) {
            super(type, false, false, local(index, OBJECT_TYPE), false);
            this.index = index;

            if (isLateinit && name == null) {
                throw new IllegalArgumentException("Lateinit shared local variable should have name: #" + index + " " + type.getDescriptor());
            }

            this.isLateinit = isLateinit;
            this.name = name;
        }

        public Shared(int index, Type type) {
            this(index, type, false, null);
        }

        public int getIndex() {
            return index;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            Type refType = refType(this.type);
            Type sharedType = sharedTypeForType(this.type);
            v.visitFieldInsn(GETFIELD, sharedType.getInternalName(), "element", refType.getDescriptor());
            if (isLateinit) {
                StackValue.genNonNullAssertForLateinit(v, name.asString());
            }
            coerceFrom(refType, v);
            coerceTo(type, v);
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
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

    public static class FieldForSharedVar extends StackValueWithSimpleReceiver {
        final Type owner;
        final String name;
        final boolean isLateinit;
        final Name variableName;

        public FieldForSharedVar(
                Type type, Type owner, String name, StackValue.Field receiver,
                boolean isLateinit, Name variableName
        ) {
            super(type, false, false, receiver, receiver.canHaveSideEffects());

            if (isLateinit && variableName == null) {
                throw new IllegalArgumentException("variableName should be non-null for captured lateinit variable " + name);
            }

            this.owner = owner;
            this.name = name;
            this.isLateinit = isLateinit;
            this.variableName = variableName;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            Type sharedType = sharedTypeForType(this.type);
            Type refType = refType(this.type);
            v.visitFieldInsn(GETFIELD, sharedType.getInternalName(), "element", refType.getDescriptor());
            if (isLateinit) {
                StackValue.genNonNullAssertForLateinit(v, variableName.asString());
            }
            coerceFrom(refType, v);
            coerceTo(type, v);
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.visitFieldInsn(PUTFIELD, sharedTypeForType(type).getInternalName(), "element", refType(type).getDescriptor());
        }

        @Override
        protected StackValueWithSimpleReceiver changeReceiver(@NotNull StackValue newReceiver) {
            return fieldForSharedVar(this, newReceiver);
        }
    }

    private static class ThisOuter extends StackValue {
        private final ExpressionCodegen codegen;
        private final ClassDescriptor descriptor;
        private final boolean isSuper;
        private final boolean coerceType;

        public ThisOuter(ExpressionCodegen codegen, ClassDescriptor descriptor, boolean isSuper, boolean coerceType) {
            super(OBJECT_TYPE, false);
            this.codegen = codegen;
            this.descriptor = descriptor;
            this.isSuper = isSuper;
            this.coerceType = coerceType;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            StackValue stackValue = codegen.generateThisOrOuter(descriptor, isSuper);
            stackValue.put(coerceType ? type : stackValue.type, v);
        }
    }

    private static class PostIncrement extends StackValue {
        private final int index;
        private final int increment;

        public PostIncrement(int index, int increment) {
            super(Type.INT_TYPE);
            this.index = index;
            this.increment = increment;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            if (!type.equals(Type.VOID_TYPE)) {
                v.load(index, Type.INT_TYPE);
                coerceTo(type, v);
            }
            v.iinc(index, increment);
        }
    }

    private static class PreIncrementForLocalVar extends StackValue {
        private final int index;
        private final int increment;

        public PreIncrementForLocalVar(int index, int increment) {
            super(Type.INT_TYPE);
            this.index = index;
            this.increment = increment;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            v.iinc(index, increment);
            if (!type.equals(Type.VOID_TYPE)) {
                v.load(index, Type.INT_TYPE);
                coerceTo(type, v);
            }
        }
    }

    private static class PrefixIncrement extends StackValue {
        private final ResolvedCall resolvedCall;
        private final ExpressionCodegen codegen;
        private StackValue value;

        public PrefixIncrement(
                @NotNull Type type,
                @NotNull StackValue value,
                ResolvedCall resolvedCall,
                @NotNull ExpressionCodegen codegen
        ) {
            super(type);
            this.value = value;
            this.resolvedCall = resolvedCall;
            this.codegen = codegen;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            value = StackValue.complexReceiver(value, true, false, true);
            value.put(this.type, v);

            value.store(codegen.invokeFunction(resolvedCall, StackValue.onStack(this.type)), v, true);

            value.put(this.type, v, true);
            coerceTo(type, v);
        }
    }

    public abstract static class StackValueWithSimpleReceiver extends StackValue {

        public final boolean isStaticPut;

        public final boolean isStaticStore;
        @NotNull
        public final StackValue receiver;

        public StackValueWithSimpleReceiver(
                @NotNull Type type,
                boolean isStaticPut,
                boolean isStaticStore,
                @NotNull StackValue receiver,
                boolean canHaveSideEffects
        ) {
            super(type, canHaveSideEffects);
            this.receiver = receiver;
            this.isStaticPut = isStaticPut;
            this.isStaticStore = isStaticStore;
        }

        @Override
        public void putReceiver(@NotNull InstructionAdapter v, boolean isRead) {
            boolean hasReceiver = isNonStaticAccess(isRead);
            if (hasReceiver || receiver.canHaveSideEffects()) {
                receiver.put(hasReceiver ? receiver.type : Type.VOID_TYPE, v);
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
            rightSide.put(rightSide.type, v);
            storeSelector(rightSide.type, v);
        }

        protected StackValueWithSimpleReceiver changeReceiver(@NotNull StackValue newReceiver) {
            return this;
        }
    }

    private static class ComplexReceiver extends StackValue {

        private final StackValueWithSimpleReceiver originalValueWithReceiver;
        private final boolean[] isReadOperations;

        public ComplexReceiver(StackValueWithSimpleReceiver value, boolean[] isReadOperations) {
            super(value.type, value.receiver.canHaveSideEffects());
            this.originalValueWithReceiver = value;
            this.isReadOperations = isReadOperations;
            if (value instanceof CollectionElement) {
                if (value.receiver instanceof CollectionElementReceiver) {
                    ((CollectionElementReceiver) value.receiver).isComplexOperationWithDup = true;
                }
            }
        }

        @Override
        public void putSelector(
                @NotNull Type type, @NotNull InstructionAdapter v
        ) {
            boolean wasPut = false;
            StackValue receiver = originalValueWithReceiver.receiver;
            for (boolean operation : isReadOperations) {
                if (originalValueWithReceiver.isNonStaticAccess(operation)) {
                    if (!wasPut) {
                        receiver.put(receiver.type, v);
                        wasPut = true;
                    }
                    else {
                        receiver.dup(v, false);
                    }
                }
            }

            if (!wasPut && receiver.canHaveSideEffects()) {
                receiver.put(Type.VOID_TYPE, v);
            }
        }
    }

    public static class Receiver extends StackValue {

        private final StackValue[] instructions;

        protected Receiver(@NotNull Type type, StackValue... receiverInstructions) {
            super(type);
            instructions = receiverInstructions;
        }

        @Override
        public void putSelector(
                @NotNull Type type, @NotNull InstructionAdapter v
        ) {
            for (StackValue instruction : instructions) {
                instruction.put(instruction.type, v);
            }
        }
    }

    public static class DelegatedForComplexReceiver extends StackValueWithSimpleReceiver {

        public final StackValueWithSimpleReceiver originalValue;

        public DelegatedForComplexReceiver(
                @NotNull Type type,
                @NotNull StackValueWithSimpleReceiver originalValue,
                @NotNull ComplexReceiver receiver
        ) {
            super(type, bothReceiverStatic(originalValue), bothReceiverStatic(originalValue), receiver, originalValue.canHaveSideEffects());
            this.originalValue = originalValue;
        }

        private static boolean bothReceiverStatic(StackValueWithSimpleReceiver originalValue) {
            return !(originalValue.isNonStaticAccess(true) || originalValue.isNonStaticAccess(false));
        }

        @Override
        public void putSelector(
                @NotNull Type type, @NotNull InstructionAdapter v
        ) {
            originalValue.putSelector(type, v);
        }

        @Override
        public void store(@NotNull StackValue rightSide, @NotNull InstructionAdapter v, boolean skipReceiver) {
            if (!skipReceiver) {
                putReceiver(v, false);
            }
            originalValue.store(rightSide, v, true);
        }

        @Override
        public void storeSelector(
                @NotNull Type topOfStackType, @NotNull InstructionAdapter v
        ) {
            originalValue.storeSelector(topOfStackType, v);
        }

        @Override
        public void dup(@NotNull InstructionAdapter v, boolean withWriteReceiver) {
            originalValue.dup(v, withWriteReceiver);
        }
    }

    public static StackValue complexWriteReadReceiver(StackValue stackValue) {
        return complexReceiver(stackValue, false, true);
    }

    private static StackValue complexReceiver(StackValue stackValue, boolean... isReadOperations) {
        if (stackValue instanceof Delegate) {
            //TODO need to support
            throwUnsupportedComplexOperation(((Delegate) stackValue).variableDescriptor);
        }

        if (stackValue instanceof StackValueWithSimpleReceiver) {
            return new DelegatedForComplexReceiver(stackValue.type, (StackValueWithSimpleReceiver) stackValue,
                                 new ComplexReceiver((StackValueWithSimpleReceiver) stackValue, isReadOperations));
        }
        else {
            return stackValue;
        }
    }

    static class SafeCall extends StackValue {

        @NotNull private final Type type;
        private final StackValue receiver;
        @Nullable private final Label ifNull;

        public SafeCall(@NotNull Type type, @NotNull StackValue value, @Nullable Label ifNull) {
            super(type);
            this.type = type;
            this.receiver = value;
            this.ifNull = ifNull;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            receiver.put(this.type, v);
            if (ifNull != null) {
                //not a primitive
                v.dup();
                v.ifnull(ifNull);
            }
            coerceTo(type, v);
        }
    }

    static class SafeFallback extends StackValueWithSimpleReceiver {

        @Nullable private final Label ifNull;

        public SafeFallback(@NotNull Type type, @Nullable Label ifNull, StackValue receiver) {
            super(type, false, false, receiver, true);
            this.ifNull = ifNull;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            Label end = new Label();

            v.goTo(end);
            v.mark(ifNull);
            v.pop();
            if (!this.type.equals(Type.VOID_TYPE)) {
                v.aconst(null);
            }
            v.mark(end);

            coerceTo(type, v);
        }

        @Override
        public void store(
                @NotNull StackValue rightSide, @NotNull InstructionAdapter v, boolean skipReceiver
        ) {
            receiver.store(rightSide, v, skipReceiver);

            Label end = new Label();
            v.goTo(end);
            v.mark(ifNull);
            v.pop();
            v.mark(end);
        }
    }

    private static void throwUnsupportedComplexOperation(
            @NotNull CallableDescriptor descriptor
    ) {
        throw new RuntimeException(
                "Augment assignment and increment are not supported for local delegated properties ans inline properties: " +
                descriptor);
    }
}

