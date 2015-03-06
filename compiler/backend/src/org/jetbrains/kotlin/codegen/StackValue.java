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

import com.intellij.psi.tree.IElementType;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethod;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.psi.JetArrayAccessExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.resolve.annotations.AnnotationsPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public abstract class StackValue {

    private static final String NULLABLE_BYTE_TYPE_NAME = "java/lang/Byte";
    private static final String NULLABLE_SHORT_TYPE_NAME = "java/lang/Short";
    private static final String NULLABLE_LONG_TYPE_NAME = "java/lang/Long";

    public static final StackValue.Local LOCAL_0 = local(0, OBJECT_TYPE);
    private static final StackValue UNIT = operation(UNIT_TYPE, new Function1<InstructionAdapter, Unit>() {
        @Override
        public Unit invoke(InstructionAdapter v) {
            v.visitFieldInsn(GETSTATIC, UNIT_TYPE.getInternalName(), JvmAbi.INSTANCE_FIELD, UNIT_TYPE.getDescriptor());
            return null;
        }
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

    public void condJump(@NotNull Label label, boolean jumpIfFalse, @NotNull InstructionAdapter v) {
        put(this.type, v);
        coerceTo(Type.BOOLEAN_TYPE, v);
        if (jumpIfFalse) {
            v.ifeq(label);
        }
        else {
            v.ifne(label);
        }
    }

    @NotNull
    public static Local local(int index, @NotNull Type type) {
        return new Local(index, type);
    }

    @NotNull
    public static StackValue shared(int index, @NotNull Type type) {
        return new Shared(index, type);
    }

    @NotNull
    public static StackValue onStack(@NotNull Type type) {
        return type == Type.VOID_TYPE ? none() : new OnStack(type);
    }

    @NotNull
    public static StackValue constant(@Nullable Object value, @NotNull Type type) {
        return new Constant(value, type);
    }

    @NotNull
    public static StackValue cmp(@NotNull IElementType opToken, @NotNull Type type, StackValue left, StackValue right) {
        return type.getSort() == Type.OBJECT ? new ObjectCompare(opToken, type, left, right) : new NumberCompare(opToken, type, left, right);
    }

    @NotNull
    public static StackValue not(@NotNull StackValue stackValue) {
        return new Invert(stackValue);
    }

    @NotNull
    public static StackValue arrayElement(@NotNull Type type, StackValue array, StackValue index) {
        return new ArrayElement(type, array, index);
    }

    @NotNull
    public static StackValue collectionElement(
            StackValue collectionElementReceiver,
            Type type,
            ResolvedCall<FunctionDescriptor> getter,
            ResolvedCall<FunctionDescriptor> setter,
            ExpressionCodegen codegen,
            GenerationState state
    ) {
        return new CollectionElement(collectionElementReceiver, type, getter, setter, codegen, state);
    }

    @NotNull
    public static Field field(@NotNull Type type, @NotNull Type owner, @NotNull String name, boolean isStatic, @NotNull StackValue receiver) {
        return new Field(type, owner, name, isStatic, receiver);
    }

    @NotNull
    public static Field field(@NotNull StackValue.Field field, @NotNull StackValue newReceiver) {
        return new Field(field.type, field.owner, field.name, field.isStaticPut, newReceiver);
    }

    @NotNull
    private static Field field(@NotNull FieldInfo info) {
        return field(info.getFieldType(), Type.getObjectType(info.getOwnerInternalName()), info.getFieldName(), true, none());
    }

    @NotNull
    public static StackValue changeReceiverForFieldAndSharedVar(@NotNull StackValueWithSimpleReceiver stackValue, @Nullable StackValue newReceiver) {
        //TODO static check
        if (newReceiver != null) {
            if (!stackValue.isStaticPut) {
                if (stackValue instanceof Field) {
                    return field((Field) stackValue, newReceiver);
                }
                else if (stackValue instanceof FieldForSharedVar) {
                    return fieldForSharedVar((FieldForSharedVar) stackValue, newReceiver);
                }
            }
        }
        return stackValue;
    }

    @NotNull
    public static Property property(
            @NotNull PropertyDescriptor descriptor,
            @NotNull Type backingFieldOwner,
            @NotNull Type type,
            boolean isStaticBackingField,
            @Nullable String fieldName,
            @Nullable CallableMethod getter,
            @Nullable CallableMethod setter,
            GenerationState state,
            @NotNull StackValue receiver
    ) {
        return new Property(descriptor, backingFieldOwner, getter, setter, isStaticBackingField, fieldName, type, state, receiver);
    }

    @NotNull
    public static StackValue expression(Type type, JetExpression expression, ExpressionCodegen generator) {
        return new Expression(type, expression, generator);
    }

    private static void box(Type type, Type toType, InstructionAdapter v) {
        if (type == Type.BYTE_TYPE || toType.getInternalName().equals(NULLABLE_BYTE_TYPE_NAME) && type == Type.INT_TYPE) {
            v.cast(type, Type.BYTE_TYPE);
            v.invokestatic(NULLABLE_BYTE_TYPE_NAME, "valueOf", "(B)L" + NULLABLE_BYTE_TYPE_NAME + ";", false);
        }
        else if (type == Type.SHORT_TYPE || toType.getInternalName().equals(NULLABLE_SHORT_TYPE_NAME) && type == Type.INT_TYPE) {
            v.cast(type, Type.SHORT_TYPE);
            v.invokestatic(NULLABLE_SHORT_TYPE_NAME, "valueOf", "(S)L" + NULLABLE_SHORT_TYPE_NAME + ";", false);
        }
        else if (type == Type.LONG_TYPE || toType.getInternalName().equals(NULLABLE_LONG_TYPE_NAME) && type == Type.INT_TYPE) {
            v.cast(type, Type.LONG_TYPE);
            v.invokestatic(NULLABLE_LONG_TYPE_NAME, "valueOf", "(J)L" + NULLABLE_LONG_TYPE_NAME + ";", false);
        }
        else if (type == Type.INT_TYPE) {
            v.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        }
        else if (type == Type.BOOLEAN_TYPE) {
            v.invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        }
        else if (type == Type.CHAR_TYPE) {
            v.invokestatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
        }
        else if (type == Type.FLOAT_TYPE) {
            v.invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        }
    }

    private static void unbox(Type type, InstructionAdapter v) {
        if (type == Type.INT_TYPE) {
            v.invokevirtual("java/lang/Number", "intValue", "()I", false);
        }
        else if (type == Type.BOOLEAN_TYPE) {
            v.invokevirtual("java/lang/Boolean", "booleanValue", "()Z", false);
        }
        else if (type == Type.CHAR_TYPE) {
            v.invokevirtual("java/lang/Character", "charValue", "()C", false);
        }
        else if (type == Type.SHORT_TYPE) {
            v.invokevirtual("java/lang/Number", "shortValue", "()S", false);
        }
        else if (type == Type.LONG_TYPE) {
            v.invokevirtual("java/lang/Number", "longValue", "()J", false);
        }
        else if (type == Type.BYTE_TYPE) {
            v.invokevirtual("java/lang/Number", "byteValue", "()B", false);
        }
        else if (type == Type.FLOAT_TYPE) {
            v.invokevirtual("java/lang/Number", "floatValue", "()F", false);
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.invokevirtual("java/lang/Number", "doubleValue", "()D", false);
        }
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
            if (fromType.equals(getType(Boolean.class)) || fromType.equals(getType(Character.class))) {
                unbox(unboxType(fromType), v);
                coerce(unboxType(fromType), toType, v);
            }
            else {
                if (toType.getSort() == Type.BOOLEAN || toType.getSort() == Type.CHAR) {
                    coerce(fromType, boxType(toType), v);
                }
                else {
                    coerce(fromType, getType(Number.class), v);
                }
                unbox(toType, v);
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

    public void putAsBoolean(InstructionAdapter v) {
        Label ifTrue = new Label();
        Label end = new Label();
        condJump(ifTrue, false, v);
        v.iconst(0);
        v.goTo(end);
        v.mark(ifTrue);
        v.iconst(1);
        v.mark(end);
    }

    public static StackValue none() {
        return None.INSTANCE;
    }

    public static FieldForSharedVar fieldForSharedVar(@NotNull Type localType, @NotNull Type classType, @NotNull String fieldName, @NotNull StackValue receiver) {
        Field receiverWithRefWrapper = field(sharedTypeForType(localType), classType, fieldName, false, receiver);
        return new FieldForSharedVar(localType, classType, fieldName, receiverWithRefWrapper);
    }

    @NotNull
    public static FieldForSharedVar fieldForSharedVar(@NotNull FieldForSharedVar field, @NotNull StackValue newReceiver) {
        Field oldReceiver = (Field) field.receiver;
        Field newSharedVarReceiver = field(oldReceiver, newReceiver);
        return new FieldForSharedVar(field.type, field.owner, field.name, newSharedVarReceiver);
    }

    public static StackValue coercion(@NotNull StackValue value, @NotNull Type castType) {
        if (value.type.equals(castType)) {
            return value;
        }
        return new CoercionValue(value, castType);
    }

    public static StackValue thisOrOuter(@NotNull ExpressionCodegen codegen, @NotNull ClassDescriptor descriptor, boolean isSuper, boolean isExplicit) {
        // Coerce this/super for traits to support traits with required classes.
        // Coerce explicit 'this' for the case when it is smart cast.
        // Do not coerce for other classes due to the 'protected' access issues (JVMS 7, 4.9.2 Structural Constraints).
        boolean coerceType = descriptor.getKind() == ClassKind.TRAIT || (isExplicit && !isSuper);
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
            @NotNull Callable method,
            ResolvedCall resolvedCall,
            @NotNull ExpressionCodegen codegen
    ) {
        return new PrefixIncrement(type, stackValue, delta, method, resolvedCall, codegen);
    }

    public static StackValue receiver(
            ResolvedCall<?> resolvedCall,
            StackValue receiver,
            ExpressionCodegen codegen,
            @Nullable CallableMethod callableMethod
    ) {
        if (resolvedCall.getDispatchReceiver().exists() || resolvedCall.getExtensionReceiver().exists() || isLocalFunCall(callableMethod)) {
            boolean hasExtensionReceiver = resolvedCall.getExtensionReceiver().exists();
            StackValue dispatchReceiver = platformStaticCallIfPresent(
                    genReceiver(hasExtensionReceiver ? none() : receiver, codegen, resolvedCall, callableMethod, false),
                    resolvedCall.getResultingDescriptor()
            );
            StackValue extensionReceiver = genReceiver(receiver, codegen, resolvedCall, callableMethod, true);
            return new CallReceiver(dispatchReceiver, extensionReceiver,
                                    CallReceiver.calcType(resolvedCall, codegen.typeMapper, callableMethod));
        }
        return receiver;
    }

    private static StackValue genReceiver(
            @NotNull StackValue receiver,
            @NotNull ExpressionCodegen codegen,
            @NotNull ResolvedCall resolvedCall,
            @Nullable CallableMethod callableMethod,
            boolean isExtension
    ) {
        ReceiverValue receiverValue = isExtension ? resolvedCall.getExtensionReceiver() : resolvedCall.getDispatchReceiver();
        if (receiver == none()) {
            if (receiverValue.exists()) {
                return codegen.generateReceiverValue(receiverValue);
            }
            else if (isLocalFunCall(callableMethod) && !isExtension) {
                CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();
                StackValue value = codegen.findLocalOrCapturedValue(descriptor.getOriginal());
                assert value != null : "Local fun should be found in locals or in captured params: " + resolvedCall;
                return value;
            }
        }
        else if (receiverValue.exists()) {
            return receiver;
        }
        return none();
    }

    private static StackValue platformStaticCallIfPresent(@NotNull StackValue resultReceiver, @NotNull CallableDescriptor descriptor) {
        if (AnnotationsPackage.isPlatformStaticInObjectOrClass(descriptor)) {
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
    private static boolean isLocalFunCall(@Nullable CallableMethod callableMethod) {
        return callableMethod != null && callableMethod.getGenerateCalleeType() != null;
    }

    public static StackValue receiverWithoutReceiverArgument(StackValue receiverWithParameter) {
        if (receiverWithParameter instanceof CallReceiver) {
            CallReceiver callReceiver = (CallReceiver) receiverWithParameter;
            return new CallReceiver(callReceiver.dispatchReceiver, none(), callReceiver.type);
        }
        return receiverWithParameter;
    }

    public static Field singleton(ClassDescriptor classDescriptor, JetTypeMapper typeMapper) {
        return field(FieldInfo.createForSingleton(classDescriptor, typeMapper));
    }

    public static Field deprecatedDefaultObjectAccessor(ClassDescriptor classDescriptor, JetTypeMapper typeMapper) {
        return field(FieldInfo.deprecatedFieldForDefaultObject(classDescriptor, typeMapper));
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

        private Local(int index, Type type) {
            super(type, false);
            this.index = index;

            if (index < 0) {
                throw new IllegalStateException("local variable index must be non-negative");
            }
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            v.load(index, this.type);
            coerceTo(type, v);
            // TODO unbox
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.store(index, this.type);
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
                } else if (size == 2) {
                    v.dupX2();
                    v.pop();
                } else {
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
        private final Object value;

        public Constant(@Nullable Object value, Type type) {
            super(type, false);
            this.value = value;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            if (value instanceof Integer) {
                v.iconst((Integer) value);
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

            coerceTo(type, v);
        }

        @Override
        public void condJump(@NotNull Label label, boolean jumpIfFalse, @NotNull InstructionAdapter v) {
            if (value instanceof Boolean) {
                boolean boolValue = (Boolean) value;
                if (boolValue ^ jumpIfFalse) {
                    v.goTo(label);
                }
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate this condjump");
            }
        }
    }

    private static class NumberCompare extends StackValue {
        protected final IElementType opToken;
        protected final Type operandType;
        protected final StackValue left;
        protected final StackValue right;

        public NumberCompare(IElementType opToken, Type operandType, StackValue left, StackValue right) {
            super(Type.BOOLEAN_TYPE);
            this.opToken = opToken;
            this.operandType = operandType;
            this.left = left;
            this.right = right;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            putAsBoolean(v);
            coerceTo(type, v);
        }

        @Override
        public void condJump(@NotNull Label label, boolean jumpIfFalse, @NotNull InstructionAdapter v) {
            left.put(this.operandType, v);
            right.put(this.operandType, v);
            int opcode;
            if (opToken == JetTokens.EQEQ || opToken == JetTokens.EQEQEQ) {
                opcode = jumpIfFalse ? IFNE : IFEQ;
            }
            else if (opToken == JetTokens.EXCLEQ || opToken == JetTokens.EXCLEQEQEQ) {
                opcode = jumpIfFalse ? IFEQ : IFNE;
            }
            else if (opToken == JetTokens.GT) {
                opcode = jumpIfFalse ? IFLE : IFGT;
            }
            else if (opToken == JetTokens.GTEQ) {
                opcode = jumpIfFalse ? IFLT : IFGE;
            }
            else if (opToken == JetTokens.LT) {
                opcode = jumpIfFalse ? IFGE : IFLT;
            }
            else if (opToken == JetTokens.LTEQ) {
                opcode = jumpIfFalse ? IFGT : IFLE;
            }
            else {
                throw new UnsupportedOperationException("Don't know how to generate this condJump: " + opToken);
            }
            if (operandType == Type.FLOAT_TYPE || operandType == Type.DOUBLE_TYPE) {
                if (opToken == JetTokens.GT || opToken == JetTokens.GTEQ) {
                    v.cmpl(operandType);
                }
                else {
                    v.cmpg(operandType);
                }
            }
            else if (operandType == Type.LONG_TYPE) {
                v.lcmp();
            }
            else {
                opcode += (IF_ICMPEQ - IFEQ);
            }
            v.visitJumpInsn(opcode, label);
        }
    }

    private static class ObjectCompare extends NumberCompare {
        public ObjectCompare(IElementType opToken, Type operandType, StackValue left, StackValue right) {
            super(opToken, operandType, left, right);
        }

        @Override
        public void condJump(@NotNull Label label, boolean jumpIfFalse, @NotNull InstructionAdapter v) {
            left.put(this.operandType, v);
            right.put(this.operandType, v);
            int opcode;
            if (opToken == JetTokens.EQEQEQ) {
                opcode = jumpIfFalse ? IF_ACMPNE : IF_ACMPEQ;
            }
            else if (opToken == JetTokens.EXCLEQEQEQ) {
                opcode = jumpIfFalse ? IF_ACMPEQ : IF_ACMPNE;
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate this condjump");
            }
            v.visitJumpInsn(opcode, label);
        }
    }

    private static class Invert extends StackValue {
        private final StackValue myOperand;

        private Invert(StackValue operand) {
            super(Type.BOOLEAN_TYPE);
            myOperand = operand;
            if (myOperand.type != Type.BOOLEAN_TYPE) {
                throw new UnsupportedOperationException("operand of ! must be boolean");
            }
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            putAsBoolean(v);
            coerceTo(type, v);
        }

        @Override
        public void condJump(@NotNull Label label, boolean jumpIfFalse, @NotNull InstructionAdapter v) {
            myOperand.condJump(label, !jumpIfFalse, v);
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
        private final JetExpression array;
        private final Type arrayType;
        private final JetArrayAccessExpression expression;
        private final Type[] argumentTypes;
        private final ArgumentGenerator argumentGenerator;
        private final List<ResolvedValueArgument> valueArguments;
        private final FrameMap frame;
        private final StackValue receiver;
        private final ResolvedCall<FunctionDescriptor> resolvedGetCall;
        private final ResolvedCall<FunctionDescriptor> resolvedSetCall;

        public CollectionElementReceiver(
                Callable callable,
                StackValue receiver,
                ResolvedCall<FunctionDescriptor> resolvedGetCall,
                ResolvedCall<FunctionDescriptor> resolvedSetCall,
                boolean isGetter,
                ExpressionCodegen codegen,
                ArgumentGenerator argumentGenerator,
                List<ResolvedValueArgument> valueArguments,
                JetExpression array,
                Type arrayType,
                JetArrayAccessExpression expression,
                Type[] argumentTypes
        ) {
            super(OBJECT_TYPE);
            this.callable = callable;

            this.isGetter = isGetter;
            this.receiver = receiver;
            this.resolvedGetCall = resolvedGetCall;
            this.resolvedSetCall = resolvedSetCall;
            this.argumentGenerator = argumentGenerator;
            this.valueArguments = valueArguments;
            this.codegen = codegen;
            this.array = array;
            this.arrayType = arrayType;
            this.expression = expression;
            this.argumentTypes = argumentTypes;
            this.frame = codegen.myFrameMap;
        }

        @Override
        public void putSelector(
                @NotNull Type type, @NotNull InstructionAdapter v
        ) {
            ResolvedCall<?> call = isGetter ? resolvedGetCall : resolvedSetCall;
            if (callable instanceof CallableMethod) {
                StackValue newReceiver = StackValue.receiver(call, receiver, codegen, (CallableMethod) callable);
                newReceiver.put(newReceiver.type, v);
                argumentGenerator.generate(valueArguments);
            }
            else {
                codegen.gen(array, arrayType); // intrinsic method

                int index = call.getExtensionReceiver().exists() ? 1 : 0;

                for (JetExpression jetExpression : expression.getIndexExpressions()) {
                    codegen.gen(jetExpression, argumentTypes[index]);
                    index++;
                }
            }
        }

        @Override
        public void dup(@NotNull InstructionAdapter v, boolean withReceiver) {
            dupReceiver(v);
        }

        public void dupReceiver(@NotNull InstructionAdapter v) {
            if (isStandardStack(resolvedGetCall, 1) && isStandardStack(resolvedSetCall, 2)) {
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
            if (receiverParameter.exists()) {
                Type type = codegen.typeMapper.mapType(receiverParameter.getType());
                receiverIndex = frame.enterTemp(type);
                v.store(receiverIndex, type);
            }

            ReceiverValue dispatchReceiver = resolvedGetCall.getDispatchReceiver();
            int thisIndex = -1;
            if (dispatchReceiver.exists()) {
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

            if (resolvedSetCall.getDispatchReceiver().exists()) {
                if (resolvedSetCall.getExtensionReceiver().exists()) {
                    codegen.generateReceiverValue(resolvedSetCall.getDispatchReceiver()).put(OBJECT_TYPE, v);
                }
                v.load(realReceiverIndex, realReceiverType);
            }
            else {
                if (resolvedSetCall.getExtensionReceiver().exists()) {
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

        private boolean isStandardStack(ResolvedCall<?> call, int valueParamsSize) {
            if (call == null) {
                return true;
            }

            List<ValueParameterDescriptor> valueParameters = call.getResultingDescriptor().getValueParameters();
            if (valueParameters.size() != valueParamsSize) {
                return false;
            }

            for (ValueParameterDescriptor valueParameter : valueParameters) {
                if (codegen.typeMapper.mapType(valueParameter.getType()).getSize() != 1) {
                    return false;
                }
            }

            if (call.getDispatchReceiver().exists()) {
                if (call.getExtensionReceiver().exists()) {
                    return false;
                }
            }
            else {
                if (codegen.typeMapper.mapType(call.getResultingDescriptor().getExtensionReceiverParameter().getType())
                            .getSize() != 1) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class CollectionElement extends StackValueWithSimpleReceiver {
        private final Callable getter;
        private final Callable setter;
        private final ExpressionCodegen codegen;
        private final GenerationState state;
        private final ResolvedCall<FunctionDescriptor> resolvedGetCall;
        private final ResolvedCall<FunctionDescriptor> resolvedSetCall;
        private final FunctionDescriptor setterDescriptor;
        private final FunctionDescriptor getterDescriptor;

        public CollectionElement(
                StackValue collectionElementReceiver,
                Type type,
                ResolvedCall<FunctionDescriptor> resolvedGetCall,
                ResolvedCall<FunctionDescriptor> resolvedSetCall,
                ExpressionCodegen codegen,
                GenerationState state
        ) {
            super(type, false, false, collectionElementReceiver, true);
            this.resolvedGetCall = resolvedGetCall;
            this.resolvedSetCall = resolvedSetCall;
            this.state = state;
            this.setterDescriptor = resolvedSetCall == null ? null : resolvedSetCall.getResultingDescriptor();
            this.getterDescriptor = resolvedGetCall == null ? null : resolvedGetCall.getResultingDescriptor();
            this.setter = resolvedSetCall == null ? null : codegen.resolveToCallable(setterDescriptor, false);
            this.getter = resolvedGetCall == null ? null : codegen.resolveToCallable(getterDescriptor, false);
            this.codegen = codegen;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            if (getter == null) {
                throw new UnsupportedOperationException("no getter specified");
            }
            if (getter instanceof CallableMethod) {
                ((CallableMethod) getter).invokeWithNotNullAssertion(v, state, resolvedGetCall);
            }
            else {
                StackValue result = ((IntrinsicMethod) getter).generate(codegen, this.type, null, Collections.<JetExpression>emptyList(), StackValue.none());
                result.put(result.type, v);
            }
            coerceTo(type, v);
        }

        @Override
        public int receiverSize() {
            if (isStandardStack(resolvedGetCall, 1) && isStandardStack(resolvedSetCall, 2)) {
                return 2;
            }
            else {
                return -1;
            }
        }

        private boolean isStandardStack(ResolvedCall<?> call, int valueParamsSize) {
            if (call == null) {
                return true;
            }

            List<ValueParameterDescriptor> valueParameters = call.getResultingDescriptor().getValueParameters();
            if (valueParameters.size() != valueParamsSize) {
                return false;
            }

            for (ValueParameterDescriptor valueParameter : valueParameters) {
                if (codegen.typeMapper.mapType(valueParameter.getType()).getSize() != 1) {
                    return false;
                }
            }

            if (call.getDispatchReceiver().exists()) {
                if (call.getExtensionReceiver().exists()) {
                    return false;
                }
            }
            else {
                if (codegen.typeMapper.mapType(call.getResultingDescriptor().getExtensionReceiverParameter().getType())
                            .getSize() != 1) {
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
            if (setter instanceof CallableMethod) {
                CallableMethod method = (CallableMethod) setter;
                Method asmMethod = method.getAsmMethod();
                Type[] argumentTypes = asmMethod.getArgumentTypes();
                coerce(topOfStackType, argumentTypes[argumentTypes.length - 1], v);
                method.invokeWithNotNullAssertion(v, state, resolvedSetCall);
                Type returnType = asmMethod.getReturnType();
                if (returnType != Type.VOID_TYPE) {
                    pop(v, returnType);
                }
            }
            else {
                //noinspection ConstantConditions
                StackValue result = ((IntrinsicMethod) setter).generate(codegen, null, null, Collections.<JetExpression>emptyList(), StackValue.none());
                result.put(result.type, v);
            }
        }
    }


    public static class Field extends StackValueWithSimpleReceiver {
        public final Type owner;
        public final String name;

        public Field(Type type, Type owner, String name, boolean isStatic, StackValue receiver) {
            super(type, isStatic, isStatic, receiver, receiver.canHaveSideEffects());
            this.owner = owner;
            this.name = name;
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
    }

    static class Property extends StackValueWithSimpleReceiver {
        private final CallableMethod getter;
        private final CallableMethod setter;
        private final Type backingFieldOwner;

        private final PropertyDescriptor descriptor;
        private final GenerationState state;

        private final String fieldName;

        public Property(
                @NotNull PropertyDescriptor descriptor, @NotNull Type backingFieldOwner,
                @Nullable CallableMethod getter, @Nullable CallableMethod setter, boolean isStaticBackingField,
                @Nullable String fieldName, @NotNull Type type, @NotNull GenerationState state,
                @NotNull StackValue receiver
        ) {
            super(type, isStatic(isStaticBackingField, getter), isStatic(isStaticBackingField, setter), receiver, true);
            this.backingFieldOwner = backingFieldOwner;
            this.getter = getter;
            this.setter = setter;
            this.descriptor = descriptor;
            this.state = state;
            this.fieldName = fieldName;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            if (getter == null) {
                assert fieldName != null : "Property should have either a getter or a field name: " + descriptor;
                v.visitFieldInsn(isStaticPut ? GETSTATIC : GETFIELD, backingFieldOwner.getInternalName(), fieldName, this.type.getDescriptor());
                genNotNullAssertionForField(v, state, descriptor);
                coerceTo(type, v);
            }
            else {
                getter.invokeWithoutAssertions(v);
                coerce(getter.getAsmMethod().getReturnType(), type, v);
            }
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            if (setter == null) {
                assert fieldName != null : "Property should have either a setter or a field name: " + descriptor;
                v.visitFieldInsn(isStaticStore ? PUTSTATIC : PUTFIELD, backingFieldOwner.getInternalName(), fieldName, this.type.getDescriptor());
            }
            else {
                setter.invokeWithoutAssertions(v);
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

    private static class Expression extends StackValue {
        private final JetExpression expression;
        private final ExpressionCodegen generator;

        public Expression(Type type, JetExpression expression, ExpressionCodegen generator) {
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

        public Shared(int index, Type type) {
            super(type, false, false, local(index, OBJECT_TYPE), false);
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            Type refType = refType(this.type);
            Type sharedType = sharedTypeForType(this.type);
            v.visitFieldInsn(GETFIELD, sharedType.getInternalName(), "element", refType.getDescriptor());
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

                String typeName = primitiveType.getTypeName().getIdentifier();
                return Type.getObjectType(REF_TYPE_PREFIX + typeName + "Ref");
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

        public FieldForSharedVar(Type type, Type owner, String name, StackValue.Field receiver) {
            super(type, false, false, receiver, receiver.canHaveSideEffects());
            this.owner = owner;
            this.name = name;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            Type sharedType = sharedTypeForType(this.type);
            Type refType = refType(this.type);
            v.visitFieldInsn(GETFIELD, sharedType.getInternalName(), "element", refType.getDescriptor());
            coerceFrom(refType, v);
            coerceTo(type, v);
        }

        @Override
        public void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.visitFieldInsn(PUTFIELD, sharedTypeForType(type).getInternalName(), "element", refType(type).getDescriptor());
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
        private final int delta;
        private final Callable callable;
        private final ResolvedCall resolvedCall;
        private final ExpressionCodegen codegen;
        private StackValue value;

        public PrefixIncrement(
                @NotNull Type type,
                @NotNull StackValue value,
                int delta,
                @NotNull Callable callable,
                ResolvedCall resolvedCall,
                @NotNull ExpressionCodegen codegen
        ) {
            super(type);
            this.value = value;
            this.delta = delta;
            this.callable = callable;
            this.resolvedCall = resolvedCall;
            this.codegen = codegen;
        }

        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            value = StackValue.complexReceiver(value, true, false, true);
            value.put(this.type, v);

            if (callable instanceof CallableMethod) {
                value.store(codegen.invokeFunction(resolvedCall, StackValue.onStack(this.type)), v, true);
            }
            else {
                genIncrement(this.type, delta, v);
                value.store(StackValue.onStack(this.type), v, true);
            }

            value.put(this.type, v, true);
            coerceTo(type, v);
        }
    }

    public static class CallReceiver extends StackValue {
        private final StackValue dispatchReceiver;
        private final StackValue extensionReceiver;

        public CallReceiver(
                @NotNull StackValue dispatchReceiver,
                @NotNull StackValue extensionReceiver,
                @NotNull Type type
        ) {
            super(type, dispatchReceiver.canHaveSideEffects() || extensionReceiver.canHaveSideEffects());
            this.dispatchReceiver = dispatchReceiver;
            this.extensionReceiver = extensionReceiver;
        }

        public static Type calcType(
                @NotNull ResolvedCall<?> resolvedCall,
                @NotNull JetTypeMapper typeMapper,
                @Nullable CallableMethod callableMethod
        ) {
            CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

            ReceiverParameterDescriptor dispatchReceiver = descriptor.getDispatchReceiverParameter();
            ReceiverParameterDescriptor extensionReceiver = descriptor.getExtensionReceiverParameter();

            if (extensionReceiver != null) {
                return callableMethod != null ? callableMethod.getReceiverClass() : typeMapper.mapType(extensionReceiver.getType());
            }
            else if (dispatchReceiver != null) {
                if (AnnotationsPackage.isPlatformStaticInObjectOrClass(descriptor)) {
                    return Type.VOID_TYPE;
                }
                else {
                    return callableMethod != null ? callableMethod.getThisType() : typeMapper.mapType(dispatchReceiver.getType());
                }
            }
            else if (isLocalFunCall(callableMethod)) {
                return callableMethod.getGenerateCalleeType();
            }

            return Type.VOID_TYPE;
        }


        @Override
        public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
            StackValue currentExtensionReceiver = extensionReceiver;
            boolean hasExtensionReceiver = extensionReceiver != none();
            if (extensionReceiver instanceof StackValue.SafeCall) {
                currentExtensionReceiver.put(currentExtensionReceiver.type, v);
                currentExtensionReceiver = StackValue.onStack(currentExtensionReceiver.type);
            }

            dispatchReceiver.put(hasExtensionReceiver ? dispatchReceiver.type : type, v);

            currentExtensionReceiver
                    .moveToTopOfStack(hasExtensionReceiver ? type : currentExtensionReceiver.type, v, dispatchReceiver.type.getSize());
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
    }

    private static class ComplexReceiver extends StackValue {

        private final StackValueWithSimpleReceiver originalValueWithReceiver;
        private final boolean[] isReadOperations;

        public ComplexReceiver(StackValueWithSimpleReceiver value, boolean[] isReadOperations) {
            super(value.type, value.receiver.canHaveSideEffects());
            this.originalValueWithReceiver = value;
            this.isReadOperations = isReadOperations;
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
}

