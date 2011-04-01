package org.jetbrains.jet.codegen;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * @author yole
 */
public abstract class StackValue {
    public abstract void put(Type type, InstructionAdapter v);

    public static StackValue local(int index, JetType type) {
        return new Local(index, type);
    }

    public static StackValue onStack(Type type) {
        return new OnStack(type);
    }

    public static StackValue constant(Object value) {
        return new Constant(value);
    }

    public static StackValue icmp(IElementType opToken) {
        return new IntCompare(opToken);
    }

    public abstract void condJump(Label label, boolean inverse, InstructionAdapter v);

    public static class Local extends StackValue {
        private final int index;
        private final JetType type;

        public Local(int index, JetType type) {
            this.index = index;
            this.type = type;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.load(index, type);
            // TODO box/unbox
        }

        @Override
        public void condJump(Label label, boolean inverse, InstructionAdapter v) {
            put(Type.INT_TYPE, v);
            if (inverse) {
                v.ifeq(label);
            }
            else {
                v.ifne(label);
            }
        }
    }

    public static class OnStack extends StackValue {
        private final Type type;

        public OnStack(Type type) {
            this.type = type;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
        }

        @Override
        public void condJump(Label label, boolean inverse, InstructionAdapter v) {
            throw new UnsupportedOperationException("don't know how to generate this condjump");
        }
    }

    public static class Constant extends StackValue {
        private final Object value;

        public Constant(Object value) {
            this.value = value;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if (type == Type.INT_TYPE) {
                if (value instanceof Number) {
                    v.iconst(((Number) value).intValue());
                }
                else {
                    throw new UnsupportedOperationException("don't know how to put this value");
                }
            }
            // TODO other primitive types
            else {
                v.aconst(value);
            }
        }

        @Override
        public void condJump(Label label, boolean inverse, InstructionAdapter v) {
            throw new UnsupportedOperationException("don't know how to generate this condjump");
        }
    }

    private static class IntCompare extends StackValue {
        private final IElementType opToken;

        public IntCompare(IElementType opToken) {
            this.opToken = opToken;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            throw new UnsupportedOperationException("don't know how to put an IntCompare on stack");
        }

        @Override
        public void condJump(Label label, boolean inverse, InstructionAdapter v) {
            int opcode;
            if (opToken == JetTokens.GT) {
                opcode = inverse ? Opcodes.IF_ICMPLE : Opcodes.IF_ICMPGT;
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate this condjump");
            }
            v.visitJumpInsn(opcode, label);
        }
    }
}
