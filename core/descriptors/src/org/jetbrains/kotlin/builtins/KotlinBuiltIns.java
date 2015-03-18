/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.builtins;

import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeCapabilitiesDeserializer;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.io.InputStream;
import java.util.*;

import static org.jetbrains.kotlin.builtins.PrimitiveType.*;

public class KotlinBuiltIns {
    public static final Name BUILT_INS_PACKAGE_NAME = Name.identifier("kotlin");
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

    private final Map<PrimitiveType, JetType> primitiveTypeToNullableJetType;
    private final Map<PrimitiveType, JetType> primitiveTypeToArrayJetType;
    private final Map<JetType, JetType> primitiveJetTypeToJetArrayType;
    private final Map<JetType, JetType> jetArrayTypeToPrimitiveJetType;

    public static final FqNames FQ_NAMES = new FqNames();

    private KotlinBuiltIns() {
        builtInsModule = new ModuleDescriptorImpl(
                Name.special("<built-ins lazy module>"), Collections.<ImportPath>emptyList(), PlatformToKotlinClassMap.EMPTY
        );
        builtinsPackageFragment = new BuiltinsPackageFragment(
                BUILT_INS_PACKAGE_FQ_NAME, new LockBasedStorageManager(), builtInsModule,
                FlexibleTypeCapabilitiesDeserializer.ThrowException.INSTANCE$,
                new Function1<String, InputStream>() {
                    @Override
                    public InputStream invoke(String path) {
                        return KotlinBuiltIns.class.getClassLoader().getResourceAsStream(path);
                    }
                }
        );
        builtInsModule.initialize(builtinsPackageFragment.getProvider());
        builtInsModule.addDependencyOnModule(builtInsModule);
        builtInsModule.seal();

        primitiveTypeToNullableJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
        primitiveTypeToArrayJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
        primitiveJetTypeToJetArrayType = new HashMap<JetType, JetType>();
        jetArrayTypeToPrimitiveJetType = new HashMap<JetType, JetType>();
    }

    private void doInitialize() {
        for (PrimitiveType primitive : PrimitiveType.values()) {
            makePrimitive(primitive);
        }
    }

    private void makePrimitive(@NotNull PrimitiveType primitiveType) {
        JetType type = getBuiltInTypeByClassName(primitiveType.getTypeName().asString());
        JetType arrayType = getBuiltInTypeByClassName(primitiveType.getArrayTypeName().asString());

        primitiveTypeToNullableJetType.put(primitiveType, TypeUtils.makeNullable(type));
        primitiveTypeToArrayJetType.put(primitiveType, arrayType);
        primitiveJetTypeToJetArrayType.put(type, arrayType);
        jetArrayTypeToPrimitiveJetType.put(arrayType, type);
    }

    public static class FqNames {
        public final FqNameUnsafe any = fqNameUnsafe("Any");
        public final FqNameUnsafe nothing = fqNameUnsafe("Nothing");
        public final FqNameUnsafe cloneable = fqNameUnsafe("Cloneable");
        public final FqNameUnsafe suppress = fqNameUnsafe("suppress");
        public final FqNameUnsafe unit = fqNameUnsafe("Unit");
        public final FqNameUnsafe string = fqNameUnsafe("String");
        public final FqNameUnsafe array = fqNameUnsafe("Array");

        public final FqName data = fqName("data");
        public final FqName deprecated = fqName("deprecated");
        public final FqName tailRecursive = fqName("tailRecursive");
        public final FqName noinline = fqName("noinline");

        public final Set<FqNameUnsafe> primitiveTypes;
        public final Set<FqNameUnsafe> primitiveArrays;
        {
            primitiveTypes = new HashSet<FqNameUnsafe>(0);
            primitiveArrays = new HashSet<FqNameUnsafe>(0);
            for (PrimitiveType primitiveType : PrimitiveType.values()) {
                primitiveTypes.add(fqNameUnsafe(primitiveType.getTypeName().asString()));
                primitiveArrays.add(fqNameUnsafe(primitiveType.getArrayTypeName().asString()));
            }
        }

        public final Set<FqNameUnsafe> functionClasses = computeIndexedFqNames("Function", FUNCTION_TRAIT_COUNT);
        public final Set<FqNameUnsafe> extensionFunctionClasses = computeIndexedFqNames("ExtensionFunction", FUNCTION_TRAIT_COUNT);

        @NotNull
        private static FqNameUnsafe fqNameUnsafe(@NotNull String simpleName) {
            return fqName(simpleName).toUnsafe();
        }

        private static FqName fqName(String simpleName) {
            return BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier(simpleName));
        }

        @NotNull
        private static Set<FqNameUnsafe> computeIndexedFqNames(@NotNull String prefix, int count) {
            Set<FqNameUnsafe> result = new HashSet<FqNameUnsafe>();
            for (int i = 0; i < count; i++) {
                result.add(fqNameUnsafe(prefix + i));
            }
            return result;
        }
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
        return KotlinPackage.<DeclarationDescriptor>setOf(
                getBuiltInClassByName("ByteRange"),
                getBuiltInClassByName("ShortRange"),
                getBuiltInClassByName("CharRange"),
                getBuiltInClassByName("IntRange")
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
    public ClassDescriptor getThrowable() {
        return getBuiltInClassByName("Throwable");
    }

    @NotNull
    public ClassDescriptor getCloneable() {
        return getBuiltInClassByName("Cloneable");
    }

    @NotNull
    public ClassDescriptor getDataClassAnnotation() {
        return getBuiltInClassByName("data");
    }

    @NotNull
    public static FqName getNoinlineClassAnnotationFqName() {
        return FQ_NAMES.noinline;
    }

    @NotNull
    public ClassDescriptor getInlineClassAnnotation() {
        return getBuiltInClassByName("inline");
    }

    @NotNull
    public ClassDescriptor getInlineOptionsClassAnnotation() {
        return getBuiltInClassByName("inlineOptions");
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
        ClassDescriptor classDescriptor = DescriptorUtils.getInnerClassByName(getMap(), "Entry");
        assert classDescriptor != null : "Can't find Map.Entry";
        return classDescriptor;
    }

    @NotNull
    public ClassDescriptor getMutableMapEntry() {
        ClassDescriptor classDescriptor = DescriptorUtils.getInnerClassByName(getMutableMap(), "MutableEntry");
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // GET TYPE

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    private JetType getBuiltInTypeByClassName(@NotNull String classSimpleName) {
        return getBuiltInClassByName(classSimpleName).getDefaultType();
    }

    // Special

    @NotNull
    public JetType getNothingType() {
        return getNothing().getDefaultType();
    }

    @NotNull
    public JetType getNullableNothingType() {
        return TypeUtils.makeNullable(getNothingType());
    }

    @NotNull
    public JetType getAnyType() {
        return getAny().getDefaultType();
    }

    @NotNull
    public JetType getNullableAnyType() {
        return TypeUtils.makeNullable(getAnyType());
    }

    // Primitive

    @NotNull
    public JetType getPrimitiveJetType(@NotNull PrimitiveType type) {
        return getPrimitiveClassDescriptor(type).getDefaultType();
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
        return getUnit().getDefaultType();
    }

    @NotNull
    public JetType getStringType() {
        return getString().getDefaultType();
    }

    @NotNull
    public JetType getArrayElementType(@NotNull JetType arrayType) {
        if (isArray(arrayType)) {
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
                Annotations.EMPTY,
                getArray().getTypeConstructor(),
                false,
                types,
                getArray().getMemberScope(types)
        );
    }

    @NotNull
    public JetType getEnumType(@NotNull JetType argument) {
        Variance projectionType = Variance.INVARIANT;
        List<TypeProjectionImpl> types = Collections.singletonList(new TypeProjectionImpl(projectionType, argument));
        return new JetTypeImpl(
                Annotations.EMPTY,
                getEnum().getTypeConstructor(),
                false,
                types,
                getEnum().getMemberScope(types)
        );
    }

    @NotNull
    public JetType getAnnotationType() {
        return getAnnotation().getDefaultType();
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
            @NotNull Annotations annotations,
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
    public static List<TypeProjection> getFunctionTypeArgumentProjections(
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

    public static boolean isArray(@NotNull JetType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.array);
    }

    public static boolean isPrimitiveArray(@NotNull JetType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor != null && FQ_NAMES.primitiveArrays.contains(DescriptorUtils.getFqName(descriptor));
    }

    public static boolean isPrimitiveType(@NotNull JetType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return !type.isMarkedNullable() && descriptor != null && FQ_NAMES.primitiveTypes.contains(DescriptorUtils.getFqName(descriptor));
    }

    // Functions

    public static boolean isFunctionOrExtensionFunctionType(@NotNull JetType type) {
        return isFunctionType(type) || isExtensionFunctionType(type);
    }

    public static boolean isFunctionType(@NotNull JetType type) {
        if (isExactFunctionType(type)) return true;

        for (JetType superType : type.getConstructor().getSupertypes()) {
            if (isFunctionType(superType)) return true;
        }

        return false;
    }

    public static boolean isExtensionFunctionType(@NotNull JetType type) {
        if (isExactExtensionFunctionType(type)) return true;

        for (JetType superType : type.getConstructor().getSupertypes()) {
            if (isExtensionFunctionType(superType)) return true;
        }

        return false;
    }

    public static boolean isExactFunctionOrExtensionFunctionType(@NotNull JetType type) {
        return isExactFunctionType(type) || isExactExtensionFunctionType(type);
    }

    public static boolean isExactFunctionType(@NotNull JetType type) {
        return isTypeConstructorFqNameInSet(type, FQ_NAMES.functionClasses);
    }

    public static boolean isExactExtensionFunctionType(@NotNull JetType type) {
        return isTypeConstructorFqNameInSet(type, FQ_NAMES.extensionFunctionClasses);
    }

    public static boolean isExactFunctionType(@NotNull FqNameUnsafe fqName) {
        return FQ_NAMES.functionClasses.contains(fqName);
    }

    public static boolean isExactExtensionFunctionType(@NotNull FqNameUnsafe fqName) {
        return FQ_NAMES.extensionFunctionClasses.contains(fqName);
    }

    private static boolean isTypeConstructorFqNameInSet(@NotNull JetType type, @NotNull Set<FqNameUnsafe> classes) {
        ClassifierDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();

        if (declarationDescriptor == null) return false;

        FqNameUnsafe fqName = DescriptorUtils.getFqName(declarationDescriptor);
        return classes.contains(fqName);
    }

    @Nullable
    public static JetType getReceiverType(@NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type) : type;
        if (isExtensionFunctionType(type)) {
            return type.getArguments().get(0).getType();
        }
        return null;
    }

    @NotNull
    public static List<ValueParameterDescriptor> getValueParameters(@NotNull FunctionDescriptor functionDescriptor, @NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> parameterTypes = getParameterTypeProjectionsFromFunctionType(type);
        List<ValueParameterDescriptor> valueParameters = new ArrayList<ValueParameterDescriptor>(parameterTypes.size());
        for (int i = 0; i < parameterTypes.size(); i++) {
            TypeProjection parameterType = parameterTypes.get(i);
            ValueParameterDescriptorImpl valueParameterDescriptor = new ValueParameterDescriptorImpl(
                    functionDescriptor, null, i, Annotations.EMPTY,
                    Name.identifier("p" + (i + 1)), parameterType.getType(), false, null, SourceElement.NO_SOURCE
            );
            valueParameters.add(valueParameterDescriptor);
        }
        return valueParameters;
    }

    @NotNull
    public static JetType getReturnTypeFromFunctionType(@NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        return arguments.get(arguments.size() - 1).getType();
    }

    @NotNull
    public static List<TypeProjection> getParameterTypeProjectionsFromFunctionType(@NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        int first = isExtensionFunctionType(type) ? 1 : 0;
        int last = arguments.size() - 2;
        List<TypeProjection> parameterTypes = new ArrayList<TypeProjection>(last - first + 1);
        for (int i = first; i <= last; i++) {
            parameterTypes.add(arguments.get(i));
        }
        return parameterTypes;
    }

    // Recognized & special

    private static boolean isConstructedFromGivenClass(@NotNull JetType type, @NotNull FqNameUnsafe fqName) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor != null && fqName.equals(DescriptorUtils.getFqName(descriptor));
    }

    private static boolean isNotNullConstructedFromGivenClass(@NotNull JetType type, @NotNull FqNameUnsafe fqName) {
        return !type.isMarkedNullable() && isConstructedFromGivenClass(type, fqName);
    }

    public static boolean isSpecialClassWithNoSupertypes(@NotNull ClassDescriptor descriptor) {
        FqNameUnsafe fqName = DescriptorUtils.getFqName(descriptor);
        return FQ_NAMES.any.equals(fqName) || FQ_NAMES.nothing.equals(fqName);
    }

    public static boolean isAny(@NotNull ClassDescriptor descriptor) {
        return isAny(DescriptorUtils.getFqName(descriptor));
    }

    public static boolean isAny(@NotNull FqNameUnsafe fqName) {
        return FQ_NAMES.any.equals(fqName);
    }

    public static boolean isNothing(@NotNull JetType type) {
        return isNothingOrNullableNothing(type)
               && !type.isMarkedNullable();
    }

    public static boolean isNullableNothing(@NotNull JetType type) {
        return isNothingOrNullableNothing(type)
               && type.isMarkedNullable();
    }

    public static boolean isNothingOrNullableNothing(@NotNull JetType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.nothing);
    }

    public static boolean isAnyOrNullableAny(@NotNull JetType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.any);
    }

    public static boolean isNullableAny(@NotNull JetType type) {
        return isAnyOrNullableAny(type) && type.isMarkedNullable();
    }

    public static boolean isUnit(@NotNull JetType type) {
        return isNotNullConstructedFromGivenClass(type, FQ_NAMES.unit);
    }

    public boolean isBooleanOrSubtype(@NotNull JetType type) {
        return JetTypeChecker.DEFAULT.isSubtypeOf(type, getBooleanType());
    }

    public static boolean isString(@Nullable JetType type) {
        return type != null && isNotNullConstructedFromGivenClass(type, FQ_NAMES.string);
    }

    public static boolean isCloneable(@NotNull ClassDescriptor descriptor) {
        return FQ_NAMES.cloneable.equals(DescriptorUtils.getFqName(descriptor));
    }

    public static boolean isData(@NotNull ClassDescriptor classDescriptor) {
        return containsAnnotation(classDescriptor, FQ_NAMES.data);
    }

    public static boolean isDeprecated(@NotNull DeclarationDescriptor declarationDescriptor) {
        return containsAnnotation(declarationDescriptor, FQ_NAMES.deprecated);
    }

    public static boolean isTailRecursive(@NotNull DeclarationDescriptor declarationDescriptor) {
        return containsAnnotation(declarationDescriptor, FQ_NAMES.tailRecursive);
    }

    public static boolean isSuppressAnnotation(@NotNull AnnotationDescriptor annotationDescriptor) {
        return isConstructedFromGivenClass(annotationDescriptor.getType(), FQ_NAMES.suppress);
    }

    static boolean containsAnnotation(DeclarationDescriptor descriptor, FqName annotationClassFqName) {
        return descriptor.getOriginal().getAnnotations().findAnnotation(annotationClassFqName) != null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public JetType getDefaultBound() {
        return getNullableAnyType();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // GET FUNCTION

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public FunctionDescriptor getIdentityEquals() {
        return KotlinPackage.first(getBuiltInsPackageFragment().getMemberScope().getFunctions(Name.identifier("identityEquals")));
    }
}
