package org.jetbrains.jet.lang.types;

import java.util.Collections;

/**
 * @author abreslav
 */
public class JetStandardTypes {
    private static final ClassType ANY_TYPE = new ClassType(JetStandardClasses.getAny());
    private static final ClassType BYTE = new ClassType(JetStandardClasses.getByte());
    private static final ClassType CHAR = new ClassType(JetStandardClasses.getChar());
    private static final ClassType SHORT = new ClassType(JetStandardClasses.getShort());
    private static final ClassType INT = new ClassType(JetStandardClasses.getInt());
    private static final ClassType LONG = new ClassType(JetStandardClasses.getLong());
    private static final ClassType FLOAT = new ClassType(JetStandardClasses.getFloat());
    private static final ClassType DOUBLE = new ClassType(JetStandardClasses.getDouble());
    private static final ClassType BOOLEAN = new ClassType(JetStandardClasses.getBoolean());
    private static final ClassType STRING = new ClassType(JetStandardClasses.getString());
    private static final TupleType UNIT = TupleType.UNIT;

    public static ClassType getInt() {
        return INT;
    }

    public static ClassType getLong() {
        return LONG;
    }

    public static ClassType getDouble() {
        return DOUBLE;
    }

    public static ClassType getFloat() {
        return FLOAT;
    }

    public static ClassType getChar() {
        return CHAR;
    }

    public static ClassType getBoolean() {
        return BOOLEAN;
    }

    public static ClassType getString() {
        return STRING;
    }

    public static ClassType getByte() {
        return BYTE;
    }

    public static ClassType getShort() {
        return SHORT;
    }

    public static TupleType getUnit() {
        return UNIT;
    }

    public static Type getAny() {
        return ANY_TYPE;
    }
}
