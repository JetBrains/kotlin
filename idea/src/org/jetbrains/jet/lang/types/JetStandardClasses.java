package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.WritableScope;
import org.jetbrains.jet.lang.resolve.WritableScopeImpl;

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

    private static NamespaceDescriptor STANDARD_CLASSES_NAMESPACE = new NamespaceDescriptor(null, Collections.<Annotation>emptyList(), "jet");

    private static final ClassDescriptor NOTHING_CLASS = new ClassDescriptorImpl(
            STANDARD_CLASSES_NAMESPACE,
            Collections.<Annotation>emptyList(),
            "Nothing"
    ).initialize(
            true,
            Collections.<TypeParameterDescriptor>emptyList(),
            new AbstractCollection<JetType>() {
                @Override
                public boolean contains(Object o) {
                    return o instanceof JetType;
                }

                @Override
                public Iterator<JetType> iterator() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }
            },
            JetScope.EMPTY,
            FunctionGroup.EMPTY,
            null
    );

    private static final JetType NOTHING_TYPE = new JetTypeImpl(getNothing());
    private static final JetType NULLABLE_NOTHING_TYPE = new JetTypeImpl(
            Collections.<Annotation>emptyList(),
            getNothing().getTypeConstructor(),
            true,
            Collections.<TypeProjection>emptyList(),
            JetScope.EMPTY);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final ClassDescriptor ANY = new ClassDescriptorImpl(
            STANDARD_CLASSES_NAMESPACE,
            Collections.<Annotation>emptyList(),
            "Any").initialize(
            false,
            Collections.<TypeParameterDescriptor>emptyList(),
            Collections.<JetType>emptySet(),
            JetScope.EMPTY,
            FunctionGroup.EMPTY,
            null
    );

    private static final JetType ANY_TYPE = new JetTypeImpl(ANY.getTypeConstructor(), JetScope.EMPTY);
    private static final JetType NULLABLE_ANY_TYPE = TypeUtils.makeNullable(ANY_TYPE);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final JetType DEFAULT_BOUND = getNullableAnyType();

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final JetScope STUB = JetScope.EMPTY;
    public static final FunctionGroup STUB_FG = FunctionGroup.EMPTY;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int TUPLE_COUNT = 22;

    private static final ClassDescriptor[] TUPLE = new ClassDescriptor[TUPLE_COUNT];
    static {
        for (int i = 0; i < TUPLE_COUNT; i++) {
            List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
            ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                    STANDARD_CLASSES_NAMESPACE,
                    Collections.<Annotation>emptyList(),
                    "Tuple" + i);
            for (int j = 0; j < i; j++) {
                parameters.add(TypeParameterDescriptor.createWithDefaultBound(
                        classDescriptor,
                        Collections.<Annotation>emptyList(),
                        Variance.OUT_VARIANCE, "T" + j, j));
            }
            TUPLE[i] = classDescriptor.initialize(
                    true,
                    parameters,
                    Collections.singleton(getAnyType()),
                    STUB,
                    STUB_FG,
                    null); // TODO : constructor
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int FUNCTION_COUNT = 22;

    private static final ClassDescriptor[] FUNCTION = new ClassDescriptor[FUNCTION_COUNT];
    private static final ClassDescriptor[] RECEIVER_FUNCTION = new ClassDescriptor[FUNCTION_COUNT];

    static {
        for (int i = 0; i < FUNCTION_COUNT; i++) {
            ClassDescriptorImpl function = new ClassDescriptorImpl(
                    STANDARD_CLASSES_NAMESPACE,
                    Collections.<Annotation>emptyList(),
                    "Function" + i);
            FUNCTION[i] = function.initialize(
                    false,
                    createTypeParameters(i, function),
                    Collections.singleton(getAnyType()), STUB, FunctionGroup.EMPTY, null);

            ClassDescriptorImpl receiverFunction = new ClassDescriptorImpl(
                    STANDARD_CLASSES_NAMESPACE,
                    Collections.<Annotation>emptyList(),
                    "ReceiverFunction" + i);
            List<TypeParameterDescriptor> parameters = createTypeParameters(i, receiverFunction);
            parameters.add(0, TypeParameterDescriptor.createWithDefaultBound(
                    receiverFunction,
                    Collections.<Annotation>emptyList(),
                    Variance.IN_VARIANCE, "T", 0));
            RECEIVER_FUNCTION[i] = receiverFunction.initialize(
                    false,
                    parameters,
                    Collections.singleton(getAnyType()), STUB, FunctionGroup.EMPTY, null);
        }
    }

    private static List<TypeParameterDescriptor> createTypeParameters(int parameterCount, ClassDescriptorImpl function) {
        List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
        for (int j = 0; j < parameterCount; j++) {
            parameters.add(TypeParameterDescriptor.createWithDefaultBound(
                    function,
                    Collections.<Annotation>emptyList(),
                    Variance.IN_VARIANCE, "P" + j, j + 1));
        }
        parameters.add(TypeParameterDescriptor.createWithDefaultBound(
                function,
                Collections.<Annotation>emptyList(),
                Variance.OUT_VARIANCE, "R", parameterCount + 1));
        return parameters;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final JetType UNIT_TYPE = new JetTypeImpl(getTuple(0));

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    /*package*/ static final JetScope STANDARD_CLASSES;

    static {
        WritableScope writableScope = new WritableScopeImpl(JetScope.EMPTY, STANDARD_CLASSES_NAMESPACE, ErrorHandler.DO_NOTHING, null);
        STANDARD_CLASSES = writableScope;
        writableScope.addClassifierAlias("Unit", getTuple(0));

        Field[] declaredFields = JetStandardClasses.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }
            Class<?> type = field.getType();
            if (type == ClassDescriptor.class) {
                try {
                    ClassDescriptor descriptor = (ClassDescriptor) field.get(null);
                    writableScope.addClassifierDescriptor(descriptor);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            } else if (type.isArray() && type.getComponentType() == ClassDescriptor.class) {
                try {
                    ClassDescriptor[] array = (ClassDescriptor[]) field.get(null);
                    for (ClassDescriptor descriptor : array) {
                        writableScope.addClassifierDescriptor(descriptor);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public static JetType getDefaultBound() {
        return DEFAULT_BOUND;
    }

    @NotNull
    public static ClassDescriptor getAny() {
        return ANY;
    }

    @NotNull
    public static JetType getAnyType() {
        return ANY_TYPE;
    }

    public static JetType getNullableAnyType() {
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

    public static JetType getUnitType() {
        return UNIT_TYPE;
    }

    public static JetType getNothingType() {
        return NOTHING_TYPE;
    }

    public static JetType getNullableNothingType() {
        return NULLABLE_NOTHING_TYPE;
    }

    public static boolean isNothing(@NotNull JetType type) {
        return !(type instanceof NamespaceType) &&
               type.getConstructor() == NOTHING_CLASS.getTypeConstructor();
    }

    public static JetType getTupleType(List<Annotation> annotations, List<JetType> arguments) {
        if (annotations.isEmpty() && arguments.isEmpty()) {
            return getUnitType();
        }
        return new JetTypeImpl(annotations, getTuple(arguments.size()).getTypeConstructor(), false, toProjections(arguments), STUB);
    }

    public static JetType getTupleType(List<JetType> arguments) {
        return getTupleType(Collections.<Annotation>emptyList(), arguments);
    }

    public static JetType getTupleType(JetType... arguments) {
        return getTupleType(Collections.<Annotation>emptyList(), Arrays.asList(arguments));
    }

    public static JetType getLabeledTupleType(List<Annotation> annotations, List<ValueParameterDescriptor> arguments) {
        // TODO
        return getTupleType(annotations, toTypes(arguments));
    }

    public static JetType getLabeledTupleType(List<ValueParameterDescriptor> arguments) {
        // TODO
        return getLabeledTupleType(Collections.<Annotation>emptyList(), arguments);
    }

    private static List<TypeProjection> toProjections(List<JetType> arguments) {
        List<TypeProjection> result = new ArrayList<TypeProjection>();
        for (JetType argument : arguments) {
            result.add(new TypeProjection(Variance.OUT_VARIANCE, argument));
        }
        return result;
    }

    private static List<JetType> toTypes(List<ValueParameterDescriptor> labeledEntries) {
        List<JetType> result = new ArrayList<JetType>();
        for (ValueParameterDescriptor entry : labeledEntries) {
            result.add(entry.getOutType());
        }
        return result;
    }

    // TODO : labeled version?
    public static JetType getFunctionType(List<Annotation> annotations, @Nullable JetType receiverType, @NotNull List<JetType> parameterTypes, @NotNull JetType returnType) {
        List<TypeProjection> arguments = new ArrayList<TypeProjection>();
        if (receiverType != null) {
            arguments.add(defaultProjection(receiverType));
        }
        for (JetType parameterType : parameterTypes) {
            arguments.add(defaultProjection(parameterType));
        }
        arguments.add(defaultProjection(returnType));
        int size = parameterTypes.size();
        TypeConstructor constructor = receiverType == null ? FUNCTION[size].getTypeConstructor() : RECEIVER_FUNCTION[size].getTypeConstructor();
        return new JetTypeImpl(annotations, constructor, false, arguments, STUB);
    }

    private static TypeProjection defaultProjection(JetType returnType) {
        return new TypeProjection(Variance.INVARIANT, returnType);
    }
}
