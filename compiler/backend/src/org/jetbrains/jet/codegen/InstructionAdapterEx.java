package org.jetbrains.jet.codegen;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

public class InstructionAdapterEx extends InstructionAdapter {
    private static final Method BOOLEAN_VALUE = Method.getMethod("boolean booleanValue()");

    private static final Method CHAR_VALUE = Method.getMethod("char charValue()");

    private static final Method INT_VALUE = Method.getMethod("int intValue()");

    private static final Method FLOAT_VALUE = Method.getMethod("float floatValue()");

    private static final Method LONG_VALUE = Method.getMethod("long longValue()");

    private static final Method DOUBLE_VALUE = Method.getMethod("double doubleValue()");

    public InstructionAdapterEx(MethodVisitor methodVisitor) {
        super(methodVisitor);
    }

    private static Type getBoxedType(final Type type) {
        switch (type.getSort()) {
            case Type.BYTE:
                return JetTypeMapper.JL_BYTE_TYPE;
            case Type.BOOLEAN:
                return JetTypeMapper.JL_BOOLEAN_TYPE;
            case Type.SHORT:
                return JetTypeMapper.JL_SHORT_TYPE;
            case Type.CHAR:
                return JetTypeMapper.JL_CHAR_TYPE;
            case Type.INT:
                return JetTypeMapper.JL_INTEGER_TYPE;
            case Type.FLOAT:
                return JetTypeMapper.JL_FLOAT_TYPE;
            case Type.LONG:
                return JetTypeMapper.JL_LONG_TYPE;
            case Type.DOUBLE:
                return JetTypeMapper.JL_DOUBLE_TYPE;
        }
        return type;
    }

    public void valueOf(final Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return;
        }
        if (type == Type.VOID_TYPE) {
            aconst(null);
        } else {
            Type boxed = getBoxedType(type);
            invokestatic(boxed.getInternalName(), "valueOf", "(" + type.getDescriptor() + ")" + boxed.getDescriptor());
        }
    }

    public void unbox(final Type type) {
        Type t = JetTypeMapper.JL_NUMBER_TYPE;
        Method sig = null;
        switch (type.getSort()) {
            case Type.VOID:
                return;
            case Type.CHAR:
                t = JetTypeMapper.JL_CHAR_TYPE;
                sig = CHAR_VALUE;
                break;
            case Type.BOOLEAN:
                t = JetTypeMapper.JL_BOOLEAN_TYPE;
                sig = BOOLEAN_VALUE;
                break;
            case Type.DOUBLE:
                sig = DOUBLE_VALUE;
                break;
            case Type.FLOAT:
                sig = FLOAT_VALUE;
                break;
            case Type.LONG:
                sig = LONG_VALUE;
                break;
            case Type.INT:
            case Type.SHORT:
            case Type.BYTE:
                sig = INT_VALUE;
        }

        checkcast(t);
        invokevirtual(t.getInternalName(), sig.getName(), sig.getDescriptor());
    }
}
