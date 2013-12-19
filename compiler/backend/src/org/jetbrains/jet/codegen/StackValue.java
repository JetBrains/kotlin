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

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethod;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.*;

public abstract class StackValue {

    private static final String NULLABLE_BYTE_TYPE_NAME = "java/lang/Byte";
    private static final String NULLABLE_SHORT_TYPE_NAME = "java/lang/Short";
    private static final String NULLABLE_LONG_TYPE_NAME = "java/lang/Long";

    @NotNull
    public final Type type;

    protected StackValue(@NotNull Type type) {
        this.type = type;
    }

    /**
     * Put this value to the top of the stack.
     */
    public abstract void put(Type type, InstructionAdapter v);

    /**
     * This method is called to put the value on the top of the JVM stack if <code>depth</code> other values have been put on the
     * JVM stack after this value was generated.
     *
     * @param type  the type as which the value should be put
     * @param v     the visitor used to genClassOrObject the instructions
     * @param depth the number of new values put onto the stack
     */
    protected void moveToTopOfStack(Type type, InstructionAdapter v, int depth) {
        put(type, v);
    }

    /**
     * Set this value from the top of the stack.
     */
    public void store(Type topOfStackType, InstructionAdapter v) {
        throw new UnsupportedOperationException("cannot store to value " + this);
    }

    public void dupReceiver(InstructionAdapter v) {
    }

    public int receiverSize() {
        return 0;
    }

    public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
        put(this.type, v);
        coerceTo(Type.BOOLEAN_TYPE, v);
        if (jumpIfFalse) {
            v.ifeq(label);
        }
        else {
            v.ifne(label);
        }
    }

    public static Local local(int index, Type type) {
        return new Local(index, type);
    }

    public static StackValue shared(int index, Type type) {
        return new Shared(index, type);
    }

    public static StackValue onStack(Type type) {
        return type == Type.VOID_TYPE ? none() : new OnStack(type);
    }

    public static StackValue constant(@Nullable Object value, Type type) {
        return new Constant(value, type);
    }

    public static StackValue cmp(IElementType opToken, Type type) {
        return type.getSort() == Type.OBJECT ? new ObjectCompare(opToken, type) : new NumberCompare(opToken, type);
    }

    public static StackValue not(StackValue stackValue) {
        return new Invert(stackValue);
    }

    @NotNull
    public static StackValue arrayElement(Type type, boolean unbox) {
        return new ArrayElement(type, unbox);
    }

    @NotNull
    public static StackValue collectionElement(
            Type type,
            ResolvedCall<FunctionDescriptor> getter,
            ResolvedCall<FunctionDescriptor> setter,
            ExpressionCodegen codegen,
            GenerationState state
    ) {
        return new CollectionElement(type, getter, setter, codegen, state);
    }

    @NotNull
    public static Field field(@NotNull Type type, @NotNull Type owner, @NotNull String name, boolean isStatic) {
        return new Field(type, owner, name, isStatic);
    }

    @NotNull
    public static Property property(
            @NotNull PropertyDescriptor descriptor,
            @NotNull Type methodOwner,
            @NotNull Type type,
            boolean isStatic,
            @NotNull String name,
            @Nullable CallableMethod getter,
            @Nullable CallableMethod setter,
            GenerationState state
    ) {
        return new Property(descriptor, methodOwner, getter, setter, isStatic, name, type, state);
    }

    @NotNull
    public static StackValue expression(Type type, JetExpression expression, ExpressionCodegen generator) {
        return new Expression(type, expression, generator);
    }

    private static void box(Type type, Type toType, InstructionAdapter v) {
        if (type == Type.BYTE_TYPE || toType.getInternalName().equals(NULLABLE_BYTE_TYPE_NAME) && type == Type.INT_TYPE) {
            v.cast(type, Type.BYTE_TYPE);
            v.invokestatic(NULLABLE_BYTE_TYPE_NAME, "valueOf", "(B)L" + NULLABLE_BYTE_TYPE_NAME + ";");
        }
        else if (type == Type.SHORT_TYPE || toType.getInternalName().equals(NULLABLE_SHORT_TYPE_NAME) && type == Type.INT_TYPE) {
            v.cast(type, Type.SHORT_TYPE);
            v.invokestatic(NULLABLE_SHORT_TYPE_NAME, "valueOf", "(S)L" + NULLABLE_SHORT_TYPE_NAME + ";");
        }
        else if (type == Type.LONG_TYPE || toType.getInternalName().equals(NULLABLE_LONG_TYPE_NAME) && type == Type.INT_TYPE) {
            v.cast(type, Type.LONG_TYPE);
            v.invokestatic(NULLABLE_LONG_TYPE_NAME, "valueOf", "(J)L" + NULLABLE_LONG_TYPE_NAME +";");
        }
        else if (type == Type.INT_TYPE) {
            v.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
        }
        else if (type == Type.BOOLEAN_TYPE) {
            v.invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
        }
        else if (type == Type.CHAR_TYPE) {
            v.invokestatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
        }
        else if (type == Type.FLOAT_TYPE) {
            v.invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
        }
    }

    private static void unbox(Type type, InstructionAdapter v) {
        if (type == Type.INT_TYPE) {
            v.invokevirtual("java/lang/Number", "intValue", "()I");
        }
        else if (type == Type.BOOLEAN_TYPE) {
            v.invokevirtual("java/lang/Boolean", "booleanValue", "()Z");
        }
        else if (type == Type.CHAR_TYPE) {
            v.invokevirtual("java/lang/Character", "charValue", "()C");
        }
        else if (type == Type.SHORT_TYPE) {
            v.invokevirtual("java/lang/Number", "shortValue", "()S");
        }
        else if (type == Type.LONG_TYPE) {
            v.invokevirtual("java/lang/Number", "longValue", "()J");
        }
        else if (type == Type.BYTE_TYPE) {
            v.invokevirtual("java/lang/Number", "byteValue", "()B");
        }
        else if (type == Type.FLOAT_TYPE) {
            v.invokevirtual("java/lang/Number", "floatValue", "()F");
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.invokevirtual("java/lang/Number", "doubleValue", "()D");
        }
    }

    protected void coerceTo(Type toType, InstructionAdapter v) {
        coerce(this.type, toType, v);
    }

    protected void coerceFrom(Type topOfStackType, InstructionAdapter v) {
        coerce(topOfStackType, this.type, v);
    }

    public static void coerce(Type fromType, Type toType, InstructionAdapter v) {
        if (toType.equals(fromType)) return;

        if (toType.getSort() == Type.VOID) {
            pop(v, fromType);
        }
        else if (fromType.getSort() == Type.VOID) {
            if (toType.equals(JET_UNIT_TYPE) || toType.equals(OBJECT_TYPE)) {
                putUnitInstance(v);
            }
            else if (toType.getSort() == Type.OBJECT || toType.getSort() == Type.ARRAY) {
                v.aconst(null);
            }
            else {
                pushDefaultPrimitiveValueOnStack(toType, v);
            }
        }
        else if (toType.equals(JET_UNIT_TYPE)) {
            if (fromType.equals(getType(Object.class))) {
                v.checkcast(JET_UNIT_TYPE);
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

    public static void putUnitInstance(InstructionAdapter v) {
        v.visitFieldInsn(GETSTATIC, AsmTypeConstants.JET_UNIT_TYPE.getInternalName(), "VALUE", AsmTypeConstants.JET_UNIT_TYPE.getDescriptor());
    }

    protected void putAsBoolean(InstructionAdapter v) {
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

    public static StackValue fieldForSharedVar(Type localType, Type classType, String fieldName) {
        return new FieldForSharedVar(localType, classType, fieldName);
    }

    public static StackValue composed(StackValue prefix, StackValue suffix) {
        return new Composed(prefix, suffix);
    }

    public static StackValue thisOrOuter(ExpressionCodegen codegen, ClassDescriptor descriptor, boolean isSuper) {
        // Coerce this/super for traits to support traits with required classes
        // Do not coerce for other classes due to the 'protected' access issues (JVMS 7, 4.9.2 Structural Constraints)
        boolean coerceType = descriptor.getKind() == ClassKind.TRAIT;
        return new ThisOuter(codegen, descriptor, isSuper, coerceType);
    }

    public static StackValue postIncrement(int index, int increment) {
        return new PostIncrement(index, increment);
    }

    public static StackValue preIncrement(int index, int increment) {
        return new PreIncrement(index, increment);
    }

    public static StackValue receiver(
            ResolvedCall<? extends CallableDescriptor> resolvedCall,
            StackValue receiver,
            ExpressionCodegen codegen,
            @Nullable CallableMethod callableMethod
    ) {
        if (resolvedCall.getThisObject().exists() || resolvedCall.getReceiverArgument().exists()) {
            return new CallReceiver(resolvedCall, receiver, codegen, callableMethod, true);
        }
        return receiver;
    }

    public static StackValue receiverWithoutReceiverArgument(StackValue receiverWithParameter) {
        if (receiverWithParameter instanceof CallReceiver) {
            CallReceiver callReceiver = (CallReceiver) receiverWithParameter;
            return new CallReceiver(callReceiver.resolvedCall, callReceiver.receiver,
                                    callReceiver.codegen, callReceiver.callableMethod, false);
        }
        return receiverWithParameter;
    }

    public static Field singleton(ClassDescriptor classDescriptor, JetTypeMapper typeMapper) {
        FieldInfo info = FieldInfo.createForSingleton(classDescriptor, typeMapper);
        return field(info.getFieldType(), Type.getObjectType(info.getOwnerInternalName()), info.getFieldName(), true);
    }

    private static class None extends StackValue {
        public static final None INSTANCE = new None();

        private None() {
            super(Type.VOID_TYPE);
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            coerceTo(type, v);
        }
    }

    public static class Local extends StackValue {
        final int index;

        public Local(int index, Type type) {
            super(type);
            this.index = index;

            if (index < 0) {
                throw new IllegalStateException("local variable index must be non-negative");
            }
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.load(index, this.type);
            coerceTo(type, v);
            // TODO unbox
        }

        @Override
        public void store(Type topOfStackType, InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.store(index, this.type);
        }
    }

    public static class OnStack extends StackValue {
        public OnStack(Type type) {
            super(type);
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            coerceTo(type, v);
        }

        @Override
        public void moveToTopOfStack(Type type, InstructionAdapter v, int depth) {
            if (depth == 0) {
                put(type, v);
            }
            else if (depth == 1) {
                if (type.getSize() != 1) {
                    throw new UnsupportedOperationException("don't know how to move type " + type + " to top of stack");
                }
                v.swap();
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
            super(type);
            this.value = value;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
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
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
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
        private final Type operandType;

        public NumberCompare(IElementType opToken, Type operandType) {
            super(Type.BOOLEAN_TYPE);
            this.opToken = opToken;
            this.operandType = operandType;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            putAsBoolean(v);
            coerceTo(type, v);
        }

        @Override
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            int opcode;
            if (opToken == JetTokens.EQEQ) {
                opcode = jumpIfFalse ? IFNE : IFEQ;
            }
            else if (opToken == JetTokens.EXCLEQ) {
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
                throw new UnsupportedOperationException("don't know how to generate this condjump");
            }
            if (operandType == Type.FLOAT_TYPE || operandType == Type.DOUBLE_TYPE) {
                if (opToken == JetTokens.GT || opToken == JetTokens.GTEQ) {
                    v.cmpg(operandType);
                }
                else {
                    v.cmpl(operandType);
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
        public ObjectCompare(IElementType opToken, Type operandType) {
            super(opToken, operandType);
        }

        @Override
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
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
        public void put(Type type, InstructionAdapter v) {
            putAsBoolean(v);
            coerceTo(type, v);
        }

        @Override
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            myOperand.condJump(label, !jumpIfFalse, v);
        }
    }

    private static class ArrayElement extends StackValue {
        private final Type boxed;

        public ArrayElement(Type type, boolean unbox) {
            super(type);
            this.boxed = unbox ? boxType(type) : type;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.aload(boxed);    // assumes array and index are on the stack
            coerce(boxed, type, v);
        }

        @Override
        public void store(Type topOfStackType, InstructionAdapter v) {
            coerce(topOfStackType, boxed, v);
            v.astore(boxed);
        }

        @Override
        public void dupReceiver(InstructionAdapter v) {
            v.dup2();   // array and index
        }

        @Override
        public int receiverSize() {
            return 2;
        }
    }

    private static class CollectionElement extends StackValue {
        private final Callable getter;
        private final Callable setter;
        private final ExpressionCodegen codegen;
        private final GenerationState state;
        private final FrameMap frame;
        private final ResolvedCall<FunctionDescriptor> resolvedGetCall;
        private final ResolvedCall<FunctionDescriptor> resolvedSetCall;
        private final FunctionDescriptor setterDescriptor;
        private final FunctionDescriptor getterDescriptor;

        public CollectionElement(
                Type type,
                ResolvedCall<FunctionDescriptor> resolvedGetCall,
                ResolvedCall<FunctionDescriptor> resolvedSetCall,
                ExpressionCodegen codegen,
                GenerationState state
        ) {
            super(type);
            this.resolvedGetCall = resolvedGetCall;
            this.resolvedSetCall = resolvedSetCall;
            this.state = state;
            this.setterDescriptor = resolvedSetCall == null ? null : resolvedSetCall.getResultingDescriptor();
            this.getterDescriptor = resolvedGetCall == null ? null : resolvedGetCall.getResultingDescriptor();
            this.setter = resolvedSetCall == null ? null : codegen.resolveToCallable(setterDescriptor, false);
            this.getter = resolvedGetCall == null ? null : codegen.resolveToCallable(getterDescriptor, false);
            this.codegen = codegen;
            this.frame = codegen.myFrameMap;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if (getter == null) {
                throw new UnsupportedOperationException("no getter specified");
            }
            if (getter instanceof CallableMethod) {
                ((CallableMethod) getter).invokeWithNotNullAssertion(v, state, resolvedGetCall);
            }
            else {
                ((IntrinsicMethod) getter).generate(codegen, v, this.type, null, null, null);
            }
            coerceTo(type, v);
        }

        @Override
        public void store(Type topOfStackType, InstructionAdapter v) {
            if (setter == null) {
                throw new UnsupportedOperationException("no setter specified");
            }
            if (setter instanceof CallableMethod) {
                CallableMethod method = (CallableMethod) setter;
                Method asmMethod = method.getSignature().getAsmMethod();
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
                ((IntrinsicMethod) setter).generate(codegen, v, null, null, null, null);
            }
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

        @Override
        public void dupReceiver(InstructionAdapter v) {
            if (isStandardStack(resolvedGetCall, 1) && isStandardStack(resolvedSetCall, 2)) {
                v.dup2();   // collection and index
            }
            else {
                int size = 0;
                // ugly hack: getting the last variable index
                int lastIndex = frame.enterTemp(Type.INT_TYPE);
                frame.leaveTemp(Type.INT_TYPE);

                // indexes
                List<ValueParameterDescriptor> valueParameters = resolvedGetCall.getResultingDescriptor().getValueParameters();
                int firstParamIndex = -1;
                for (int i = valueParameters.size() - 1; i >= 0; --i) {
                    Type type = codegen.typeMapper.mapType(valueParameters.get(i).getType());
                    int sz = type.getSize();
                    frame.enterTemp(type);
                    lastIndex += sz;
                    size += sz;
                    v.store((firstParamIndex = lastIndex) - sz, type);
                }

                List<TypeParameterDescriptor> typeParameters = resolvedGetCall.getResultingDescriptor().getTypeParameters();
                int firstTypeParamIndex = -1;
                for (int i = typeParameters.size() - 1; i >= 0; --i) {
                    if (typeParameters.get(i).isReified()) {
                        frame.enterTemp(OBJECT_TYPE);
                        lastIndex++;
                        size++;
                        v.store(firstTypeParamIndex = lastIndex - 1, OBJECT_TYPE);
                    }
                }

                ReceiverValue receiverParameter = resolvedGetCall.getReceiverArgument();
                int receiverIndex = -1;
                if (receiverParameter.exists()) {
                    Type type = codegen.typeMapper.mapType(receiverParameter.getType());
                    int sz = type.getSize();
                    frame.enterTemp(type);
                    lastIndex += sz;
                    size += sz;
                    v.store((receiverIndex = lastIndex) - sz, type);
                }

                ReceiverValue thisObject = resolvedGetCall.getThisObject();
                int thisIndex = -1;
                if (thisObject.exists()) {
                    frame.enterTemp(OBJECT_TYPE);
                    lastIndex++;
                    size++;
                    v.store((thisIndex = lastIndex) - 1, OBJECT_TYPE);
                }

                // for setter

                int realReceiverIndex;
                Type realReceiverType;
                if (thisIndex != -1) {
                    if (receiverIndex != -1) {
                        realReceiverIndex = receiverIndex;
                        realReceiverType = codegen.typeMapper.mapType(receiverParameter.getType());
                    }
                    else {
                        realReceiverIndex = thisIndex;
                        realReceiverType = OBJECT_TYPE;
                    }
                }
                else {
                    if (receiverIndex != -1) {
                        realReceiverType = codegen.typeMapper.mapType(receiverParameter.getType());
                        realReceiverIndex = receiverIndex;
                    }
                    else {
                        throw new UnsupportedOperationException();
                    }
                }

                if (resolvedSetCall.getThisObject().exists()) {
                    if (resolvedSetCall.getReceiverArgument().exists()) {
                        codegen.generateFromResolvedCall(resolvedSetCall.getThisObject(), OBJECT_TYPE);
                    }
                    v.load(realReceiverIndex - realReceiverType.getSize(), realReceiverType);
                }
                else {
                    if (resolvedSetCall.getReceiverArgument().exists()) {
                        v.load(realReceiverIndex - realReceiverType.getSize(), realReceiverType);
                    }
                    else {
                        throw new UnsupportedOperationException();
                    }
                }

                int index = firstParamIndex;
                for (int i = 0; i != valueParameters.size(); ++i) {
                    Type type = codegen.typeMapper.mapType(valueParameters.get(i).getType());
                    int sz = type.getSize();
                    v.load(index - sz, type);
                    index -= sz;
                }

                // restoring original
                if (thisIndex != -1) {
                    v.load(thisIndex - 1, OBJECT_TYPE);
                }

                if (receiverIndex != -1) {
                    Type type = codegen.typeMapper.mapType(receiverParameter.getType());
                    v.load(receiverIndex - type.getSize(), type);
                }

                if (firstTypeParamIndex != -1) {
                    index = firstTypeParamIndex;
                    for (int i = 0; i != typeParameters.size(); ++i) {
                        if (typeParameters.get(i).isReified()) {
                            v.load(index - 1, OBJECT_TYPE);
                            index--;
                        }
                    }
                }

                index = firstParamIndex;
                for (int i = 0; i != valueParameters.size(); ++i) {
                    Type type = codegen.typeMapper.mapType(valueParameters.get(i).getType());
                    int sz = type.getSize();
                    v.load(index - sz, type);
                    index -= sz;
                }

                for (int i = 0; i < size; i++) {
                    frame.leaveTemp(OBJECT_TYPE);
                }
            }
        }

        private boolean isStandardStack(ResolvedCall call, int valueParamsSize) {
            if (call == null) {
                return true;
            }

            for (TypeParameterDescriptor typeParameterDescriptor : call.getResultingDescriptor().getTypeParameters()) {
                if (typeParameterDescriptor.isReified()) {
                    return false;
                }
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

            if (call.getThisObject().exists()) {
                if (call.getReceiverArgument().exists()) {
                    return false;
                }
            }
            else {
                if (codegen.typeMapper.mapType(call.getResultingDescriptor().getReceiverParameter().getType())
                            .getSize() != 1) {
                    return false;
                }
            }

            return true;
        }
    }


    static class Field extends StackValueWithSimpleReceiver {
        final Type owner;
        final String name;

        public Field(Type type, Type owner, String name, boolean isStatic) {
            super(type, isStatic);
            this.owner = owner;
            this.name = name;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.visitFieldInsn(isStatic ? GETSTATIC : GETFIELD, owner.getInternalName(), name, this.type.getDescriptor());
            coerceTo(type, v);
        }

        @Override
        public void store(Type topOfStackType, InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.visitFieldInsn(isStatic ? PUTSTATIC : PUTFIELD, owner.getInternalName(), name, this.type.getDescriptor());
        }
    }

    static class Property extends StackValueWithSimpleReceiver {
        @Nullable
        private final CallableMethod getter;
        @Nullable
        private final CallableMethod setter;
        @NotNull
        public final Type methodOwner;

        @NotNull
        private final PropertyDescriptor descriptor;
        @NotNull
        private final GenerationState state;

        private final String name;

        public Property(
                @NotNull PropertyDescriptor descriptor, @NotNull Type methodOwner,
                @Nullable CallableMethod getter, @Nullable CallableMethod setter, boolean isStatic,
                @NotNull String name, @NotNull Type type, @NotNull GenerationState state
        ) {
            super(type, isStatic);
            this.methodOwner = methodOwner;
            this.getter = getter;
            this.setter = setter;
            this.descriptor = descriptor;
            this.state = state;
            this.name = name;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if (getter == null) {
                v.visitFieldInsn(isStatic ? GETSTATIC : GETFIELD, methodOwner.getInternalName(), getPropertyName(),
                                 this.type.getDescriptor());
                genNotNullAssertionForField(v, state, descriptor);
            }
            else {
                Method method = getter.getSignature().getAsmMethod();
                v.visitMethodInsn(getter.getInvokeOpcode(), getter.getOwner().getInternalName(), method.getName(), method.getDescriptor());
            }
            coerceTo(type, v);
        }

        @Override
        public void store(Type topOfStackType, InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            if (setter == null) {
                v.visitFieldInsn(isStatic ? PUTSTATIC : PUTFIELD, methodOwner.getInternalName(), getPropertyName(),
                                 this.type.getDescriptor()); }
            else {
                Method method = setter.getSignature().getAsmMethod();
                v.visitMethodInsn(setter.getInvokeOpcode(), setter.getOwner().getInternalName(), method.getName(), method.getDescriptor());
            }
        }

        private String getPropertyName() {
            return name;
        }

        public boolean isPropertyWithBackingFieldInOuterClass() {
            return descriptor instanceof AccessorForPropertyBackingFieldInOuterClass;
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
        public void put(Type type, InstructionAdapter v) {
            generator.gen(expression, type);
        }
    }

    public static class Shared extends StackValue {
        private final int index;
        private boolean isReleaseOnPut = false;

        public Shared(int index, Type type) {
            super(type);
            this.index = index;
        }

        public void releaseOnPut() {
            isReleaseOnPut = true;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.load(index, OBJECT_TYPE);
            Type refType = refType(this.type);
            Type sharedType = sharedTypeForType(this.type);
            v.visitFieldInsn(GETFIELD, sharedType.getInternalName(), "ref", refType.getDescriptor());
            coerceFrom(refType, v);
            coerceTo(type, v);
            if (isReleaseOnPut) {
                v.aconst(null);
                v.store(index, OBJECT_TYPE);
            }
        }

        @Override
        public void store(Type topOfStackType, InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.load(index, OBJECT_TYPE);
            AsmUtil.swap(v, sharedTypeForType(this.type), topOfStackType);
            Type refType = refType(this.type);
            Type sharedType = sharedTypeForType(this.type);
            v.visitFieldInsn(PUTFIELD, sharedType.getInternalName(), "ref", refType.getDescriptor());
        }
    }

    public static Type sharedTypeForType(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                return JET_SHARED_VAR_TYPE;

            case Type.BYTE:
                return JET_SHARED_BYTE_TYPE;

            case Type.SHORT:
                return JET_SHARED_SHORT_TYPE;

            case Type.CHAR:
                return JET_SHARED_CHAR_TYPE;

            case Type.INT:
                return JET_SHARED_INT_TYPE;

            case Type.LONG:
                return JET_SHARED_LONG_TYPE;

            case Type.BOOLEAN:
                return JET_SHARED_BOOLEAN_TYPE;

            case Type.FLOAT:
                return JET_SHARED_FLOAT_TYPE;

            case Type.DOUBLE:
                return JET_SHARED_DOUBLE_TYPE;

            default:
                throw new UnsupportedOperationException();
        }
    }

    public static Type refType(Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return OBJECT_TYPE;
        }

        return type;
    }

    static class FieldForSharedVar extends StackValueWithSimpleReceiver {
        final Type owner;
        final String name;

        public FieldForSharedVar(Type type, Type owner, String name) {
            super(type, false);
            this.owner = owner;
            this.name = name;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            Type sharedType = sharedTypeForType(this.type);
            Type refType = refType(this.type);
            v.visitFieldInsn(GETFIELD, sharedType.getInternalName(), "ref", refType.getDescriptor());
            coerceFrom(refType, v);
            coerceTo(type, v);
        }

        @Override
        public void store(Type topOfStackType, InstructionAdapter v) {
            coerceFrom(topOfStackType, v);
            v.visitFieldInsn(PUTFIELD, sharedTypeForType(type).getInternalName(), "ref", refType(type).getDescriptor());
        }
    }

    public static class Composed extends StackValue {
        public final StackValue prefix;
        public final StackValue suffix;

        public Composed(StackValue prefix, StackValue suffix) {
            super(suffix.type);
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            prefix.put(prefix.type, v);
            suffix.put(type, v);
        }

        @Override
        public void store(Type topOfStackType, InstructionAdapter v) {
            prefix.put(OBJECT_TYPE, v);
            suffix.store(topOfStackType, v);
        }
    }

    private static class ThisOuter extends StackValue {
        private final ExpressionCodegen codegen;
        private final ClassDescriptor descriptor;
        private final boolean isSuper;
        private final boolean coerceType;

        public ThisOuter(ExpressionCodegen codegen, ClassDescriptor descriptor, boolean isSuper, boolean coerceType) {
            super(OBJECT_TYPE);
            this.codegen = codegen;
            this.descriptor = descriptor;
            this.isSuper = isSuper;
            this.coerceType = coerceType;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
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
        public void put(Type type, InstructionAdapter v) {
            if (!type.equals(Type.VOID_TYPE)) {
                v.load(index, Type.INT_TYPE);
                coerceTo(type, v);
            }
            v.iinc(index, increment);
        }
    }

    private static class PreIncrement extends StackValue {
        private final int index;
        private final int increment;

        public PreIncrement(int index, int increment) {
            super(Type.INT_TYPE);
            this.index = index;
            this.increment = increment;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.iinc(index, increment);
            if (!type.equals(Type.VOID_TYPE)) {
                v.load(index, Type.INT_TYPE);
                coerceTo(type, v);
            }
        }
    }

    public static class CallReceiver extends StackValue {
        private final ResolvedCall<? extends CallableDescriptor> resolvedCall;
        final StackValue receiver;
        private final ExpressionCodegen codegen;
        private final CallableMethod callableMethod;
        private final boolean putReceiverArgumentOnStack;

        public CallReceiver(
                ResolvedCall<? extends CallableDescriptor> resolvedCall,
                StackValue receiver,
                ExpressionCodegen codegen,
                CallableMethod callableMethod,
                boolean putReceiverArgumentOnStack
        ) {
            super(calcType(resolvedCall, codegen, callableMethod));
            this.resolvedCall = resolvedCall;
            this.receiver = receiver;
            this.codegen = codegen;
            this.callableMethod = callableMethod;
            this.putReceiverArgumentOnStack = putReceiverArgumentOnStack;
        }

        private static Type calcType(
                ResolvedCall<? extends CallableDescriptor> resolvedCall,
                ExpressionCodegen codegen,
                CallableMethod callableMethod
        ) {
            ReceiverValue thisObject = resolvedCall.getThisObject();
            ReceiverValue receiverArgument = resolvedCall.getReceiverArgument();

            CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

            if (thisObject.exists()) {
                if (callableMethod != null) {
                    if (receiverArgument.exists()) {
                        return callableMethod.getReceiverClass();
                    }
                    else {
                        //noinspection ConstantConditions
                        return callableMethod.getThisType();
                    }
                }
                else {
                    if (receiverArgument.exists()) {
                        return codegen.typeMapper.mapType(descriptor.getReceiverParameter().getType());
                    }
                    else {
                        return codegen.typeMapper.mapType(descriptor.getExpectedThisObject().getType());
                    }
                }
            }
            else {
                if (receiverArgument.exists()) {
                    if (callableMethod != null) {
                        return callableMethod.getReceiverClass();
                    }
                    else {
                        return codegen.typeMapper.mapType(descriptor.getReceiverParameter().getType());
                    }
                }
                else {
                    return Type.VOID_TYPE;
                }
            }
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

            ReceiverValue thisObject = resolvedCall.getThisObject();
            ReceiverValue receiverArgument = resolvedCall.getReceiverArgument();
            if (thisObject.exists()) {
                if (receiverArgument.exists()) {
                    if (callableMethod != null) {
                        codegen.generateFromResolvedCall(thisObject, callableMethod.getOwner());
                    }
                    else {
                        codegen.generateFromResolvedCall(thisObject, codegen.typeMapper
                                .mapType(descriptor.getExpectedThisObject().getType()));
                    }
                    if (putReceiverArgumentOnStack) {
                        genReceiver(v, receiverArgument, type, descriptor.getReceiverParameter(), 1);
                    }
                }
                else {
                    genReceiver(v, thisObject, type, null, 0);
                }
            }
            else {
                if (putReceiverArgumentOnStack && receiverArgument.exists()) {
                    genReceiver(v, receiverArgument, type, descriptor.getReceiverParameter(), 0);
                }
            }
        }

        private void genReceiver(
                InstructionAdapter v, ReceiverValue receiverArgument, Type type,
                @Nullable ReceiverParameterDescriptor receiverParameter, int depth
        ) {
            if (receiver == StackValue.none()) {
                if (receiverParameter != null) {
                    Type receiverType = codegen.typeMapper.mapType(receiverParameter.getType());
                    codegen.generateFromResolvedCall(receiverArgument, receiverType);
                    StackValue.onStack(receiverType).put(type, v);
                }
                else {
                    codegen.generateFromResolvedCall(receiverArgument, type);
                }
            }
            else {
                receiver.moveToTopOfStack(type, v, depth);
            }
        }
    }

    public abstract static class StackValueWithSimpleReceiver extends StackValue {

        protected final boolean isStatic;

        public StackValueWithSimpleReceiver(@NotNull Type type, boolean isStatic) {
            super(type);
            this.isStatic = isStatic;
        }

        @Override
        public void dupReceiver(InstructionAdapter v) {
            if (!isStatic) {
                v.dup();
            }
        }

        @Override
        public int receiverSize() {
            return isStatic ? 0 : 1;
        }
    }
}
