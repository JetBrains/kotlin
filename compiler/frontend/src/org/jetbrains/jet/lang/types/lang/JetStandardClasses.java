/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.types.lang;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.Variance;

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

    public static final ModuleDescriptor FAKE_STANDARD_CLASSES_MODULE = new ModuleDescriptor("<builtin>");

    private static final NamespaceDescriptorImpl STANDARD_CLASSES_FAKE_ROOT_NS = new NamespaceDescriptorImpl(FAKE_STANDARD_CLASSES_MODULE, Collections.<AnnotationDescriptor>emptyList(), "<root>");

    static {
        FAKE_STANDARD_CLASSES_MODULE.setRootNs(STANDARD_CLASSES_FAKE_ROOT_NS);
    }

    public static NamespaceDescriptorImpl STANDARD_CLASSES_NAMESPACE = new NamespaceDescriptorImpl(STANDARD_CLASSES_FAKE_ROOT_NS, Collections.<AnnotationDescriptor>emptyList(), "jet");
    
    public static final FqName STANDARD_CLASSES_FQNAME = DescriptorUtils.getFQName(STANDARD_CLASSES_NAMESPACE).toSafe();

    private static final ClassDescriptor NOTHING_CLASS = new ClassDescriptorImpl(
            STANDARD_CLASSES_NAMESPACE,
            Collections.<AnnotationDescriptor>emptyList(),
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
                    throw new UnsupportedOperationException("Don't enumerate supertypes of Nothing");
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException("Supertypes of Nothing do not constitute a valid collection");
                }
            },
            JetScope.EMPTY,
            Collections.<ConstructorDescriptor>emptySet(),
            null,
            null
    );

    private static final JetType NOTHING_TYPE = new JetTypeImpl(getNothing());
    private static final JetType NULLABLE_NOTHING_TYPE = new JetTypeImpl(
            Collections.<AnnotationDescriptor>emptyList(),
            getNothing().getTypeConstructor(),
            true,
            Collections.<TypeProjection>emptyList(),
            JetScope.EMPTY);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final ClassDescriptor ANY = new ClassDescriptorImpl(
            STANDARD_CLASSES_NAMESPACE,
            Collections.<AnnotationDescriptor>emptyList(),
            "Any").initialize(
            false,
            Collections.<TypeParameterDescriptor>emptyList(),
            Collections.<JetType>emptySet(),
            JetScope.EMPTY,
            Collections.<ConstructorDescriptor>emptySet(),
            null,
            null
    );

    private static final JetType ANY_TYPE = new JetTypeImpl(ANY.getTypeConstructor(), new JetScopeImpl() {
        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            return STANDARD_CLASSES_NAMESPACE;
        }

        @Override
        public String toString() {
            return "Scope for Any";
        }
    });
    private static final JetType NULLABLE_ANY_TYPE = TypeUtils.makeNullable(ANY_TYPE);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final JetType DEFAULT_BOUND = getNullableAnyType();

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final JetScope STUB = JetScope.EMPTY;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int TUPLE_COUNT = 22;
    
    private static final Set<TypeConstructor> TUPLE_CONSTRUCTORS = Sets.newHashSet();

    private static final ClassDescriptor[] TUPLE = new ClassDescriptor[TUPLE_COUNT];
    static {
        for (int i = 0; i < TUPLE_COUNT; i++) {
            List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
            ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                    STANDARD_CLASSES_NAMESPACE,
                    Collections.<AnnotationDescriptor>emptyList(),
                    "Tuple" + i);
            WritableScopeImpl writableScope = new WritableScopeImpl(JetScope.EMPTY, classDescriptor, RedeclarationHandler.THROW_EXCEPTION);
            for (int j = 0; j < i; j++) {
                TypeParameterDescriptor typeParameterDescriptor = TypeParameterDescriptor.createWithDefaultBound(
                        classDescriptor,
                        Collections.<AnnotationDescriptor>emptyList(),
                        true, Variance.OUT_VARIANCE, "T" + (j + 1), j);
                parameters.add(typeParameterDescriptor);
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(classDescriptor, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, Visibility.PUBLIC, false, false, "_" + (j + 1), CallableMemberDescriptor.Kind.DECLARATION);
                propertyDescriptor.setType(typeParameterDescriptor.getDefaultType(), Collections.<TypeParameterDescriptor>emptyList(), classDescriptor.getImplicitReceiver(), ReceiverDescriptor.NO_RECEIVER);
                PropertyGetterDescriptor getterDescriptor = new PropertyGetterDescriptor(propertyDescriptor, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, Visibility.PUBLIC, false, true, CallableMemberDescriptor.Kind.DECLARATION);
                getterDescriptor.initialize(typeParameterDescriptor.getDefaultType());
                propertyDescriptor.initialize(getterDescriptor, null);
                writableScope.addPropertyDescriptor(propertyDescriptor);
            }
            writableScope.changeLockLevel(WritableScope.LockLevel.READING);
            TUPLE[i] = classDescriptor.initialize(
                    true,
                    parameters,
                    Collections.singleton(getAnyType()),
                    writableScope,
                    Collections.<ConstructorDescriptor>emptySet(), // TODO
                    null); // TODO : constructor
            TUPLE_CONSTRUCTORS.add(TUPLE[i].getTypeConstructor());
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int FUNCTION_COUNT = 22;

    private static final ClassDescriptor[] FUNCTION = new ClassDescriptor[FUNCTION_COUNT];
    private static final ClassDescriptor[] RECEIVER_FUNCTION = new ClassDescriptor[FUNCTION_COUNT];

    private static final Set<TypeConstructor> FUNCTION_TYPE_CONSTRUCTORS = Sets.newHashSet();
    private static final Set<TypeConstructor> RECEIVER_FUNCTION_TYPE_CONSTRUCTORS = Sets.newHashSet();

    static {
        for (int i = 0; i < FUNCTION_COUNT; i++) {
            ClassDescriptorImpl function = new ClassDescriptorImpl(
                    STANDARD_CLASSES_NAMESPACE,
                    Collections.<AnnotationDescriptor>emptyList(),
                    "Function" + i);
            FUNCTION[i] = function.initialize(
                    false,
                    createTypeParameters(i, function),
                    Collections.singleton(getAnyType()), STUB, Collections.<ConstructorDescriptor>emptySet(), null);
            FUNCTION_TYPE_CONSTRUCTORS.add(FUNCTION[i].getTypeConstructor());

            ClassDescriptorImpl receiverFunction = new ClassDescriptorImpl(
                    STANDARD_CLASSES_NAMESPACE,
                    Collections.<AnnotationDescriptor>emptyList(),
                    "ExtensionFunction" + i);
            List<TypeParameterDescriptor> parameters = createTypeParameters(i, receiverFunction);
            parameters.add(0, TypeParameterDescriptor.createWithDefaultBound(
                    receiverFunction,
                    Collections.<AnnotationDescriptor>emptyList(),
                    true, Variance.IN_VARIANCE, "T", 0));
            RECEIVER_FUNCTION[i] = receiverFunction.initialize(
                    false,
                    parameters,
                    Collections.singleton(getAnyType()), STUB, Collections.<ConstructorDescriptor>emptySet(), null);
            RECEIVER_FUNCTION_TYPE_CONSTRUCTORS.add(RECEIVER_FUNCTION[i].getTypeConstructor());
        }
    }

    private static List<TypeParameterDescriptor> createTypeParameters(int parameterCount, ClassDescriptorImpl function) {
        List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
        for (int j = 0; j < parameterCount; j++) {
            parameters.add(TypeParameterDescriptor.createWithDefaultBound(
                    function,
                    Collections.<AnnotationDescriptor>emptyList(),
                    true, Variance.IN_VARIANCE, "P" + j, j + 1));
        }
        parameters.add(TypeParameterDescriptor.createWithDefaultBound(
                function,
                Collections.<AnnotationDescriptor>emptyList(),
                true, Variance.OUT_VARIANCE, "R", parameterCount + 1));
        return parameters;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final JetType UNIT_TYPE = new JetTypeImpl(getTuple(0));

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    /*package*/ static final JetScope STANDARD_CLASSES;

    static {
        WritableScope writableScope = new WritableScopeImpl(JetScope.EMPTY, STANDARD_CLASSES_NAMESPACE, RedeclarationHandler.DO_NOTHING).setDebugName("JetStandardClasses.STANDARD_CLASSES");
        writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);

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
        STANDARD_CLASSES_NAMESPACE.initialize(writableScope);
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
    
    public static boolean isAny(JetType type) {
        return !(type instanceof NamespaceType) &&
               type.getConstructor() == ANY_TYPE.getConstructor();
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
        return isNothingOrNullableNothing(type)
               && !type.isNullable();
    }

    public static boolean isNothingOrNullableNothing(@NotNull JetType type) {
        return !(type instanceof NamespaceType)
               && type.getConstructor() == NOTHING_CLASS.getTypeConstructor();
    }

    public static boolean isUnit(@NotNull JetType type) {
        return !(type instanceof NamespaceType) &&
               type.getConstructor() == UNIT_TYPE.getConstructor();
    }

    public static JetType getTupleType(List<AnnotationDescriptor> annotations, List<JetType> arguments) {
        if (annotations.isEmpty() && arguments.isEmpty()) {
            return getUnitType();
        }
        ClassDescriptor tuple = getTuple(arguments.size());
        List<TypeProjection> typeArguments = toProjections(arguments);
        return new JetTypeImpl(annotations, tuple.getTypeConstructor(), false, typeArguments, tuple.getMemberScope(typeArguments));
    }

    public static JetType getTupleType(List<JetType> arguments) {
        return getTupleType(Collections.<AnnotationDescriptor>emptyList(), arguments);
    }

    public static JetType getTupleType(JetType... arguments) {
        return getTupleType(Collections.<AnnotationDescriptor>emptyList(), Arrays.asList(arguments));
    }
    
    public static boolean isTupleType(@NotNull JetType type) {
        return TUPLE_CONSTRUCTORS.contains(type.getConstructor());
    }

    public static List<JetType> getTupleElementTypes(@NotNull JetType type) {
        assert isTupleType(type);
        List<JetType> result = Lists.newArrayList();
        for (TypeProjection typeProjection : type.getArguments()) {
            result.add(typeProjection.getType());
        }
        return result;
    }

    public static JetType getLabeledTupleType(List<AnnotationDescriptor> annotations, List<ValueParameterDescriptor> arguments) {
        // TODO
        return getTupleType(annotations, toTypes(arguments));
    }

    public static JetType getLabeledTupleType(List<ValueParameterDescriptor> arguments) {
        // TODO
        return getLabeledTupleType(Collections.<AnnotationDescriptor>emptyList(), arguments);
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
            result.add(entry.getType());
        }
        return result;
    }

    // TODO : labeled version?
    public static JetType getFunctionType(List<AnnotationDescriptor> annotations, @Nullable JetType receiverType, @NotNull List<JetType> parameterTypes, @NotNull JetType returnType) {
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

    public static boolean isFunctionType(@NotNull JetType type) {
        return FUNCTION_TYPE_CONSTRUCTORS.contains(type.getConstructor()) || isReceiverFunctionType(type);
    }

    public static boolean isReceiverFunctionType(@NotNull JetType type) {
        return RECEIVER_FUNCTION_TYPE_CONSTRUCTORS.contains(type.getConstructor());
    }

    @Nullable
    public static JetType getReceiverType(@NotNull JetType type) {
        assert isFunctionType(type) : type;
        if (RECEIVER_FUNCTION_TYPE_CONSTRUCTORS.contains(type.getConstructor())) {
            return type.getArguments().get(0).getType();
        }
        return null;
    }

    @NotNull
    public static List<ValueParameterDescriptor> getValueParameters(@NotNull FunctionDescriptor functionDescriptor, @NotNull JetType type) {
        assert isFunctionType(type);
        int receiverOffset = getReceiverType(type) != null ? 1 : 0;
        List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();
        List<TypeProjection> parameterTypes = getParameterTypeProjectionsFromFunctionType(type);
        for (int i = 0; i < parameterTypes.size(); i++) {
            TypeProjection parameterType = parameterTypes.get(i);
            ValueParameterDescriptorImpl valueParameterDescriptor = new ValueParameterDescriptorImpl(
                    functionDescriptor, i, Collections.<AnnotationDescriptor>emptyList(),
                    "p" + (i + receiverOffset), false, parameterType.getType(), false, null);
            valueParameters.add(valueParameterDescriptor);
        }
        return valueParameters;
    }

    @NotNull
    public static List<TypeProjection> getParameterTypeProjectionsFromFunctionType(@NotNull JetType type) {
        assert isFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        int first = RECEIVER_FUNCTION_TYPE_CONSTRUCTORS.contains(type.getConstructor()) ? 1 : 0;
        int last = arguments.size() - 2;
        List<TypeProjection> parameterTypes = Lists.newArrayList();
        for (int i = first; i <= last; i++) {
            parameterTypes.add(arguments.get(i));
        }
        return parameterTypes;
    }

    @NotNull
    public static JetType getReturnTypeFromFunctionType(@NotNull JetType type) {
        assert isFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        return arguments.get(arguments.size() - 1).getType();
    }

    @NotNull
    public static Collection<DeclarationDescriptor> getAllStandardClasses() {
        return STANDARD_CLASSES.getAllDescriptors();
    }
}
