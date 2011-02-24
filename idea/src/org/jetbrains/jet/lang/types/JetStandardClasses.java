package org.jetbrains.jet.lang.types;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetFileType;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.FileContentsResolver;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.JetScopeImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author abreslav
 */
public class JetStandardClasses {

    private static ClassDescriptor NOTHING_CLASS = new ClassDescriptorImpl(
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

    private static final ClassDescriptor ANY = new ClassDescriptorImpl(
            Collections.<Attribute>emptyList(),
            false,
            "Any",
            Collections.<TypeParameterDescriptor>emptyList(),
            Collections.<Type>emptySet(),
            JetScope.EMPTY
    );

    public static final JetScope STUB = JetScope.EMPTY;

    private static final Type ANY_TYPE = new TypeImpl(ANY.getTypeConstructor(), JetScope.EMPTY);

    private static final JetScope LIBRARY_SCOPE;
    static {
        // TODO : review
        Project project = ProjectManager.getInstance().getDefaultProject();
        InputStream stream = JetStandardClasses.class.getClassLoader().getResourceAsStream("jet/lang/Library.jet");
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            JetFile file = (JetFile) PsiFileFactory.getInstance(project).createFileFromText("Library.jet", JetFileType.INSTANCE, FileUtil.loadTextAndClose(new InputStreamReader(stream)));

            LIBRARY_SCOPE = FileContentsResolver.INSTANCE.resolveFileContents(JetScope.EMPTY, file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Type NULLABLE_ANY_TYPE = TypeUtils.makeNullable(ANY_TYPE);
    @NotNull
    private static final ClassDescriptor BYTE    = LIBRARY_SCOPE.getClass("Byte");
    @NotNull
    private static final ClassDescriptor CHAR    = LIBRARY_SCOPE.getClass("Char");
    @NotNull
    private static final ClassDescriptor SHORT   = LIBRARY_SCOPE.getClass("Short");
    @NotNull
    private static final ClassDescriptor INT     = LIBRARY_SCOPE.getClass("Int");
    @NotNull
    private static final ClassDescriptor LONG    = LIBRARY_SCOPE.getClass("Long");
    @NotNull
    private static final ClassDescriptor FLOAT   = LIBRARY_SCOPE.getClass("Float");
    @NotNull
    private static final ClassDescriptor DOUBLE  = LIBRARY_SCOPE.getClass("Double");
    @NotNull
    private static final ClassDescriptor BOOLEAN = LIBRARY_SCOPE.getClass("Boolean");
    @NotNull
    private static final ClassDescriptor STRING  = LIBRARY_SCOPE.getClass("String");

    public static final int TUPLE_COUNT = 22;
    private static final ClassDescriptor[] TUPLE = new ClassDescriptor[TUPLE_COUNT];

    static {
        for (int i = 0; i < TUPLE_COUNT; i++) {
            List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
            for (int j = 0; j < i; j++) {
                parameters.add(new TypeParameterDescriptor(
                        Collections.<Attribute>emptyList(),
                        Variance.OUT_VARIANCE, "T" + j,
                        Collections.singleton(getNullableAnyType())));
            }
            TUPLE[i] = new ClassDescriptorImpl(
                    Collections.<Attribute>emptyList(),
                    true,
                    "Tuple" + i,
                    parameters,
                    Collections.singleton(JetStandardClasses.getAnyType()), STUB);
        }
    }
    public static final int FUNCTION_COUNT = 22;
    private static final ClassDescriptor[] FUNCTION = new ClassDescriptor[FUNCTION_COUNT];
    private static final ClassDescriptor[] RECEIVER_FUNCTION = new ClassDescriptor[FUNCTION_COUNT];


    static {
        for (int i = 0; i < FUNCTION_COUNT; i++) {
            List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
            for (int j = 0; j < i; j++) {
                parameters.add(new TypeParameterDescriptor(
                        Collections.<Attribute>emptyList(),
                        Variance.IN_VARIANCE, "P" + j,
                        Collections.singleton(getNullableAnyType())));
            }
            parameters.add(new TypeParameterDescriptor(
                        Collections.<Attribute>emptyList(),
                        Variance.OUT_VARIANCE, "R",
                        Collections.singleton(getNullableAnyType())));
            FUNCTION[i] = new ClassDescriptorImpl(
                    Collections.<Attribute>emptyList(),
                    false,
                    "Function" + i,
                    parameters,
                    Collections.singleton(JetStandardClasses.getAnyType()), STUB);
            parameters.add(0, new TypeParameterDescriptor(
                        Collections.<Attribute>emptyList(),
                        Variance.IN_VARIANCE, "T",
                        Collections.singleton(getNullableAnyType())));
            RECEIVER_FUNCTION[i] = new ClassDescriptorImpl(
                    Collections.<Attribute>emptyList(),
                    false,
                    "ReceiverFunction" + i,
                    parameters,
                    Collections.singleton(JetStandardClasses.getAnyType()), STUB);
        }
    }

    private static final Type BYTE_TYPE = new TypeImpl(getByte());
    private static final Type CHAR_TYPE = new TypeImpl(getChar());
    private static final Type SHORT_TYPE = new TypeImpl(getShort());
    private static final Type INT_TYPE = new TypeImpl(getInt());
    private static final Type LONG_TYPE = new TypeImpl(getLong());
    private static final Type FLOAT_TYPE = new TypeImpl(getFloat());
    private static final Type DOUBLE_TYPE = new TypeImpl(getDouble());
    private static final Type BOOLEAN_TYPE = new TypeImpl(getBoolean());
    private static final Type STRING_TYPE = new TypeImpl(getString());
    private static final Type UNIT_TYPE = new TypeImpl(getTuple(0));
    private static final Type NOTHING_TYPE = new TypeImpl(getNothing());

    private static final Type NULLABLE_NOTHING_TYPE = new TypeImpl(
            Collections.<Attribute>emptyList(),
            getNothing().getTypeConstructor(),
            true,
            Collections.<TypeProjection>emptyList(),
            JetScope.EMPTY);

    private static final Map<String, ClassDescriptor> CLASS_MAP = new HashMap<String, ClassDescriptor>();
    static {
        Field[] declaredFields = JetStandardClasses.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }
            Class<?> type = field.getType();
            if (type == ClassDescriptor.class) {
                try {
                    ClassDescriptor descriptor = (ClassDescriptor) field.get(null);
                    CLASS_MAP.put(descriptor.getName(), descriptor);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            } else if (type.isArray() && type.getComponentType() == ClassDescriptor.class) {
                try {
                    ClassDescriptor[] array = (ClassDescriptor[]) field.get(null);
                    for (ClassDescriptor descriptor : array) {
                        CLASS_MAP.put(descriptor.getName(), descriptor);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        CLASS_MAP.put("Unit", getTuple(0));
    }

    @NotNull
    public static final JetScope STANDARD_CLASSES = new JetScopeImpl() {
        @Override
        public ClassDescriptor getClass(String name) {
            return CLASS_MAP.get(name);
        }
    };

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
