package org.jetbrains.jet.codegen;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * @author yole
 */
public abstract class StackValue {
    protected final Type type;

    public StackValue(Type type) {
        this.type = type;
    }

    public abstract void put(Type type, InstructionAdapter v);

    public static StackValue local(int index, Type type) {
        return new Local(index, type);
    }

    public static StackValue onStack(Type type) {
        assert type != Type.VOID_TYPE;
        return new OnStack(type);
    }

    public static StackValue constant(Object value, Type type) {
        return new Constant(value, type);
    }

    public static StackValue cmp(IElementType opToken, Type operandType) {
        return new NumberCompare(opToken, operandType);
    }

    public static StackValue not(StackValue stackValue) {
        return new Invert(stackValue);
    }

    public abstract void condJump(Label jumpIfTrue, Label jumpIfFalse, InstructionAdapter v);

    private static void box(final Type type, InstructionAdapter v) {
        if (type == Type.INT_TYPE) {
            v.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
        }
        else if (type == Type.BOOLEAN_TYPE) {
            v.invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
        }
        else if (type == Type.CHAR_TYPE) {
            v.invokestatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
        }
        else if (type == Type.SHORT_TYPE) {
            v.invokestatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
        }
        else if (type == Type.LONG_TYPE) {
            v.invokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
        }
        else if (type == Type.BYTE_TYPE) {
            v.invokestatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
        }
        else if (type == Type.FLOAT_TYPE) {
            v.invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
        }
    }

    protected void coerce(Type type, InstructionAdapter v) {
        if (type.getSort() == Type.OBJECT) {
            box(this.type, v);
        }
        else if (type != this.type) {
            v.cast(this.type, type);
        }
    }

    protected void putAsBoolean(InstructionAdapter v) {
        Label ifTrue = new Label();
        Label ifFalse = new Label();
        Label end = new Label();
        condJump(ifTrue, ifFalse, v);
        v.mark(ifFalse);
        v.iconst(0);
        v.goTo(end);
        v.mark(ifTrue);
        v.iconst(1);
        v.mark(end);
    }

    public static class Local extends StackValue {
        private final int index;

        public Local(int index, Type type) {
            super(type);
            this.index = index;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.load(index, this.type);
            coerce(type, v);
            // TODO unbox
        }

        @Override
        public void condJump(Label ifTrue, Label ifFalse, InstructionAdapter v) {
            put(Type.INT_TYPE, v);
            // TODO optimize extra jump away
            v.ifne(ifTrue);
            v.goTo(ifFalse);
        }
    }

    public static class OnStack extends StackValue {
        public OnStack(Type type) {
            super(type);
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            coerce(type, v);
        }

        @Override
        public void condJump(Label ifTrue, Label ifFalse, InstructionAdapter v) {
            if (this.type == Type.BOOLEAN_TYPE) {
                // TODO optimize extra jump away
                v.ifne(ifTrue);
                v.goTo(ifFalse);
            }
            else {
                throw new UnsupportedOperationException("can't generate a cond jump for a non-boolean value on stack");
            }
        }
    }

    public static class Constant extends StackValue {
        private final Object value;

        public Constant(Object value, Type type) {
            super(type);
            this.value = value;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.aconst(value);
            coerce(type, v);
        }

        @Override
        public void condJump(Label ifTrue, Label ifFalse, InstructionAdapter v) {
            if (value instanceof Boolean) {
                boolean boolValue = ((Boolean) value).booleanValue();
                v.goTo(boolValue ? ifTrue : ifFalse);
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate this condjump");
            }
        }
    }

    private static class NumberCompare extends StackValue {
        private final IElementType opToken;
        private final Type operandType;

        public NumberCompare(IElementType opToken, Type operandType) {
            super(Type.BOOLEAN_TYPE);
            this.opToken = opToken;
            this.operandType = operandType;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if (type != Type.BOOLEAN_TYPE) {
                throw new UnsupportedOperationException("don't know how to put a compare as a non-boolean type");
            }
            putAsBoolean(v);
        }

        @Override
        public void condJump(Label ifTrue, Label ifFalse, InstructionAdapter v) {
            int opcode;
            if (opToken == JetTokens.EQEQ) opcode = Opcodes.IFEQ;
            else if (opToken == JetTokens.EXCLEQ) opcode = Opcodes.IFNE;
            else if (opToken == JetTokens.GT) opcode = Opcodes.IFGT;
            else if (opToken == JetTokens.GTEQ) opcode = Opcodes.IFGE;
            else if (opToken == JetTokens.LT) opcode = Opcodes.IFLT;
            else if (opToken == JetTokens.LTEQ) opcode = Opcodes.IFLE;
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
                opcode += (Opcodes.IF_ICMPEQ - Opcodes.IFEQ);
            }
            v.visitJumpInsn(opcode, ifTrue);
            v.goTo(ifFalse);
        }
    }

    private static class Invert extends StackValue {
        private StackValue myOperand;

        private Invert(StackValue operand) {
            super(operand.type);
            myOperand = operand;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if (type != Type.BOOLEAN_TYPE) {
                throw new UnsupportedOperationException("don't know how to put a compare as a non-boolean type");
            }
            putAsBoolean(v);
        }

        @Override
        public void condJump(Label ifTrue, Label ifFalse, InstructionAdapter v) {
            myOperand.condJump(ifFalse, ifTrue, v);
        }
    }
}
