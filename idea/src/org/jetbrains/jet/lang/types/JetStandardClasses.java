package org.jetbrains.jet.lang.types;

import java.util.Collections;

/**
 * @author abreslav
 */
public class JetStandardClasses {
    private static final ClassDescriptor ANY = new ClassDescriptor(
            Collections.<Annotation>emptyList(),
            "Any",
            Collections.<TypeParameterDescriptor>emptyList(),
            Collections.<Type>emptySet()
    );

    private static final ClassType ANY_TYPE = new ClassType(ANY);

    private static final ClassDescriptor BYTE    = new ClassDescriptor("Byte");
    private static final ClassDescriptor CHAR    = new ClassDescriptor("Char");
    private static final ClassDescriptor SHORT   = new ClassDescriptor("Short");
    private static final ClassDescriptor INT     = new ClassDescriptor("Int");
    private static final ClassDescriptor LONG    = new ClassDescriptor("Long");
    private static final ClassDescriptor FLOAT   = new ClassDescriptor("Float");
    private static final ClassDescriptor DOUBLE  = new ClassDescriptor("Double");
    private static final ClassDescriptor BOOLEAN = new ClassDescriptor("Boolean");
    private static final ClassDescriptor STRING  = new ClassDescriptor("String");

    public static ClassDescriptor getAny() {
        return ANY;
    }

    public static ClassType getAnyType() {
        return ANY_TYPE;
    }

    public static ClassDescriptor getByte() {
        return BYTE;
    }

    public static ClassDescriptor getChar() {
        return CHAR;
    }

    public static ClassDescriptor getShort() {
        return SHORT;
    }

    public static ClassDescriptor getInt() {
        return INT;
    }

    public static ClassDescriptor getLong() {
        return LONG;
    }

    public static ClassDescriptor getFloat() {
        return FLOAT;
    }

    public static ClassDescriptor getDouble() {
        return DOUBLE;
    }

    public static ClassDescriptor getBoolean() {
        return BOOLEAN;
    }

    public static ClassDescriptor getString() {
        return STRING;
    }
}
