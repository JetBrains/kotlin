package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.WritableScope;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author abreslav
 */
public class JetStandardClasses {

    private JetStandardClasses() {
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static ClassDescriptor NOTHING_CLASS = new ClassDescriptorImpl(
            null,
            Collections.<Attribute>emptyList(),
            true,
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
            }, JetScope.EMPTY
    );
    private static final Type NOTHING_TYPE = new TypeImpl(getNothing());

    private static final Type NULLABLE_NOTHING_TYPE = new TypeImpl(
            Collections.<Attribute>emptyList(),
            getNothing().getTypeConstructor(),
            true,
            Collections.<TypeProjection>emptyList(),
            JetScope.EMPTY);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final ClassDescriptor ANY = new ClassDescriptorImpl(
            null,
            Collections.<Attribute>emptyList(),
            false,
            "Any",
            Collections.<TypeParameterDescriptor>emptyList(),
            Collections.<Type>emptySet(),
            JetScope.EMPTY
    );
    private static final Type ANY_TYPE = new TypeImpl(ANY.getTypeConstructor(), JetScope.EMPTY);

    private static final Type NULLABLE_ANY_TYPE = TypeUtils.makeNullable(ANY_TYPE);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final JetScope STUB = JetScope.EMPTY;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int TUPLE_COUNT = 22;
    private static final ClassDescriptor[] TUPLE = new ClassDescriptor[TUPLE_COUNT];

    static {
        for (int i = 0; i < TUPLE_COUNT; i++) {
            List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
            for (int j = 0; j < i; j++) {
                parameters.add(new TypeParameterDescriptor(
                        null,
                        Collections.<Attribute>emptyList(),
                        Variance.OUT_VARIANCE, "T" + j,
                        Collections.singleton(getNullableAnyType())));
            }
            TUPLE[i] = new ClassDescriptorImpl(
                    null,
                    Collections.<Attribute>emptyList(),
                    true,
                    "Tuple" + i,
                    parameters,
                    Collections.singleton(getAnyType()), STUB);
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int FUNCTION_COUNT = 22;
    private static final ClassDescriptor[] FUNCTION = new ClassDescriptor[FUNCTION_COUNT];

    private static final ClassDescriptor[] RECEIVER_FUNCTION = new ClassDescriptor[FUNCTION_COUNT];

    static {
        for (int i = 0; i < FUNCTION_COUNT; i++) {
            List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
            for (int j = 0; j < i; j++) {
                parameters.add(new TypeParameterDescriptor(
                        null,
                        Collections.<Attribute>emptyList(),
                        Variance.IN_VARIANCE, "P" + j,
                        Collections.singleton(getNullableAnyType())));
            }
            parameters.add(new TypeParameterDescriptor(
                        null,
                        Collections.<Attribute>emptyList(),
                        Variance.OUT_VARIANCE, "R",
                        Collections.singleton(getNullableAnyType())));
            FUNCTION[i] = new ClassDescriptorImpl(
                    null,
                    Collections.<Attribute>emptyList(),
                    false,
                    "Function" + i,
                    parameters,
                    Collections.singleton(getAnyType()), STUB);
            parameters.add(0, new TypeParameterDescriptor(
                        null,
                        Collections.<Attribute>emptyList(),
                        Variance.IN_VARIANCE, "T",
                        Collections.singleton(getNullableAnyType())));
            RECEIVER_FUNCTION[i] = new ClassDescriptorImpl(
                    null,
                    Collections.<Attribute>emptyList(),
                    false,
                    "ReceiverFunction" + i,
                    parameters,
                    Collections.singleton(getAnyType()), STUB);
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Type UNIT_TYPE = new TypeImpl(getTuple(0));

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    /*package*/ static final JetScope STANDARD_CLASSES;

    static {
        WritableScope writableScope = new WritableScope(JetScope.EMPTY);
        STANDARD_CLASSES = writableScope;
        writableScope.addClassAlias("Unit", getTuple(0));

        Field[] declaredFields = JetStandardClasses.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }
            Class<?> type = field.getType();
            if (type == ClassDescriptor.class) {
                try {
                    ClassDescriptor descriptor = (ClassDescriptor) field.get(null);
                    writableScope.addClassDescriptor(descriptor);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            } else if (type.isArray() && type.getComponentType() == ClassDescriptor.class) {
                try {
                    ClassDescriptor[] array = (ClassDescriptor[]) field.get(null);
                    for (ClassDescriptor descriptor : array) {
                        writableScope.addClassDescriptor(descriptor);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public static ClassDescriptor getAny() {
        return ANY;
    }

    @NotNull
    public static Type getAnyType() {
        return ANY_TYPE;
    }

    public static Type getNullableAnyType() {
        return NULLABLE_ANY_TYPE;
    }

    @NotNull
    public static ClassDescriptor getNothing() {
        return NOTHING_CLASS;
    }

    @NotNull
    public static ClassDescriptor getTuple(int size) {
        return TUPLE[size];
    }

    @NotNull
    public static ClassDescriptor getFunction(int parameterCount) {
        return FUNCTION[parameterCount];
    }

    @NotNull
    public static ClassDescriptor getReceiverFunction(int parameterCount) {
        return RECEIVER_FUNCTION[parameterCount];
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

    public static boolean isNothing(Type type) {
        return type.getConstructor() == NOTHING_CLASS.getTypeConstructor();
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

    public static Type getLabeledTupleType(List<Attribute> attributes, List<ValueParameterDescriptor> arguments) {
        // TODO
        return getTupleType(attributes, toTypes(arguments));
    }

    public static Type getLabeledTupleType(List<ValueParameterDescriptor> arguments) {
        // TODO
        return getLabeledTupleType(Collections.<Attribute>emptyList(), arguments);
    }

    private static List<TypeProjection> toProjections(List<Type> arguments) {
        List<TypeProjection> result = new ArrayList<TypeProjection>();
        for (Type argument : arguments) {
            result.add(new TypeProjection(Variance.OUT_VARIANCE, argument));
        }
        return result;
    }

    private static List<Type> toTypes(List<ValueParameterDescriptor> labeledEntries) {
        List<Type> result = new ArrayList<Type>();
        for (ValueParameterDescriptor entry : labeledEntries) {
            result.add(entry.getType());
        }
        return result;
    }

    // TODO : labeled version?
    public static Type getFunctionType(List<Attribute> attributes, @Nullable Type receiverType, @NotNull List<Type> parameterTypes, @NotNull Type returnType) {
        List<TypeProjection> arguments = new ArrayList<TypeProjection>();
        if (receiverType != null) {
            arguments.add(defaultProjection(receiverType));
        }
        for (Type parameterType : parameterTypes) {
            arguments.add(defaultProjection(parameterType));
        }
        arguments.add(defaultProjection(returnType));
        int size = parameterTypes.size();
        TypeConstructor constructor = receiverType == null ? FUNCTION[size].getTypeConstructor() : RECEIVER_FUNCTION[size].getTypeConstructor();
        return new TypeImpl(attributes, constructor, false, arguments, STUB);
    }

    private static TypeProjection defaultProjection(Type returnType) {
        return new TypeProjection(Variance.INVARIANT, returnType);
    }
}
