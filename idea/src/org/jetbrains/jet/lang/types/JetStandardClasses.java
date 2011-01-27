package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author abreslav
 */
public class JetStandardClasses {
    private static final TypeConstructor NOTHING = new TypeConstructor(
            Collections.<Attribute>emptyList(),
            "Nothing",
            Collections.<TypeParameterDescriptor>emptyList(),
            new AbstractCollection<Type>() {

                @Override
                public boolean contains(Object o) {
                    return o instanceof Type;
                }

                @Override
                public Iterator<Type> iterator() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }
            });

    private static final ClassDescriptor ANY = new ClassDescriptor(
            Collections.<Attribute>emptyList(),
            "Any",
            Collections.<TypeParameterDescriptor>emptyList(),
            Collections.<Type>emptySet()
    );

    private static final Type ANY_TYPE = new TypeImpl(ANY.getTypeConstructor(), TypeMemberDomain.EMPTY);

    private static final ClassDescriptor BYTE    = new ClassDescriptor("Byte");
    private static final ClassDescriptor CHAR    = new ClassDescriptor("Char");
    private static final ClassDescriptor SHORT   = new ClassDescriptor("Short");
    private static final ClassDescriptor INT     = new ClassDescriptor("Int");
    private static final ClassDescriptor LONG    = new ClassDescriptor("Long");
    private static final ClassDescriptor FLOAT   = new ClassDescriptor("Float");
    private static final ClassDescriptor DOUBLE  = new ClassDescriptor("Double");
    private static final ClassDescriptor BOOLEAN = new ClassDescriptor("Boolean");
    private static final ClassDescriptor STRING  = new ClassDescriptor("String");

    public static final int TUPLE_COUNT = 22;
    private static final ClassDescriptor[] TUPLE = new ClassDescriptor[TUPLE_COUNT];
    static {
        for (int i = 0; i < TUPLE_COUNT; i++) {
            List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
            for (int j = 0; j < i; j++) {
                parameters.add(new TypeParameterDescriptor(
                        Collections.<Attribute>emptyList(),
                        Variance.OUT_VARIANCE, "T" + j,
                        Collections.<Type>emptySet()));
            }
            TUPLE[i] = new ClassDescriptor(
                    Collections.<Attribute>emptyList(),
                    "Tuple" + i,
                    parameters,
                    Collections.singleton(JetStandardClasses.getAnyType()));
        }
    }

    public static final TypeMemberDomain STUB = new TypeMemberDomain() {
        @Override
        public ClassDescriptor getClassDescriptor(@NotNull Type type) {
            throw new UnsupportedOperationException(); // TODO
        }
    };

    private static final Type BYTE_TYPE = new TypeImpl(getByte().getTypeConstructor(), STUB);
    private static final Type CHAR_TYPE = new TypeImpl(getChar().getTypeConstructor(), STUB);
    private static final Type SHORT_TYPE = new TypeImpl(getShort().getTypeConstructor(), STUB);
    private static final Type INT_TYPE = new TypeImpl(getInt().getTypeConstructor(), STUB);
    private static final Type LONG_TYPE = new TypeImpl(getLong().getTypeConstructor(), STUB);
    private static final Type FLOAT_TYPE = new TypeImpl(getFloat().getTypeConstructor(), STUB);
    private static final Type DOUBLE_TYPE = new TypeImpl(getDouble().getTypeConstructor(), STUB);
    private static final Type BOOLEAN_TYPE = new TypeImpl(getBoolean().getTypeConstructor(), STUB);
    private static final Type STRING_TYPE = new TypeImpl(getString().getTypeConstructor(), STUB);
    private static final Type UNIT_TYPE = new TypeImpl(getTuple(0).getTypeConstructor(), STUB);
    private static final Type NOTHING_TYPE = new TypeImpl(NOTHING, STUB);
    private static final Type NULLABLE_NOTHING_TYPE = new TypeImpl(Collections.<Attribute>emptyList(), NOTHING, true, Collections.<TypeProjection>emptyList(), TypeMemberDomain.EMPTY);

    @NotNull
    public static ClassDescriptor getAny() {
        return ANY;
    }

    @NotNull
    public static Type getAnyType() {
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

    public static Type getIntType() {
        return INT_TYPE;
    }

    public static Type getLongType() {
        return LONG_TYPE;
    }

    public static Type getDoubleType() {
        return DOUBLE_TYPE;
    }

    public static Type getFloatType() {
        return FLOAT_TYPE;
    }

    public static Type getCharType() {
        return CHAR_TYPE;
    }

    public static Type getBooleanType() {
        return BOOLEAN_TYPE;
    }

    public static Type getStringType() {
        return STRING_TYPE;
    }

    public static Type getByteType() {
        return BYTE_TYPE;
    }

    public static Type getShortType() {
        return SHORT_TYPE;
    }

    public static Type getUnitType() {
        return UNIT_TYPE;
    }

    public static Type getNothingType() {
        return NOTHING_TYPE;
    }

    public static Type getNullableNothingType() {
        return NULLABLE_NOTHING_TYPE;
    }

    public static Type getTupleType(List<Attribute> attributes, List<Type> arguments) {
        if (attributes.isEmpty() && arguments.isEmpty()) {
            return getUnitType();
        }
        return new TypeImpl(attributes, getTuple(arguments.size()).getTypeConstructor(), false, toProjections(arguments), STUB);
    }

    public static Type getTupleType(List<Type> arguments) {
        return getTupleType(Collections.<Attribute>emptyList(), arguments);
    }

    public static Type getTupleType(Type... arguments) {
        return getTupleType(Collections.<Attribute>emptyList(), Arrays.asList(arguments));
    }

    public static Type getLabeledTupleType(List<Attribute> attributes, List<ParameterDescriptor> arguments) {
        // TODO
        return getTupleType(attributes, toTypes(arguments));
    }

    public static Type getLabeledTupleType(List<ParameterDescriptor> arguments) {
        // TODO
        return getLabeledTupleType(Collections.<Attribute>emptyList(), arguments);
    }

    private static List<TypeProjection> toProjections(List<Type> arguments) {
        List<TypeProjection> result = new ArrayList<TypeProjection>();
        for (Type argument : arguments) {
            result.add(new TypeProjection(ProjectionKind.OUT_ONLY, argument));
        }
        return result;
    }

    private static List<Type> toTypes(List<ParameterDescriptor> labeledEntries) {
        List<Type> result = new ArrayList<Type>();
        for (ParameterDescriptor entry : labeledEntries) {
            result.add(entry.getType());
        }
        return result;
    }
}
