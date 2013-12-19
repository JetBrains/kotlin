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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.storage.LockBasedStorageManager;

import java.util.*;

import static org.jetbrains.jet.lang.types.lang.PrimitiveType.*;

public class KotlinBuiltIns {
    public static final JetScope STUB = JetScope.EMPTY;

    public static final String BUILT_INS_PACKAGE_NAME_STRING = "jet";
    public static final Name BUILT_INS_PACKAGE_NAME = Name.identifier(BUILT_INS_PACKAGE_NAME_STRING);
    public static final FqName BUILT_INS_PACKAGE_FQ_NAME = FqName.topLevel(BUILT_INS_PACKAGE_NAME);

    public static final int FUNCTION_TRAIT_COUNT = 23;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static volatile KotlinBuiltIns instance = null;

    private static volatile boolean initializing;
    private static Throwable initializationFailed;

    private static synchronized void initialize() {
        if (instance == null) {
            if (initializationFailed != null) {
                throw new RuntimeException(
                        "builtin library initialization failed previously: " + initializationFailed, initializationFailed);
            }
            if (initializing) {
                throw new IllegalStateException("builtin library initialization loop");
            }
            initializing = true;
            try {
                instance = new KotlinBuiltIns();
                instance.doInitialize();
            }
            catch (Throwable e) {
                initializationFailed = e;
                throw new RuntimeException("builtin library initialization failed: " + e, e);
            }
            finally {
                initializing = false;
            }
        }
    }

    @NotNull
    public static KotlinBuiltIns getInstance() {
        if (initializing) {
            synchronized (KotlinBuiltIns.class) {
                assert instance != null : "Built-ins are not initialized (note: We are under the same lock as initializing and instance)";
                return instance;
            }
        }
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ModuleDescriptorImpl builtInsModule;
    private final BuiltinsPackageFragment builtinsPackageFragment;

    private volatile ImmutableSet<ClassDescriptor> nonPhysicalClasses;

    private final ImmutableSet<ClassDescriptor> functionClassesSet;

    private final ImmutableSet<ClassDescriptor> extensionFunctionClassesSet;

    private final EnumMap<PrimitiveType, ClassDescriptor> primitiveTypeToClass;
    private final EnumMap<PrimitiveType, ClassDescriptor> primitiveTypeToArrayClass;
    private final EnumMap<PrimitiveType, JetType> primitiveTypeToJetType;
    private final EnumMap<PrimitiveType, JetType> primitiveTypeToNullableJetType;
    private final EnumMap<PrimitiveType, JetType> primitiveTypeToArrayJetType;
    private final Map<JetType, JetType> primitiveJetTypeToJetArrayType;
    private final Map<JetType, JetType> jetArrayTypeToPrimitiveJetType;

    private final ClassDescriptor nothingClass;
    private final ClassDescriptor arrayClass;
    private final ClassDescriptor deprecatedAnnotationClass;
    private final ClassDescriptor dataAnnotationClass;
    private final ClassDescriptor[] functionClasses;

    private volatile JetType anyType;
    private volatile JetType nullableAnyType;
    private volatile JetType nothingType;
    private volatile JetType nullableNothingType;
    private volatile JetType unitType;
    private volatile JetType stringType;
    private volatile JetType annotationType;

    private KotlinBuiltIns() {
        this.builtInsModule = new ModuleDescriptorImpl(Name.special("<built-ins lazy module>"),
                                                       Collections.<ImportPath>emptyList(),
                                                       PlatformToKotlinClassMap.EMPTY);
        builtinsPackageFragment = new BuiltinsPackageFragment(new LockBasedStorageManager(), builtInsModule);
        builtInsModule.addFragmentProvider(builtinsPackageFragment.packageFragmentProvider);

        this.functionClassesSet = computeIndexedClasses("Function", FUNCTION_TRAIT_COUNT);
        this.extensionFunctionClassesSet = computeIndexedClasses("ExtensionFunction", FUNCTION_TRAIT_COUNT);

        this.primitiveTypeToClass = new EnumMap<PrimitiveType, ClassDescriptor>(PrimitiveType.class);
        this.primitiveTypeToJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
        this.primitiveTypeToNullableJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
        this.primitiveTypeToArrayClass = new EnumMap<PrimitiveType, ClassDescriptor>(PrimitiveType.class);
        this.primitiveTypeToArrayJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
        this.primitiveJetTypeToJetArrayType = new HashMap<JetType, JetType>();
        this.jetArrayTypeToPrimitiveJetType = new HashMap<JetType, JetType>();

        this.nothingClass = getBuiltInClassByName("Nothing");
        this.arrayClass = getBuiltInClassByName("Array");
        this.deprecatedAnnotationClass = getBuiltInClassByName("deprecated");
        this.dataAnnotationClass = getBuiltInClassByName("data");
        this.functionClasses = new ClassDescriptor[FUNCTION_TRAIT_COUNT];
        for (int i = 0; i < functionClasses.length; i++) {
            functionClasses[i] = getBuiltInClassByName("Function" + i);
        }
    }

    private void doInitialize() {
        anyType = getBuiltInTypeByClassName("Any");
        nullableAnyType = TypeUtils.makeNullable(anyType);
        nothingType = getBuiltInTypeByClassName("Nothing");
        nullableNothingType = TypeUtils.makeNullable(nothingType);
        unitType = getBuiltInTypeByClassName("Unit");
        stringType = getBuiltInTypeByClassName("String");
        annotationType = getBuiltInTypeByClassName("Annotation");

        for (PrimitiveType primitive : PrimitiveType.values()) {
            makePrimitive(primitive);
        }

        nonPhysicalClasses = computeNonPhysicalClasses();
    }

    private void makePrimitive(PrimitiveType primitiveType) {
        ClassDescriptor theClass = getBuiltInClassByName(primitiveType.getTypeName().asString());
        JetType type = new JetTypeImpl(theClass);
        ClassDescriptor arrayClass = getBuiltInClassByName(primitiveType.getArrayTypeName().asString());
        JetType arrayType = new JetTypeImpl(arrayClass);

        primitiveTypeToClass.put(primitiveType, theClass);
        primitiveTypeToJetType.put(primitiveType, type);
        primitiveTypeToNullableJetType.put(primitiveType, TypeUtils.makeNullable(type));
        primitiveTypeToArrayClass.put(primitiveType, arrayClass);
        primitiveTypeToArrayJetType.put(primitiveType, arrayType);
        primitiveJetTypeToJetArrayType.put(type, arrayType);
        jetArrayTypeToPrimitiveJetType.put(arrayType, type);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public ModuleDescriptorImpl getBuiltInsModule() {
        return builtInsModule;
    }

    @NotNull
    public PackageFragmentDescriptor getBuiltInsPackageFragment() {
        return builtinsPackageFragment;
    }

    @NotNull
    public JetScope getBuiltInsPackageScope() {
        return builtinsPackageFragment.getMemberScope();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // GET CLASS

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public ClassDescriptor getBuiltInClassByName(@NotNull Name simpleName) {
        ClassifierDescriptor classifier = getBuiltInsPackageFragment().getMemberScope().getClassifier(simpleName);
        assert classifier instanceof ClassDescriptor : "Must be a class descriptor " + simpleName + ", but was " + classifier;
        return (ClassDescriptor) classifier;
    }

    @NotNull
    private ClassDescriptor getBuiltInClassByName(@NotNull String simpleName) {
        return getBuiltInClassByName(Name.identifier(simpleName));
    }

    // Special

    @NotNull
    public ClassDescriptor getAny() {
        return getBuiltInClassByName("Any");
    }

    @NotNull
    public ClassDescriptor getNothing() {
        return getBuiltInClassByName("Nothing");
    }

    // Primitive

    @NotNull
    public ClassDescriptor getPrimitiveClassDescriptor(@NotNull PrimitiveType type) {
        return getBuiltInClassByName(type.getTypeName().asString());
    }

    @NotNull
    public ClassDescriptor getByte() {
        return getPrimitiveClassDescriptor(BYTE);
    }

    @NotNull
    public ClassDescriptor getShort() {
        return getPrimitiveClassDescriptor(SHORT);
    }

    @NotNull
    public ClassDescriptor getInt() {
        return getPrimitiveClassDescriptor(INT);
    }

    @NotNull
    public ClassDescriptor getLong() {
        return getPrimitiveClassDescriptor(LONG);
    }

    @NotNull
    public ClassDescriptor getFloat() {
        return getPrimitiveClassDescriptor(FLOAT);
    }

    @NotNull
    public ClassDescriptor getDouble() {
        return getPrimitiveClassDescriptor(DOUBLE);
    }

    @NotNull
    public ClassDescriptor getChar() {
        return getPrimitiveClassDescriptor(CHAR);
    }

    @NotNull
    public ClassDescriptor getBoolean() {
        return getPrimitiveClassDescriptor(BOOLEAN);
    }

    // Recognized

    @NotNull
    public Set<DeclarationDescriptor> getIntegralRanges() {
        return ImmutableSet.<DeclarationDescriptor>of(
                getBuiltInClassByName("ByteRange"),
                getBuiltInClassByName("ShortRange"),
                getBuiltInClassByName("CharRange"),
                getBuiltInClassByName("IntRange"),
                getBuiltInClassByName("LongRange")
        );
    }

    @NotNull
    public ClassDescriptor getArray() {
        return getBuiltInClassByName("Array");
    }

    @NotNull
    public ClassDescriptor getPrimitiveArrayClassDescriptor(@NotNull PrimitiveType type) {
        return getBuiltInClassByName(type.getArrayTypeName().asString());
    }

    @NotNull
    public ClassDescriptor getNumber() {
        return getBuiltInClassByName("Number");
    }

    @NotNull
    public ClassDescriptor getHashable() {
        return getBuiltInClassByName("Hashable");
    }

    @NotNull
    public ClassDescriptor getUnit() {
        return getBuiltInClassByName("Unit");
    }

    @NotNull
    public ClassDescriptor getFunction(int parameterCount) {
        return getBuiltInClassByName("Function" + parameterCount);
    }

    @NotNull
    public ClassDescriptor getExtensionFunction(int parameterCount) {
        return getBuiltInClassByName("ExtensionFunction" + parameterCount);
    }

    @NotNull
    public ClassDescriptor getKFunction(int parameterCount) {
        return getBuiltInClassByName("KFunction" + parameterCount);
    }

    @NotNull
    public ClassDescriptor getKMemberFunction(int parameterCount) {
        return getBuiltInClassByName("KMemberFunction" + parameterCount);
    }

    @NotNull
    public ClassDescriptor getKExtensionFunction(int parameterCount) {
        return getBuiltInClassByName("KExtensionFunction" + parameterCount);
    }

    @NotNull
    public ClassDescriptor getThrowable() {
        return getBuiltInClassByName("Throwable");
    }

    @NotNull
    public ClassDescriptor getDataClassAnnotation() {
        return getBuiltInClassByName("data");
    }

    public ClassDescriptor getNoinlineClassAnnotation() {
        return getBuiltInClassByName("noinline");
    }

    @NotNull
    public ClassDescriptor getInlineClassAnnotation() {
        return getBuiltInClassByName("inline");
    }

    @NotNull
    public ClassDescriptor getSuppressAnnotationClass() {
        return getBuiltInClassByName("suppress");
    }

    @NotNull
    public ClassDescriptor getVolatileAnnotationClass() {
        return getBuiltInClassByName("volatile");
    }

    @NotNull
    public ClassDescriptor getTailRecursiveAnnotationClass() {
        return getBuiltInClassByName("tailRecursive");
    }

    @NotNull
    public ClassDescriptor getDeprecatedAnnotation() {
        return getBuiltInClassByName("deprecated");
    }

    @NotNull
    public ClassDescriptor getString() {
        return getBuiltInClassByName("String");
    }

    @NotNull
    public ClassDescriptor getCharSequence() {
        return getBuiltInClassByName("CharSequence");
    }

    @NotNull
    public ClassDescriptor getComparable() {
        return getBuiltInClassByName("Comparable");
    }

    @NotNull
    public ClassDescriptor getEnum() {
        return getBuiltInClassByName("Enum");
    }

    @NotNull
    public ClassDescriptor getAnnotation() {
        return getBuiltInClassByName("Annotation");
    }

    @NotNull
    public ClassDescriptor getIterator() {
        return getBuiltInClassByName("Iterator");
    }

    @NotNull
    public ClassDescriptor getIterable() {
        return getBuiltInClassByName("Iterable");
    }

    @NotNull
    public ClassDescriptor getMutableIterable() {
        return getBuiltInClassByName("MutableIterable");
    }

    @NotNull
    public ClassDescriptor getMutableIterator() {
        return getBuiltInClassByName("MutableIterator");
    }

    @NotNull
    public ClassDescriptor getCollection() {
        return getBuiltInClassByName("Collection");
    }

    @NotNull
    public ClassDescriptor getMutableCollection() {
        return getBuiltInClassByName("MutableCollection");
    }

    @NotNull
    public ClassDescriptor getList() {
        return getBuiltInClassByName("List");
    }

    @NotNull
    public ClassDescriptor getMutableList() {
        return getBuiltInClassByName("MutableList");
    }

    @NotNull
    public ClassDescriptor getSet() {
        return getBuiltInClassByName("Set");
    }

    @NotNull
    public ClassDescriptor getMutableSet() {
        return getBuiltInClassByName("MutableSet");
    }

    @NotNull
    public ClassDescriptor getMap() {
        return getBuiltInClassByName("Map");
    }

    @NotNull
    public ClassDescriptor getMutableMap() {
        return getBuiltInClassByName("MutableMap");
    }

    @NotNull
    public ClassDescriptor getMapEntry() {
        ClassDescriptor classDescriptor = DescriptorUtils.getInnerClassByName(getBuiltInClassByName("Map"), "Entry");
        assert classDescriptor != null : "Can't find Map.Entry";
        return classDescriptor;
    }

    @NotNull
    public ClassDescriptor getMutableMapEntry() {
        ClassDescriptor classDescriptor = DescriptorUtils.getInnerClassByName(getBuiltInClassByName("MutableMap"), "MutableEntry");
        assert classDescriptor != null : "Can't find MutableMap.MutableEntry";
        return classDescriptor;
    }

    @NotNull
    public ClassDescriptor getListIterator() {
        return getBuiltInClassByName("ListIterator");
    }

    @NotNull
    public ClassDescriptor getMutableListIterator() {
        return getBuiltInClassByName("MutableListIterator");
    }

    /**
     * Classes that only exist for the Kotlin compiler: they are erased at runtime.
     * As a consequence they, for example, shouldn't be referred to by other languages
     * (e.g. Java).
     */
    @NotNull
    public Set<ClassDescriptor> getNonPhysicalClasses() {
        return nonPhysicalClasses;
    }

    @NotNull
    private ImmutableSet<ClassDescriptor> computeNonPhysicalClasses() {
        ImmutableSet.Builder<ClassDescriptor> nonPhysical = ImmutableSet.builder();
        nonPhysical.add(
                getAny(),
                getNothing(),

                getNumber(),
                getString(),
                getCharSequence(),
                getThrowable(),
                getBuiltInClassByName("Hashable"),

                getIterator(),
                getIterable(),
                getCollection(),
                getList(),
                getListIterator(),
                getSet(),
                getMap(),
                getMapEntry(),

                getMutableIterator(),
                getMutableIterable(),
                getMutableCollection(),
                getMutableList(),
                getMutableListIterator(),
                getMutableSet(),
                getMutableMap(),
                getMutableMapEntry(),

                getVolatileAnnotationClass(),
                getDataClassAnnotation(),
                getAnnotation(),
                getComparable(),
                getEnum(),
                getArray()
        );

        for (PrimitiveType primitiveType : values()) {
            nonPhysical.add(getPrimitiveClassDescriptor(primitiveType));
            nonPhysical.add(getPrimitiveArrayClassDescriptor(primitiveType));
        }

        return nonPhysical.build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // GET TYPE

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    private JetType getBuiltInTypeByClassName(@NotNull String classSimpleName) {
        // TODO
        return new JetTypeImpl(getBuiltInClassByName(classSimpleName));
    }

    // Special

    @NotNull
    public JetType getNothingType() {
        return getBuiltInTypeByClassName("Nothing");
    }

    @NotNull
    public JetType getNullableNothingType() {
        // TODO
        return TypeUtils.makeNullable(getNothingType());
    }

    @NotNull
    public JetType getAnyType() {
        return getBuiltInTypeByClassName("Any");
    }

    @NotNull
    public JetType getNullableAnyType() {
        // TODO
        return TypeUtils.makeNullable(getAnyType());
    }

    // Primitive

    @NotNull
    public JetType getPrimitiveJetType(@NotNull PrimitiveType type) {
        // TODO
        return new JetTypeImpl(getPrimitiveClassDescriptor(type));
    }

    @NotNull
    public JetType getNullablePrimitiveJetType(@NotNull PrimitiveType primitiveType) {
        return primitiveTypeToNullableJetType.get(primitiveType);
    }

    @NotNull
    public JetType getByteType() {
        return getPrimitiveJetType(BYTE);
    }

    @NotNull
    public JetType getShortType() {
        return getPrimitiveJetType(SHORT);
    }

    @NotNull
    public JetType getIntType() {
        return getPrimitiveJetType(INT);
    }

    @NotNull
    public JetType getLongType() {
        return getPrimitiveJetType(LONG);
    }

    @NotNull
    public JetType getFloatType() {
        return getPrimitiveJetType(FLOAT);
    }

    @NotNull
    public JetType getDoubleType() {
        return getPrimitiveJetType(DOUBLE);
    }

    @NotNull
    public JetType getCharType() {
        return getPrimitiveJetType(CHAR);
    }

    @NotNull
    public JetType getBooleanType() {
        return getPrimitiveJetType(BOOLEAN);
    }

    // Recognized

    @NotNull
    public JetType getUnitType() {
        return getBuiltInTypeByClassName("Unit");
    }

    @NotNull
    public JetType getStringType() {
        return getBuiltInTypeByClassName("String");
    }

    @NotNull
    public JetType getArrayElementType(@NotNull JetType arrayType) {
        if (arrayType.getConstructor().getDeclarationDescriptor() == getArray()) {
            if (arrayType.getArguments().size() != 1) {
                throw new IllegalStateException();
            }
            return arrayType.getArguments().get(0).getType();
        }
        JetType primitiveType = jetArrayTypeToPrimitiveJetType.get(TypeUtils.makeNotNullable(arrayType));
        if (primitiveType == null) {
            throw new IllegalStateException("not array: " + arrayType);
        }
        return primitiveType;
    }

    @NotNull
    public JetType getPrimitiveArrayJetType(@NotNull PrimitiveType primitiveType) {
        return primitiveTypeToArrayJetType.get(primitiveType);
    }

    /**
     * @return <code>null</code> if not primitive
     */
    @Nullable
    public JetType getPrimitiveArrayJetTypeByPrimitiveJetType(@NotNull JetType jetType) {
        return primitiveJetTypeToJetArrayType.get(jetType);
    }

    @NotNull
    public JetType getArrayType(@NotNull Variance projectionType, @NotNull JetType argument) {
        List<TypeProjectionImpl> types = Collections.singletonList(new TypeProjectionImpl(projectionType, argument));
        return new JetTypeImpl(
                Collections.<AnnotationDescriptor>emptyList(),
                getArray().getTypeConstructor(),
                false,
                types,
                getArray().getMemberScope(types)
        );
    }

    @NotNull
    public JetType getArrayType(@NotNull JetType argument) {
        return getArrayType(Variance.INVARIANT, argument);
    }

    @NotNull
    public JetType getEnumType(@NotNull JetType argument) {
        Variance projectionType = Variance.INVARIANT;
        List<TypeProjectionImpl> types = Collections.singletonList(new TypeProjectionImpl(projectionType, argument));
        return new JetTypeImpl(
                Collections.<AnnotationDescriptor>emptyList(),
                getEnum().getTypeConstructor(),
                false,
                types,
                getEnum().getMemberScope(types)
        );
    }

    @NotNull
    public JetType getAnnotationType() {
        return getBuiltInTypeByClassName("Annotation");
    }

    @NotNull
    public ClassDescriptor getPropertyMetadata() {
        return getBuiltInClassByName("PropertyMetadata");
    }

    @NotNull
    public ClassDescriptor getPropertyMetadataImpl() {
        return getBuiltInClassByName("PropertyMetadataImpl");
    }

    @NotNull
    public JetType getFunctionType(
            @NotNull List<AnnotationDescriptor> annotations,
            @Nullable JetType receiverType,
            @NotNull List<JetType> parameterTypes,
            @NotNull JetType returnType
    ) {
        List<TypeProjection> arguments = getFunctionTypeArgumentProjections(receiverType, parameterTypes, returnType);
        int size = parameterTypes.size();
        ClassDescriptor classDescriptor = receiverType == null ? getFunction(size) : getExtensionFunction(size);
        TypeConstructor constructor = classDescriptor.getTypeConstructor();

        return new JetTypeImpl(annotations, constructor, false, arguments, classDescriptor.getMemberScope(arguments));
    }

    @NotNull
    public JetType getKFunctionType(
            @NotNull List<AnnotationDescriptor> annotations,
            @Nullable JetType receiverType,
            @NotNull List<JetType> parameterTypes,
            @NotNull JetType returnType,
            boolean extensionFunction
    ) {
        List<TypeProjection> arguments = getFunctionTypeArgumentProjections(receiverType, parameterTypes, returnType);
        ClassDescriptor classDescriptor = getCorrespondingKFunctionClass(receiverType, extensionFunction, parameterTypes.size());

        return new JetTypeImpl(
                annotations,
                classDescriptor.getTypeConstructor(),
                false,
                arguments,
                classDescriptor.getMemberScope(arguments)
        );
    }

    @NotNull
    private ClassDescriptor getCorrespondingKFunctionClass(
            @Nullable JetType receiverType,
            boolean extensionFunction,
            int numberOfParameters
    ) {
        if (receiverType == null) {
            return getKFunction(numberOfParameters);
        }
        else if (extensionFunction) {
            return getKExtensionFunction(numberOfParameters);
        }
        else {
            return getKMemberFunction(numberOfParameters);
        }
    }

    @NotNull
    private static List<TypeProjection> getFunctionTypeArgumentProjections(
            @Nullable JetType receiverType,
            @NotNull List<JetType> parameterTypes,
            @NotNull JetType returnType
    ) {
        List<TypeProjection> arguments = new ArrayList<TypeProjection>();
        if (receiverType != null) {
            arguments.add(defaultProjection(receiverType));
        }
        for (JetType parameterType : parameterTypes) {
            arguments.add(defaultProjection(parameterType));
        }
        arguments.add(defaultProjection(returnType));
        return arguments;
    }

    private static TypeProjection defaultProjection(JetType returnType) {
        return new TypeProjectionImpl(Variance.INVARIANT, returnType);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // IS TYPE

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isArray(@NotNull JetType type) {
        return getArray().equals(type.getConstructor().getDeclarationDescriptor());
    }

    public boolean isPrimitiveArray(@NotNull JetType type) {
        return jetArrayTypeToPrimitiveJetType.containsKey(TypeUtils.makeNotNullable(type));
    }

    public boolean isPrimitiveType(@NotNull JetType type) {
        return primitiveJetTypeToJetArrayType.containsKey(type);
    }

    // Functions

    @NotNull
    private ImmutableSet<ClassDescriptor> computeIndexedClasses(@NotNull String prefix, int count) {
        ImmutableSet.Builder<ClassDescriptor> builder = ImmutableSet.builder();
        for (int i = 0; i < count; i++) {
            builder.add(getBuiltInClassByName(prefix + i));
        }
        return builder.build();
    }

    public boolean isFunctionOrExtensionFunctionType(@NotNull JetType type) {
        return isFunctionType(type) || isExtensionFunctionType(type);
    }

    public boolean isFunctionType(@NotNull JetType type) {
        if (setContainsClassOf(functionClassesSet, type)) return true;

        for (JetType superType : type.getConstructor().getSupertypes()) {
            if (isFunctionType(superType)) return true;
        }

        return false;
    }

    public boolean isExactFunctionOrExtensionFunctionType(@NotNull JetType type) {
        return setContainsClassOf(extensionFunctionClassesSet, type) || setContainsClassOf(functionClassesSet, type);
    }

    public boolean isExtensionFunctionType(@NotNull JetType type) {
        if (setContainsClassOf(extensionFunctionClassesSet, type)) return true;

        for (JetType superType : type.getConstructor().getSupertypes()) {
            if (isExtensionFunctionType(superType)) return true;
        }

        return false;
    }

    @Nullable
    public JetType getReceiverType(@NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type) : type;
        if (isExtensionFunctionType(type)) {
            return type.getArguments().get(0).getType();
        }
        return null;
    }

    @NotNull
    public List<ValueParameterDescriptor> getValueParameters(@NotNull FunctionDescriptor functionDescriptor, @NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type);
        List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();
        List<TypeProjection> parameterTypes = getParameterTypeProjectionsFromFunctionType(type);
        for (int i = 0; i < parameterTypes.size(); i++) {
            TypeProjection parameterType = parameterTypes.get(i);
            ValueParameterDescriptorImpl valueParameterDescriptor = new ValueParameterDescriptorImpl(
                    functionDescriptor, i, Collections.<AnnotationDescriptor>emptyList(),
                    Name.identifier("p" + (i + 1)), parameterType.getType(), false, null);
            valueParameters.add(valueParameterDescriptor);
        }
        return valueParameters;
    }

    @NotNull
    public JetType getReturnTypeFromFunctionType(@NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        return arguments.get(arguments.size() - 1).getType();
    }

    @NotNull
    public List<TypeProjection> getParameterTypeProjectionsFromFunctionType(@NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        int first = isExtensionFunctionType(type) ? 1 : 0;
        int last = arguments.size() - 2;
        List<TypeProjection> parameterTypes = Lists.newArrayList();
        for (int i = first; i <= last; i++) {
            parameterTypes.add(arguments.get(i));
        }
        return parameterTypes;
    }

    // Recognized & special

    public boolean isNothing(@NotNull JetType type) {
        return isNothingOrNullableNothing(type)
               && !type.isNullable();
    }

    public boolean isNullableNothing(@NotNull JetType type) {
        return isNothingOrNullableNothing(type)
               && type.isNullable();
    }

    public boolean isNothingOrNullableNothing(@NotNull JetType type) {
        return !(type instanceof NamespaceType)
               && type.getConstructor() == getNothing().getTypeConstructor();
    }

    public boolean isAnyOrNullableAny(@NotNull JetType type) {
        return !(type instanceof NamespaceType) &&
               type.getConstructor() == getAny().getTypeConstructor();
    }

    public boolean isUnit(@NotNull JetType type) {
        return !(type instanceof NamespaceType) && type.equals(getUnitType());
    }

    public boolean isData(@NotNull ClassDescriptor classDescriptor) {
        return containsAnnotation(classDescriptor, getDataClassAnnotation());
    }

    public boolean isDeprecated(@NotNull DeclarationDescriptor declarationDescriptor) {
        return containsAnnotation(declarationDescriptor, getDeprecatedAnnotation());
    }

    public boolean isTailRecursive(@NotNull DeclarationDescriptor declarationDescriptor) {
        return containsAnnotation(declarationDescriptor, getTailRecursiveAnnotationClass());
    }

    static boolean containsAnnotation(DeclarationDescriptor descriptor, ClassDescriptor annotationClass) {
        List<AnnotationDescriptor> annotations = descriptor.getOriginal().getAnnotations();
        if (annotations != null) {
            for (AnnotationDescriptor annotation : annotations) {
                if (annotationClass.equals(annotation.getType().getConstructor().getDeclarationDescriptor())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isVolatile(@NotNull PropertyDescriptor descriptor) {
        return containsAnnotation(descriptor, getVolatileAnnotationClass());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public JetType getDefaultBound() {
        return getNullableAnyType();
    }

    private static boolean setContainsClassOf(ImmutableSet<ClassDescriptor> set, JetType type) {
        //noinspection SuspiciousMethodCalls
        return set.contains(type.getConstructor().getDeclarationDescriptor());
    }
}
