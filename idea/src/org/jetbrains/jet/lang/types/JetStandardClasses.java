package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static final ClassDescriptor UNIT    = new ClassDescriptor("Unit");

    public static final int TUPLE_COUNT = 22;
    private static final ClassDescriptor[] TUPLE = new ClassDescriptor[TUPLE_COUNT];
    static {
        for (int i = 0; i < TUPLE_COUNT; i++) {
            List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
            for (int j = 0; j < i; j++) {
                parameters.add(new TypeParameterDescriptor(
                        Collections.<Annotation>emptyList(),
                        "T" + j,
                        Variance.OUT_VARIANCE,
                        Collections.<Type>emptySet()));
            }
            TUPLE[i] = new ClassDescriptor(
                    Collections.<Annotation>emptyList(),
                    "Tuple" + i,
                    parameters,
                    Collections.singleton(JetStandardClasses.getAnyType()));
        }
    }

    @NotNull
    public static ClassDescriptor getAny() {
        return ANY;
    }

    @NotNull
    public static ClassType getAnyType() {
        return ANY_TYPE;
    }

    @NotNull
    public static ClassDescriptor getByte() {
        return BYTE;
    }

    @NotNull
    public static ClassDescriptor getChar() {
        return CHAR;
    }

    @NotNull
    public static ClassDescriptor getShort() {
        return SHORT;
    }

    @NotNull
    public static ClassDescriptor getInt() {
        return INT;
    }

    @NotNull
    public static ClassDescriptor getLong() {
        return LONG;
    }

    @NotNull
    public static ClassDescriptor getFloat() {
        return FLOAT;
    }

    @NotNull
    public static ClassDescriptor getDouble() {
        return DOUBLE;
    }

    @NotNull
    public static ClassDescriptor getBoolean() {
        return BOOLEAN;
    }

    @NotNull
    public static ClassDescriptor getString() {
        return STRING;
    }

    @NotNull
    public static ClassDescriptor getTuple(int size) {
        return TUPLE[size];
    }

    @NotNull
    public static ClassDescriptor getLabeledTuple(int size) {
        // TODO
        return getTuple(size);
    }
}
