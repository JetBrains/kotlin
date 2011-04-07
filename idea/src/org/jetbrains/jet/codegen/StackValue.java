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
        assert type != Type.VOID_TYPE;
        return new OnStack(type);
    }

    public static StackValue constant(Object value) {
        return new Constant(value);
    }

    public static StackValue cmp(IElementType opToken, Type type) {
        return new NumberCompare(opToken, type);
    }

    public abstract void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v);

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
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            put(Type.INT_TYPE, v);
            if (jumpIfFalse) {
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
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            if (type == Type.BOOLEAN_TYPE) {
                if (jumpIfFalse) {
                    v.ifeq(label);
                }
                else {
                    v.ifne(label);
                }
            }
            else {
                throw new UnsupportedOperationException("can't generate a cond jump for a non-boolean value on stack");
            }
        }
    }

    public static class Constant extends StackValue {
        private final Object value;

        public Constant(Object value) {
            this.value = value;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.aconst(value);
            if (type.getSort() == Type.OBJECT) {
                if (value instanceof Integer) {
                    v.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
                }
                else if (value instanceof Boolean) {
                    v.invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
                }
                else if (value instanceof Character) {
                    v.invokestatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
                }
                else if (value instanceof Short) {
                    v.invokestatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
                }
                else if (value instanceof Long) {
                    v.invokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
                }
                else if (value instanceof Byte) {
                    v.invokestatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
                }
                else if (value instanceof Float) {
                    v.invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
                }
                else if (value instanceof Double) {
                    v.invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
                }
            }
        }

        @Override
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            if (value instanceof Boolean) {
                boolean boolValue = ((Boolean) value).booleanValue();
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
        private final IElementType opToken;
        private final Type type;

        public NumberCompare(IElementType opToken, Type type) {
            this.opToken = opToken;
            this.type = type;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            Label ifTrue = new Label();
            Label end = new Label();
            condJump(ifTrue, false, v);
            v.iconst(0);
            v.goTo(end);
            v.mark(ifTrue);
            v.iconst(1);
            v.mark(end);
        }

        @Override
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            int opcode;
            if (opToken == JetTokens.EQEQ) {
                opcode = jumpIfFalse ? Opcodes.IFNE : Opcodes.IFEQ;
            }
            else if (opToken == JetTokens.EXCLEQ) {
                opcode = jumpIfFalse ? Opcodes.IFEQ : Opcodes.IFNE;
            }
            else if (opToken == JetTokens.GT) {
                opcode = jumpIfFalse ? Opcodes.IFLE : Opcodes.IFGT;
            }
            else if (opToken == JetTokens.GTEQ) {
                opcode = jumpIfFalse ? Opcodes.IFLT : Opcodes.IFGE;
            }
            else if (opToken == JetTokens.LT) {
                opcode = jumpIfFalse ? Opcodes.IFGE : Opcodes.IFLT;
            }
            else if (opToken == JetTokens.LTEQ) {
                opcode = jumpIfFalse ? Opcodes.IFGT : Opcodes.IFLE;
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate this condjump");
            }
            if (type == Type.FLOAT_TYPE || type == Type.DOUBLE_TYPE) {
                if (opToken == JetTokens.GT || opToken == JetTokens.GTEQ) {
                    v.cmpg(type);
                }
                else {
                    v.cmpl(type);
                }
            }
            else if (type == Type.LONG_TYPE) {
                v.lcmp();
            }
            else {
                opcode += (Opcodes.IF_ICMPEQ - Opcodes.IFEQ);
            }
            v.visitJumpInsn(opcode, label);
        }
    }
}
