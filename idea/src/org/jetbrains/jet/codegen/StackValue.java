package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.types.JetType;
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
    }

    public static class OnStack extends StackValue {
        private final Type type;

        public OnStack(Type type) {
            this.type = type;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
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
    }
}
