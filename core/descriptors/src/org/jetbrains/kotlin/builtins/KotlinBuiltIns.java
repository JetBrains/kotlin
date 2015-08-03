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

import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.*;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.UsageLocation;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.io.InputStream;
import java.util.*;

import static kotlin.KotlinPackage.*;
import static org.jetbrains.kotlin.builtins.PrimitiveType.*;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName;

public class KotlinBuiltIns {
    public static final Name BUILT_INS_PACKAGE_NAME = Name.identifier("kotlin");
    public static final FqName BUILT_INS_PACKAGE_FQ_NAME = FqName.topLevel(BUILT_INS_PACKAGE_NAME);
    public static final FqName ANNOTATION_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("annotation"));

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static volatile KotlinBuiltIns instance = null;

    private static volatile boolean initializing;
    private static Throwable initializationFailed;

    private static synchronized void initialize() {
        if (instance == null) {
            if (initializationFailed != null) {
                throw new IllegalStateException(
                        "Built-in library initialization failed previously: " + initializationFailed, initializationFailed
                );
            }
            if (initializing) {
                throw new IllegalStateException("Built-in library initialization loop");
            }
            initializing = true;
            try {
                instance = new KotlinBuiltIns();
                instance.doInitialize();
            }
            catch (Throwable e) {
                initializationFailed = e;
                throw new IllegalStateException("Built-in library initialization failed. " +
                                                "Please ensure you have kotlin-runtime.jar in the classpath: " + e, e);
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
    private final BuiltinsPackageFragment annotationPackageFragment;

    private final Map<PrimitiveType, JetType> primitiveTypeToArrayJetType;
    private final Map<JetType, JetType> primitiveJetTypeToJetArrayType;
    private final Map<JetType, JetType> jetArrayTypeToPrimitiveJetType;

    public static final FqNames FQ_NAMES = new FqNames();

    private KotlinBuiltIns() {
        LockBasedStorageManager storageManager = new LockBasedStorageManager();
        builtInsModule = new ModuleDescriptorImpl(
                Name.special("<built-ins module>"), storageManager, ModuleParameters.Empty.INSTANCE$, this
        );

        PackageFragmentProvider packageFragmentProvider = BuiltinsPackage.createBuiltInPackageFragmentProvider(
                storageManager, builtInsModule,
                setOf(BUILT_INS_PACKAGE_FQ_NAME, ANNOTATION_PACKAGE_FQ_NAME, BuiltinsPackage.getKOTLIN_REFLECT_FQ_NAME()),
                new BuiltInFictitiousFunctionClassFactory(storageManager, builtInsModule),
                new Function1<String, InputStream>() {
                    @Override
                    public InputStream invoke(String path) {
                        return KotlinBuiltIns.class.getClassLoader().getResourceAsStream(path);
                    }
                }
        );

        builtInsModule.initialize(packageFragmentProvider);
        builtInsModule.setDependencies(builtInsModule);

        builtinsPackageFragment = (BuiltinsPackageFragment) single(packageFragmentProvider.getPackageFragments(BUILT_INS_PACKAGE_FQ_NAME));
        annotationPackageFragment = (BuiltinsPackageFragment) single(packageFragmentProvider.getPackageFragments(ANNOTATION_PACKAGE_FQ_NAME));

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

        public final FqNameUnsafe _boolean = fqNameUnsafe("Boolean");
        public final FqNameUnsafe _char = fqNameUnsafe("Char");
        public final FqNameUnsafe _byte = fqNameUnsafe("Byte");
        public final FqNameUnsafe _short = fqNameUnsafe("Short");
        public final FqNameUnsafe _int = fqNameUnsafe("Int");
        public final FqNameUnsafe _long = fqNameUnsafe("Long");
        public final FqNameUnsafe _float = fqNameUnsafe("Float");
        public final FqNameUnsafe _double = fqNameUnsafe("Double");

        public final FqName data = fqName("data");
        public final FqName deprecated = fqName("deprecated");
        public final FqName tailRecursive = fqName("tailRecursive");
        public final FqName inline = fqName("inline");
        public final FqName noinline = fqName("noinline");
        public final FqName inlineOptions = fqName("inlineOptions");
        public final FqName extension = fqName("extension");
        public final FqName target = annotationName("target");
        public final FqName annotation = annotationName("annotation");
        public final FqName annotationTarget = annotationName("AnnotationTarget");
        public final FqName annotationRetention = annotationName("AnnotationRetention");

        public final FqName mutableList = fqName("MutableList");
        public final FqName mutableSet = fqName("MutableSet");
        public final FqName mutableMap = fqName("MutableMap");

        public final FqNameUnsafe kClass = new FqName("kotlin.reflect.KClass").toUnsafe();

        public final Map<FqNameUnsafe, PrimitiveType> fqNameToPrimitiveType;
        public final Map<FqNameUnsafe, PrimitiveType> arrayClassFqNameToPrimitiveType;
        {
            fqNameToPrimitiveType = new HashMap<FqNameUnsafe, PrimitiveType>(0);
            arrayClassFqNameToPrimitiveType = new HashMap<FqNameUnsafe, PrimitiveType>(0);
            for (PrimitiveType primitiveType : PrimitiveType.values()) {
                fqNameToPrimitiveType.put(fqNameUnsafe(primitiveType.getTypeName().asString()), primitiveType);
                arrayClassFqNameToPrimitiveType.put(fqNameUnsafe(primitiveType.getArrayTypeName().asString()), primitiveType);
            }
        }

        @NotNull
        private static FqNameUnsafe fqNameUnsafe(@NotNull String simpleName) {
            return fqName(simpleName).toUnsafe();
        }

        @NotNull
        private static FqName fqName(@NotNull String simpleName) {
            return BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier(simpleName));
        }

        @NotNull
        private static FqName annotationName(@NotNull String simpleName) {
            return ANNOTATION_PACKAGE_FQ_NAME.child(Name.identifier(simpleName));
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
    private ClassDescriptor getAnnotationClassByName(@NotNull Name simpleName) {
        ClassifierDescriptor classifier = annotationPackageFragment.getMemberScope().getClassifier(simpleName, UsageLocation.NO_LOCATION);
        assert classifier instanceof ClassDescriptor : "Must be a class descriptor " + simpleName + ", but was " +
                                                       (classifier == null ? "null" : classifier.toString());
        return (ClassDescriptor) classifier;
    }

    @NotNull
    public ClassDescriptor getBuiltInClassByName(@NotNull Name simpleName) {
        ClassDescriptor classDescriptor = getBuiltInClassByNameNullable(simpleName);
        assert classDescriptor != null : "Must be a class descriptor " + simpleName + ", but was null";
        return classDescriptor;
    }

    @Nullable
    public ClassDescriptor getBuiltInClassByNameNullable(@NotNull Name simpleName) {
        ClassifierDescriptor classifier = getBuiltInsPackageFragment().getMemberScope().getClassifier(simpleName, UsageLocation.NO_LOCATION);
        assert classifier == null ||
               classifier instanceof ClassDescriptor : "Must be a class descriptor " + simpleName + ", but was " + classifier;
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

    /**
     * @return the descriptor representing the class kotlin.Function{parameterCount + 1}
     * @deprecated there are no ExtensionFunction classes anymore, use {@link #getFunction(int)} instead
     */
    @Deprecated
    @NotNull
    public ClassDescriptor getExtensionFunction(int parameterCount) {
        return getBuiltInClassByName("Function" + (parameterCount + 1));
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
    public ClassDescriptor getDeprecatedAnnotation() {
        return getBuiltInClassByName("deprecated");
    }

    @NotNull
    public ClassDescriptor getTargetAnnotation() {
        return getAnnotationClassByName(FQ_NAMES.target.shortName());
    }

    @NotNull
    public ClassDescriptor getAnnotationTargetEnum() {
        return getAnnotationClassByName(FQ_NAMES.annotationTarget.shortName());
    }

    @Nullable
    public ClassDescriptor getAnnotationTargetEnumEntry(@NotNull KotlinTarget target) {
        ClassifierDescriptor result = getAnnotationTargetEnum().getUnsubstitutedInnerClassesScope().getClassifier(
                Name.identifier(target.name()), UsageLocation.NO_LOCATION
        );
        return result instanceof ClassDescriptor ? (ClassDescriptor) result : null;
    }

    @NotNull
    public ClassDescriptor getAnnotationRetentionEnum() {
        return getAnnotationClassByName(FQ_NAMES.annotationRetention.shortName());
    }

    @Nullable
    public ClassDescriptor getAnnotationRetentionEnumEntry(@NotNull KotlinRetention retention) {
        ClassifierDescriptor result = getAnnotationRetentionEnum().getUnsubstitutedInnerClassesScope().getClassifier(
                Name.identifier(retention.name()), UsageLocation.NO_LOCATION
        );
        return result instanceof ClassDescriptor ? (ClassDescriptor) result : null;
    }

    @NotNull
    public ClassDescriptor getAnnotationAnnotation() {
        return getAnnotationClassByName(FQ_NAMES.annotation.shortName());
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
     * @return {@code null} if not primitive
     */
    @Nullable
    public JetType getPrimitiveArrayJetTypeByPrimitiveJetType(@NotNull JetType jetType) {
        return primitiveJetTypeToJetArrayType.get(jetType);
    }

    @Nullable
    public static PrimitiveType getPrimitiveTypeByFqName(@NotNull FqNameUnsafe primitiveClassFqName) {
        return FQ_NAMES.fqNameToPrimitiveType.get(primitiveClassFqName);
    }

    @Nullable
    public static PrimitiveType getPrimitiveTypeByArrayClassFqName(@NotNull FqNameUnsafe primitiveArrayClassFqName) {
        return FQ_NAMES.arrayClassFqNameToPrimitiveType.get(primitiveArrayClassFqName);
    }

    @NotNull
    public JetType getArrayType(@NotNull Variance projectionType, @NotNull JetType argument) {
        List<TypeProjectionImpl> types = Collections.singletonList(new TypeProjectionImpl(projectionType, argument));
        return JetTypeImpl.create(
                Annotations.EMPTY,
                getArray(),
                false,
                types
        );
    }

    @NotNull
    public JetType getEnumType(@NotNull JetType argument) {
        Variance projectionType = Variance.INVARIANT;
        List<TypeProjectionImpl> types = Collections.singletonList(new TypeProjectionImpl(projectionType, argument));
        return JetTypeImpl.create(
                Annotations.EMPTY,
                getEnum(),
                false,
                types
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
    public AnnotationDescriptor createExtensionAnnotation() {
        return new AnnotationDescriptorImpl(getBuiltInClassByName("extension").getDefaultType(),
                                            Collections.<ValueParameterDescriptor, ConstantValue<?>>emptyMap(), SourceElement.NO_SOURCE);
    }

    private static boolean isTypeAnnotatedWithExtension(@NotNull JetType type) {
        return type.getAnnotations().findAnnotation(FQ_NAMES.extension) != null;
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

        Annotations typeAnnotations = receiverType == null ? annotations : addExtensionAnnotation(annotations);

        return JetTypeImpl.create(typeAnnotations, classDescriptor, false, arguments);
    }

    @NotNull
    private Annotations addExtensionAnnotation(@NotNull Annotations annotations) {
        if (annotations.findAnnotation(FQ_NAMES.extension) != null) return annotations;

        // TODO: preserve laziness of given annotations
        return new AnnotationsImpl(plus(annotations, listOf(createExtensionAnnotation())));
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
        return descriptor != null && getPrimitiveTypeByArrayClassFqName(getFqName(descriptor)) != null;
    }

    public static boolean isPrimitiveType(@NotNull JetType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return !type.isMarkedNullable() && descriptor instanceof ClassDescriptor && isPrimitiveClass((ClassDescriptor) descriptor);
    }

    public static boolean isPrimitiveClass(@NotNull ClassDescriptor descriptor) {
        return getPrimitiveTypeByFqName(getFqName(descriptor)) != null;
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
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor != null && isNumberedFunctionClassFqName(getFqName(descriptor));
    }

    public static boolean isExactFunctionType(@NotNull JetType type) {
        return isExactFunctionOrExtensionFunctionType(type) && !isTypeAnnotatedWithExtension(type);
    }

    public static boolean isExactExtensionFunctionType(@NotNull JetType type) {
        return isExactFunctionOrExtensionFunctionType(type) && isTypeAnnotatedWithExtension(type);
    }

    /**
     * @return true if this is an FQ name of a fictitious class representing the function type,
     * e.g. kotlin.Function1 (but NOT kotlin.reflect.KFunction1)
     */
    public static boolean isNumberedFunctionClassFqName(@NotNull FqNameUnsafe fqName) {
        List<Name> segments = fqName.pathSegments();
        if (segments.size() != 2) return false;

        if (!BUILT_INS_PACKAGE_NAME.equals(first(segments))) return false;

        String shortName = last(segments).asString();
        return BuiltInFictitiousFunctionClassFactory.parseClassName(shortName, BUILT_INS_PACKAGE_FQ_NAME) != null;
    }

    @Nullable
    public static JetType getReceiverType(@NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type) : type;
        if (isExtensionFunctionType(type)) {
            // TODO: this is incorrect when a class extends from an extension function and swaps type arguments
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
        return descriptor != null &&
               /* quick check to avoid creation of full FqName instance */ descriptor.getName().equals(fqName.shortName()) &&
               fqName.equals(getFqName(descriptor));
    }

    private static boolean isNotNullConstructedFromGivenClass(@NotNull JetType type, @NotNull FqNameUnsafe fqName) {
        return !type.isMarkedNullable() && isConstructedFromGivenClass(type, fqName);
    }

    public static boolean isSpecialClassWithNoSupertypes(@NotNull ClassDescriptor descriptor) {
        FqNameUnsafe fqName = getFqName(descriptor);
        return FQ_NAMES.any.equals(fqName) || FQ_NAMES.nothing.equals(fqName);
    }

    public static boolean isAny(@NotNull ClassDescriptor descriptor) {
        return isAny(getFqName(descriptor));
    }

    public static boolean isBoolean(@NotNull JetType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._boolean);
    }

    public static boolean isBoolean(@NotNull ClassDescriptor classDescriptor) {
        return FQ_NAMES._boolean.equals(getFqName(classDescriptor));
    }

    public static boolean isChar(@NotNull JetType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._char);
    }

    public static boolean isInt(@NotNull JetType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._int);
    }

    public static boolean isByte(@NotNull JetType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._byte);
    }

    public static boolean isLong(@NotNull JetType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._long);
    }

    public static boolean isShort(@NotNull JetType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._short);
    }

    public static boolean isFloat(@NotNull JetType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._float);
    }

    public static boolean isDouble(@NotNull JetType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._double);
    }

    private static boolean isConstructedFromGivenClassAndNotNullable(@NotNull JetType type, @NotNull FqNameUnsafe fqName) {
        return isConstructedFromGivenClass(type, fqName) && !type.isMarkedNullable();
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

    public static boolean isDefaultBound(@NotNull JetType type) {
        return isNullableAny(type);
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

    public static boolean isKClass(@NotNull ClassDescriptor descriptor) {
        return FQ_NAMES.kClass.equals(getFqName(descriptor));
    }

    public static boolean isNonPrimitiveArray(@NotNull ClassDescriptor descriptor) {
        return FQ_NAMES.array.equals(getFqName(descriptor));
    }

    public static boolean isAnnotation(@NotNull ClassDescriptor descriptor) {
        return DescriptorUtils.getFqName(descriptor) == FQ_NAMES.annotation.toUnsafe()
               || containsAnnotation(descriptor, FQ_NAMES.annotation);
    }

    public static boolean isCloneable(@NotNull ClassDescriptor descriptor) {
        return FQ_NAMES.cloneable.equals(getFqName(descriptor));
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

    /** Checks that the symbol represented by the descriptor is annotated with the {@code kotlin.noinline} annotation */
    public static boolean isNoinline(@NotNull DeclarationDescriptor descriptor) {
        return containsAnnotation(descriptor, FQ_NAMES.noinline);
    }

    public static boolean isSuppressAnnotation(@NotNull AnnotationDescriptor annotationDescriptor) {
        return isConstructedFromGivenClass(annotationDescriptor.getType(), FQ_NAMES.suppress);
    }

    private static boolean containsAnnotation(DeclarationDescriptor descriptor, FqName annotationClassFqName) {
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
        return KotlinPackage.first(getBuiltInsPackageFragment().getMemberScope().getFunctions(Name.identifier("identityEquals"), UsageLocation.NO_LOCATION));
    }
}
