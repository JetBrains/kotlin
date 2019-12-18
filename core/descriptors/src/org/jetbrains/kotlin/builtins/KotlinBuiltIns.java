/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory;
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider;
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory;
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.*;

import static kotlin.collections.SetsKt.setOf;
import static org.jetbrains.kotlin.builtins.PrimitiveType.*;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_RELEASE;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName;
import static org.jetbrains.kotlin.utils.CollectionsKt.newHashMapWithExpectedSize;
import static org.jetbrains.kotlin.utils.CollectionsKt.newHashSetWithExpectedSize;

public abstract class KotlinBuiltIns {
    public static final Name BUILT_INS_PACKAGE_NAME = Name.identifier("kotlin");
    public static final FqName BUILT_INS_PACKAGE_FQ_NAME = FqName.topLevel(BUILT_INS_PACKAGE_NAME);
    private static final FqName ANNOTATION_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("annotation"));
    public static final FqName COLLECTIONS_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("collections"));
    public static final FqName RANGES_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("ranges"));
    public static final FqName TEXT_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("text"));

    public static final Set<FqName> BUILT_INS_PACKAGE_FQ_NAMES = setOf(
            BUILT_INS_PACKAGE_FQ_NAME,
            COLLECTIONS_PACKAGE_FQ_NAME,
            RANGES_PACKAGE_FQ_NAME,
            ANNOTATION_PACKAGE_FQ_NAME,
            ReflectionTypesKt.getKOTLIN_REFLECT_FQ_NAME(),
            BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("internal")),
            COROUTINES_PACKAGE_FQ_NAME_RELEASE
    );

    private ModuleDescriptorImpl builtInsModule;

    private final NotNullLazyValue<Primitives> primitives;
    private final NotNullLazyValue<Collection<PackageViewDescriptor>> builtInPackagesImportedByDefault;

    private final MemoizedFunctionToNotNull<Name, ClassDescriptor> builtInClassesByName;

    private final StorageManager storageManager;

    public static final FqNames FQ_NAMES = new FqNames();
    public static final Name BUILTINS_MODULE_NAME = Name.special("<built-ins module>");

    protected KotlinBuiltIns(@NotNull StorageManager storageManager) {
        this.storageManager = storageManager;

        this.builtInPackagesImportedByDefault = storageManager.createLazyValue(new Function0<Collection<PackageViewDescriptor>>() {
            @Override
            public Collection<PackageViewDescriptor> invoke() {
                return Arrays.asList(
                    builtInsModule.getPackage(BUILT_INS_PACKAGE_FQ_NAME),
                    builtInsModule.getPackage(COLLECTIONS_PACKAGE_FQ_NAME),
                    builtInsModule.getPackage(RANGES_PACKAGE_FQ_NAME),
                    builtInsModule.getPackage(ANNOTATION_PACKAGE_FQ_NAME)
                );
            }
        });

        this.primitives = storageManager.createLazyValue(new Function0<Primitives>() {
            @Override
            public Primitives invoke() {
                Map<PrimitiveType, SimpleType> primitiveTypeToArrayKotlinType = new EnumMap<PrimitiveType, SimpleType>(PrimitiveType.class);
                Map<KotlinType, SimpleType> primitiveKotlinTypeToKotlinArrayType = new HashMap<KotlinType, SimpleType>();
                Map<SimpleType, SimpleType> kotlinArrayTypeToPrimitiveKotlinType = new HashMap<SimpleType, SimpleType>();
                for (PrimitiveType primitive : PrimitiveType.values()) {
                    SimpleType type = getBuiltInTypeByClassName(primitive.getTypeName().asString());
                    SimpleType arrayType = getBuiltInTypeByClassName(primitive.getArrayTypeName().asString());

                    primitiveTypeToArrayKotlinType.put(primitive, arrayType);
                    primitiveKotlinTypeToKotlinArrayType.put(type, arrayType);
                    kotlinArrayTypeToPrimitiveKotlinType.put(arrayType, type);
                }
                return new Primitives(
                        primitiveTypeToArrayKotlinType, primitiveKotlinTypeToKotlinArrayType, kotlinArrayTypeToPrimitiveKotlinType
                );
            }
        });

        this.builtInClassesByName = storageManager.createMemoizedFunction(new Function1<Name, ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke(Name name) {
                ClassifierDescriptor classifier = getBuiltInsPackageScope().getContributedClassifier(name, NoLookupLocation.FROM_BUILTINS);
                if (classifier == null) {
                    throw new AssertionError("Built-in class " + BUILT_INS_PACKAGE_FQ_NAME.child(name) + " is not found");
                }
                if (!(classifier instanceof ClassDescriptor)) {
                    throw new AssertionError("Must be a class descriptor " + name + ", but was " + classifier);
                }
                return (ClassDescriptor) classifier;
            }
        });
    }

    protected void createBuiltInsModule(boolean isFallback) {
        builtInsModule = new ModuleDescriptorImpl(BUILTINS_MODULE_NAME, storageManager, this, null);
        builtInsModule.initialize(BuiltInsLoader.Companion.getInstance().createPackageFragmentProvider(
                storageManager, builtInsModule,
                getClassDescriptorFactories(), getPlatformDependentDeclarationFilter(), getAdditionalClassPartsProvider(), isFallback
        ));
        builtInsModule.setDependencies(builtInsModule);
    }

    public void setBuiltInsModule(@NotNull final ModuleDescriptorImpl module) {
        storageManager.compute(new Function0<Void>() {
            @Override
            public Void invoke() {
                if (builtInsModule != null) {
                    throw new AssertionError(
                            "Built-ins module is already set: " + builtInsModule + " (attempting to reset to " + module + ")"
                    );
                }
                builtInsModule = module;
                return null;
            }
        });
    }

    @NotNull
    protected AdditionalClassPartsProvider getAdditionalClassPartsProvider() {
        return AdditionalClassPartsProvider.None.INSTANCE;
    }

    @NotNull
    protected PlatformDependentDeclarationFilter getPlatformDependentDeclarationFilter() {
        return PlatformDependentDeclarationFilter.NoPlatformDependent.INSTANCE;
    }

    @NotNull
    protected Iterable<ClassDescriptorFactory> getClassDescriptorFactories() {
        return Collections.<ClassDescriptorFactory>singletonList(new BuiltInFictitiousFunctionClassFactory(storageManager, builtInsModule));
    }

    @NotNull
    protected StorageManager getStorageManager() {
        return storageManager;
    }

    private static class Primitives {
        public final Map<PrimitiveType, SimpleType> primitiveTypeToArrayKotlinType;
        public final Map<KotlinType, SimpleType> primitiveKotlinTypeToKotlinArrayType;
        public final Map<SimpleType, SimpleType> kotlinArrayTypeToPrimitiveKotlinType;

        private Primitives(
                @NotNull Map<PrimitiveType, SimpleType> primitiveTypeToArrayKotlinType,
                @NotNull Map<KotlinType, SimpleType> primitiveKotlinTypeToKotlinArrayType,
                @NotNull Map<SimpleType, SimpleType> kotlinArrayTypeToPrimitiveKotlinType
        ) {
            this.primitiveTypeToArrayKotlinType = primitiveTypeToArrayKotlinType;
            this.primitiveKotlinTypeToKotlinArrayType = primitiveKotlinTypeToKotlinArrayType;
            this.kotlinArrayTypeToPrimitiveKotlinType = kotlinArrayTypeToPrimitiveKotlinType;
        }
    }

    public static class FqNames {
        public final FqNameUnsafe any = fqNameUnsafe("Any");
        public final FqNameUnsafe nothing = fqNameUnsafe("Nothing");
        public final FqNameUnsafe cloneable = fqNameUnsafe("Cloneable");
        public final FqName suppress = fqName("Suppress");
        public final FqNameUnsafe unit = fqNameUnsafe("Unit");
        public final FqNameUnsafe charSequence = fqNameUnsafe("CharSequence");
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
        public final FqNameUnsafe number = fqNameUnsafe("Number");

        public final FqNameUnsafe _enum = fqNameUnsafe("Enum");

        public final FqNameUnsafe functionSupertype = fqNameUnsafe("Function");

        public final FqName throwable = fqName("Throwable");
        public final FqName comparable = fqName("Comparable");

        public final FqNameUnsafe intRange = rangesFqName("IntRange");
        public final FqNameUnsafe longRange = rangesFqName("LongRange");

        public final FqName deprecated = fqName("Deprecated");
        public final FqName deprecationLevel = fqName("DeprecationLevel");
        public final FqName replaceWith = fqName("ReplaceWith");
        public final FqName extensionFunctionType = fqName("ExtensionFunctionType");
        public final FqName parameterName = fqName("ParameterName");
        public final FqName annotation = fqName("Annotation");
        public final FqName target = annotationName("Target");
        public final FqName annotationTarget = annotationName("AnnotationTarget");
        public final FqName annotationRetention = annotationName("AnnotationRetention");
        public final FqName retention = annotationName("Retention");
        public final FqName repeatable = annotationName("Repeatable");
        public final FqName mustBeDocumented = annotationName("MustBeDocumented");
        public final FqName unsafeVariance = fqName("UnsafeVariance");
        public final FqName publishedApi = fqName("PublishedApi");

        public final FqName iterator = collectionsFqName("Iterator");
        public final FqName iterable = collectionsFqName("Iterable");
        public final FqName collection = collectionsFqName("Collection");
        public final FqName list = collectionsFqName("List");
        public final FqName listIterator = collectionsFqName("ListIterator");
        public final FqName set = collectionsFqName("Set");
        public final FqName map = collectionsFqName("Map");
        public final FqName mapEntry = map.child(Name.identifier("Entry"));
        public final FqName mutableIterator = collectionsFqName("MutableIterator");
        public final FqName mutableIterable = collectionsFqName("MutableIterable");
        public final FqName mutableCollection = collectionsFqName("MutableCollection");
        public final FqName mutableList = collectionsFqName("MutableList");
        public final FqName mutableListIterator = collectionsFqName("MutableListIterator");
        public final FqName mutableSet = collectionsFqName("MutableSet");
        public final FqName mutableMap = collectionsFqName("MutableMap");
        public final FqName mutableMapEntry = mutableMap.child(Name.identifier("MutableEntry"));

        public final FqNameUnsafe kClass = reflect("KClass");
        public final FqNameUnsafe kCallable = reflect("KCallable");
        public final FqNameUnsafe kProperty0 = reflect("KProperty0");
        public final FqNameUnsafe kProperty1 = reflect("KProperty1");
        public final FqNameUnsafe kProperty2 = reflect("KProperty2");
        public final FqNameUnsafe kMutableProperty0 = reflect("KMutableProperty0");
        public final FqNameUnsafe kMutableProperty1 = reflect("KMutableProperty1");
        public final FqNameUnsafe kMutableProperty2 = reflect("KMutableProperty2");
        public final FqNameUnsafe kPropertyFqName = reflect("KProperty");
        public final FqNameUnsafe kMutablePropertyFqName = reflect("KMutableProperty");
        public final ClassId kProperty = ClassId.topLevel(kPropertyFqName.toSafe());
        public final FqNameUnsafe kDeclarationContainer = reflect("KDeclarationContainer");

        public final FqName uByteFqName = fqName("UByte");
        public final FqName uShortFqName = fqName("UShort");
        public final FqName uIntFqName = fqName("UInt");
        public final FqName uLongFqName = fqName("ULong");
        public final ClassId uByte = ClassId.topLevel(uByteFqName);
        public final ClassId uShort = ClassId.topLevel(uShortFqName);
        public final ClassId uInt = ClassId.topLevel(uIntFqName);
        public final ClassId uLong = ClassId.topLevel(uLongFqName);

        public final Set<Name> primitiveTypeShortNames = newHashSetWithExpectedSize(PrimitiveType.values().length);
        public final Set<Name> primitiveArrayTypeShortNames = newHashSetWithExpectedSize(PrimitiveType.values().length);
        public final Map<FqNameUnsafe, PrimitiveType> fqNameToPrimitiveType = newHashMapWithExpectedSize(PrimitiveType.values().length);
        public final Map<FqNameUnsafe, PrimitiveType> arrayClassFqNameToPrimitiveType = newHashMapWithExpectedSize(PrimitiveType.values().length);
        {
            for (PrimitiveType primitiveType : PrimitiveType.values()) {
                primitiveTypeShortNames.add(primitiveType.getTypeName());
                primitiveArrayTypeShortNames.add(primitiveType.getArrayTypeName());
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
        private static FqName collectionsFqName(@NotNull String simpleName) {
            return COLLECTIONS_PACKAGE_FQ_NAME.child(Name.identifier(simpleName));
        }

        @NotNull
        private static FqNameUnsafe rangesFqName(@NotNull String simpleName) {
            return RANGES_PACKAGE_FQ_NAME.child(Name.identifier(simpleName)).toUnsafe();
        }

        @NotNull
        private static FqNameUnsafe reflect(@NotNull String simpleName) {
            return ReflectionTypesKt.getKOTLIN_REFLECT_FQ_NAME().child(Name.identifier(simpleName)).toUnsafe();
        }

        @NotNull
        private static FqName annotationName(@NotNull String simpleName) {
            return ANNOTATION_PACKAGE_FQ_NAME.child(Name.identifier(simpleName));
        }
    }

    @NotNull
    public ModuleDescriptorImpl getBuiltInsModule() {
        return builtInsModule;
    }

    @NotNull
    public Collection<PackageViewDescriptor> getBuiltInPackagesImportedByDefault() {
        return builtInPackagesImportedByDefault.invoke();
    }

    /**
     * Checks if the given descriptor is declared in the deserialized built-in package fragment, i.e. if it was loaded as a part of
     * loading .kotlin_builtins definition files.
     *
     * NOTE: this method returns false for descriptors loaded from .class files or other binaries, even if they are "built-in" in some
     * other sense! For example, it returns false for the class descriptor of `kotlin.IntRange` loaded from `kotlin/IntRange.class`.
     * In case you need to check if the class is "built-in" in another sense, you should probably do it by inspecting its FQ name,
     * or the FQ name of its containing package.
     */
    public static boolean isBuiltIn(@NotNull DeclarationDescriptor descriptor) {
        return DescriptorUtils.getParentOfType(descriptor, BuiltInsPackageFragment.class, false) != null;
    }

    /**
     * @return true if the containing package of the descriptor is "kotlin" or any subpackage of "kotlin"
     */
    public static boolean isUnderKotlinPackage(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor current = descriptor;
        while (current != null) {
            if (current instanceof PackageFragmentDescriptor) {
                return ((PackageFragmentDescriptor) current).getFqName().startsWith(BUILT_INS_PACKAGE_NAME);
            }
            current = current.getContainingDeclaration();
        }
        return false;
    }

    @NotNull
    public MemberScope getBuiltInsPackageScope() {
        return builtInsModule.getPackage(BUILT_INS_PACKAGE_FQ_NAME).getMemberScope();
    }

    @NotNull
    public ClassDescriptor getBuiltInClassByFqName(@NotNull FqName fqName) {
        ClassDescriptor descriptor = DescriptorUtilKt.resolveClassByFqName(builtInsModule, fqName, NoLookupLocation.FROM_BUILTINS);
        assert descriptor != null : "Can't find built-in class " + fqName;
        return descriptor;
    }

    @NotNull
    private ClassDescriptor getBuiltInClassByName(@NotNull String simpleName) {
        return builtInClassesByName.invoke(Name.identifier(simpleName));
    }

    @NotNull
    public ClassDescriptor getAny() {
        return getBuiltInClassByName("Any");
    }

    @NotNull
    public ClassDescriptor getNothing() {
        return getBuiltInClassByName("Nothing");
    }

    @NotNull
    private ClassDescriptor getPrimitiveClassDescriptor(@NotNull PrimitiveType type) {
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
    public static String getFunctionName(int parameterCount) {
        return "Function" + parameterCount;
    }

    @NotNull
    public static ClassId getFunctionClassId(int parameterCount) {
        return new ClassId(BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(getFunctionName(parameterCount)));
    }

    @NotNull
    public ClassDescriptor getFunction(int parameterCount) {
        return getBuiltInClassByName(getFunctionName(parameterCount));
    }

    @NotNull
    public ClassDescriptor getSuspendFunction(int parameterCount) {
        Name name = Name.identifier(FunctionClassDescriptor.Kind.SuspendFunction.getClassNamePrefix() + parameterCount);
        return getBuiltInClassByFqName(COROUTINES_PACKAGE_FQ_NAME_RELEASE.child(name));
    }

    @NotNull
    public ClassDescriptor getThrowable() {
        return getBuiltInClassByName("Throwable");
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
    public ClassDescriptor getKClass() {
        return getBuiltInClassByFqName(FQ_NAMES.kClass.toSafe());
    }

    @NotNull
    public ClassDescriptor getKCallable() {
        return getBuiltInClassByFqName(FQ_NAMES.kCallable.toSafe());
    }

    @NotNull
    public ClassDescriptor getKProperty() {
        return getBuiltInClassByFqName(FQ_NAMES.kPropertyFqName.toSafe());
    }

    @NotNull
    public ClassDescriptor getKProperty0() {
        return getBuiltInClassByFqName(FQ_NAMES.kProperty0.toSafe());
    }

    @NotNull
    public ClassDescriptor getKProperty1() {
        return getBuiltInClassByFqName(FQ_NAMES.kProperty1.toSafe());
    }

    @NotNull
    public ClassDescriptor getKProperty2() {
        return getBuiltInClassByFqName(FQ_NAMES.kProperty2.toSafe());
    }

    @NotNull
    public ClassDescriptor getKMutableProperty0() {
        return getBuiltInClassByFqName(FQ_NAMES.kMutableProperty0.toSafe());
    }

    @NotNull
    public ClassDescriptor getKMutableProperty1() {
        return getBuiltInClassByFqName(FQ_NAMES.kMutableProperty1.toSafe());
    }

    @NotNull
    public ClassDescriptor getKMutableProperty2() {
        return getBuiltInClassByFqName(FQ_NAMES.kMutableProperty2.toSafe());
    }

    @NotNull
    public ClassDescriptor getIterator() {
        return getBuiltInClassByFqName(FQ_NAMES.iterator);
    }

    @NotNull
    public ClassDescriptor getIterable() {
        return getBuiltInClassByFqName(FQ_NAMES.iterable);
    }

    @NotNull
    public ClassDescriptor getMutableIterable() {
        return getBuiltInClassByFqName(FQ_NAMES.mutableIterable);
    }

    @NotNull
    public ClassDescriptor getMutableIterator() {
        return getBuiltInClassByFqName(FQ_NAMES.mutableIterator);
    }

    @NotNull
    public ClassDescriptor getCollection() {
        return getBuiltInClassByFqName(FQ_NAMES.collection);
    }

    @NotNull
    public ClassDescriptor getMutableCollection() {
        return getBuiltInClassByFqName(FQ_NAMES.mutableCollection);
    }

    @NotNull
    public ClassDescriptor getList() {
        return getBuiltInClassByFqName(FQ_NAMES.list);
    }

    @NotNull
    public ClassDescriptor getMutableList() {
        return getBuiltInClassByFqName(FQ_NAMES.mutableList);
    }

    @NotNull
    public ClassDescriptor getSet() {
        return getBuiltInClassByFqName(FQ_NAMES.set);
    }

    @NotNull
    public ClassDescriptor getMutableSet() {
        return getBuiltInClassByFqName(FQ_NAMES.mutableSet);
    }

    @NotNull
    public ClassDescriptor getMap() {
        return getBuiltInClassByFqName(FQ_NAMES.map);
    }

    @NotNull
    public ClassDescriptor getMutableMap() {
        return getBuiltInClassByFqName(FQ_NAMES.mutableMap);
    }

    @NotNull
    public ClassDescriptor getMapEntry() {
        ClassDescriptor classDescriptor = DescriptorUtils.getInnerClassByName(getMap(), "Entry", NoLookupLocation.FROM_BUILTINS);
        assert classDescriptor != null : "Can't find Map.Entry";
        return classDescriptor;
    }

    @NotNull
    public ClassDescriptor getMutableMapEntry() {
        ClassDescriptor classDescriptor = DescriptorUtils.getInnerClassByName(getMutableMap(), "MutableEntry", NoLookupLocation.FROM_BUILTINS);
        assert classDescriptor != null : "Can't find MutableMap.MutableEntry";
        return classDescriptor;
    }

    @NotNull
    public ClassDescriptor getListIterator() {
        return getBuiltInClassByFqName(FQ_NAMES.listIterator);
    }

    @NotNull
    public ClassDescriptor getMutableListIterator() {
        return getBuiltInClassByFqName(FQ_NAMES.mutableListIterator);
    }

    @NotNull
    private SimpleType getBuiltInTypeByClassName(@NotNull String classSimpleName) {
        return getBuiltInClassByName(classSimpleName).getDefaultType();
    }

    @NotNull
    public SimpleType getNothingType() {
        return getNothing().getDefaultType();
    }

    @NotNull
    public SimpleType getNullableNothingType() {
        return getNothingType().makeNullableAsSpecified(true);
    }

    @NotNull
    public SimpleType getAnyType() {
        return getAny().getDefaultType();
    }

    @NotNull
    public SimpleType getNullableAnyType() {
        return getAnyType().makeNullableAsSpecified(true);
    }

    @NotNull
    public SimpleType getDefaultBound() {
        return getNullableAnyType();
    }

    @NotNull
    public SimpleType getPrimitiveKotlinType(@NotNull PrimitiveType type) {
        return getPrimitiveClassDescriptor(type).getDefaultType();
    }

    @NotNull
    public SimpleType getNumberType() {
        return getNumber().getDefaultType();
    }

    @NotNull
    public SimpleType getByteType() {
        return getPrimitiveKotlinType(BYTE);
    }

    @NotNull
    public SimpleType getShortType() {
        return getPrimitiveKotlinType(SHORT);
    }

    @NotNull
    public SimpleType getIntType() {
        return getPrimitiveKotlinType(INT);
    }

    @NotNull
    public SimpleType getLongType() {
        return getPrimitiveKotlinType(LONG);
    }

    @NotNull
    public SimpleType getFloatType() {
        return getPrimitiveKotlinType(FLOAT);
    }

    @NotNull
    public SimpleType getDoubleType() {
        return getPrimitiveKotlinType(DOUBLE);
    }

    @NotNull
    public SimpleType getCharType() {
        return getPrimitiveKotlinType(CHAR);
    }

    @NotNull
    public SimpleType getBooleanType() {
        return getPrimitiveKotlinType(BOOLEAN);
    }

    @NotNull
    public SimpleType getUnitType() {
        return getUnit().getDefaultType();
    }

    @NotNull
    public SimpleType getStringType() {
        return getString().getDefaultType();
    }

    @NotNull
    public KotlinType getIterableType() {
        return getIterable().getDefaultType();
    }

    @NotNull
    public KotlinType getArrayElementType(@NotNull KotlinType arrayType) {
        if (isArray(arrayType)) {
            if (arrayType.getArguments().size() != 1) {
                throw new IllegalStateException();
            }
            return arrayType.getArguments().get(0).getType();
        }
        KotlinType notNullArrayType = TypeUtils.makeNotNullable(arrayType);
        //noinspection SuspiciousMethodCalls
        KotlinType primitiveType = primitives.invoke().kotlinArrayTypeToPrimitiveKotlinType.get(notNullArrayType);
        if (primitiveType != null) return primitiveType;

        ModuleDescriptor module = DescriptorUtils.getContainingModuleOrNull(notNullArrayType);
        if (module != null) {
            KotlinType unsignedType = getElementTypeForUnsignedArray(notNullArrayType, module);
            if (unsignedType != null) return unsignedType;
        }


        throw new IllegalStateException("not array: " + arrayType);
    }

    @Nullable
    private static KotlinType getElementTypeForUnsignedArray(@NotNull KotlinType notNullArrayType, @NotNull ModuleDescriptor module) {
        ClassifierDescriptor descriptor = notNullArrayType.getConstructor().getDeclarationDescriptor();
        if (descriptor == null) return null;
        if (!UnsignedTypes.INSTANCE.isShortNameOfUnsignedArray(descriptor.getName())) return null;

        ClassId arrayClassId = DescriptorUtilsKt.getClassId(descriptor);
        if (arrayClassId == null) return null;

        ClassId elementClassId = UnsignedTypes.INSTANCE.getUnsignedClassIdByArrayClassId(arrayClassId);
        if (elementClassId == null) return null;

        ClassDescriptor elementClassDescriptor = FindClassInModuleKt.findClassAcrossModuleDependencies(module, elementClassId);
        if (elementClassDescriptor == null) return null;

        return elementClassDescriptor.getDefaultType();
    }

    @NotNull
    public SimpleType getPrimitiveArrayKotlinType(@NotNull PrimitiveType primitiveType) {
        return primitives.invoke().primitiveTypeToArrayKotlinType.get(primitiveType);
    }

    /**
     * @return {@code null} if not primitive
     */
    @Nullable
    public SimpleType getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(@NotNull KotlinType kotlinType) {
        SimpleType primitiveArray = primitives.invoke().primitiveKotlinTypeToKotlinArrayType.get(kotlinType);
        if (primitiveArray != null) return primitiveArray;

        if (UnsignedTypes.INSTANCE.isUnsignedType(kotlinType)) {
            if (TypeUtils.isNullableType(kotlinType)) return null;

            ModuleDescriptor module = DescriptorUtils.getContainingModuleOrNull(kotlinType);
            if (module == null) return null;

            ClassId unsignedClassId = DescriptorUtilsKt.getClassId(kotlinType.getConstructor().getDeclarationDescriptor());
            assert unsignedClassId != null : "unsignedClassId should not be null for unsigned type " + kotlinType;

            ClassId arrayClassId = UnsignedTypes.INSTANCE.getUnsignedArrayClassIdByUnsignedClassId(unsignedClassId);
            assert arrayClassId != null : "arrayClassId should not be null for unsigned type " + unsignedClassId;

            ClassDescriptor arrayClassDescriptor = FindClassInModuleKt.findClassAcrossModuleDependencies(module, arrayClassId);
            if (arrayClassDescriptor == null) return null;

            return arrayClassDescriptor.getDefaultType();
        }

        return null;
    }

    public static boolean isPrimitiveArray(@NotNull FqNameUnsafe arrayFqName) {
        return FQ_NAMES.arrayClassFqNameToPrimitiveType.get(arrayFqName) != null;
    }

    @Nullable
    public static PrimitiveType getPrimitiveType(@NotNull DeclarationDescriptor descriptor) {
        return FQ_NAMES.primitiveTypeShortNames.contains(descriptor.getName())
               ? FQ_NAMES.fqNameToPrimitiveType.get(getFqName(descriptor))
               : null;
    }

    @Nullable
    public static PrimitiveType getPrimitiveArrayType(@NotNull DeclarationDescriptor descriptor) {
        return FQ_NAMES.primitiveArrayTypeShortNames.contains(descriptor.getName())
               ? FQ_NAMES.arrayClassFqNameToPrimitiveType.get(getFqName(descriptor))
               : null;
    }

    @NotNull
    public SimpleType getArrayType(@NotNull Variance projectionType, @NotNull KotlinType argument) {
        List<TypeProjectionImpl> types = Collections.singletonList(new TypeProjectionImpl(projectionType, argument));
        return KotlinTypeFactory.simpleNotNullType(Annotations.Companion.getEMPTY(), getArray(), types);
    }

    @NotNull
    public SimpleType getEnumType(@NotNull SimpleType argument) {
        Variance projectionType = Variance.INVARIANT;
        List<TypeProjectionImpl> types = Collections.singletonList(new TypeProjectionImpl(projectionType, argument));
        return KotlinTypeFactory.simpleNotNullType(Annotations.Companion.getEMPTY(), getEnum(), types);
    }

    @NotNull
    public SimpleType getAnnotationType() {
        return getAnnotation().getDefaultType();
    }

    public static boolean isArray(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.array);
    }

    public static boolean isArrayOrPrimitiveArray(@NotNull ClassDescriptor descriptor) {
        return classFqNameEquals(descriptor, FQ_NAMES.array) || getPrimitiveArrayType(descriptor) != null;
    }

    public static boolean isArrayOrPrimitiveArray(@NotNull KotlinType type) {
        return isArray(type) || isPrimitiveArray(type);
    }

    public static boolean isPrimitiveArray(@NotNull KotlinType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor != null && getPrimitiveArrayType(descriptor) != null;
    }

    @Nullable
    public static PrimitiveType getPrimitiveArrayElementType(@NotNull KotlinType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor == null ? null : getPrimitiveArrayType(descriptor);
    }

    @Nullable
    public static PrimitiveType getPrimitiveType(@NotNull KotlinType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor == null ? null : getPrimitiveType(descriptor);
    }

    public static boolean isPrimitiveType(@NotNull KotlinType type) {
        return !type.isMarkedNullable() && isPrimitiveTypeOrNullablePrimitiveType(type);
    }

    public static boolean isPrimitiveTypeOrNullablePrimitiveType(@NotNull KotlinType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor instanceof ClassDescriptor && isPrimitiveClass((ClassDescriptor) descriptor);
    }

    public static boolean isPrimitiveClass(@NotNull ClassDescriptor descriptor) {
        return getPrimitiveType(descriptor) != null;
    }

    private static boolean isConstructedFromGivenClass(@NotNull KotlinType type, @NotNull FqNameUnsafe fqName) {
        return isTypeConstructorForGivenClass(type.getConstructor(), fqName);
    }

    public static boolean isConstructedFromGivenClass(@NotNull KotlinType type, @NotNull FqName fqName) {
        return isConstructedFromGivenClass(type, fqName.toUnsafe());
    }

    public static boolean isTypeConstructorForGivenClass(@NotNull TypeConstructor typeConstructor, @NotNull FqNameUnsafe fqName) {
        ClassifierDescriptor descriptor = typeConstructor.getDeclarationDescriptor();
        return descriptor instanceof ClassDescriptor && classFqNameEquals(descriptor, fqName);
    }

    private static boolean classFqNameEquals(@NotNull ClassifierDescriptor descriptor, @NotNull FqNameUnsafe fqName) {
        // Quick check to avoid creation of full FqName instance
        return descriptor.getName().equals(fqName.shortName()) &&
               fqName.equals(getFqName(descriptor));
    }

    private static boolean isNotNullConstructedFromGivenClass(@NotNull KotlinType type, @NotNull FqNameUnsafe fqName) {
        return !type.isMarkedNullable() && isConstructedFromGivenClass(type, fqName);
    }

    public static boolean isSpecialClassWithNoSupertypes(@NotNull ClassDescriptor descriptor) {
        return classFqNameEquals(descriptor, FQ_NAMES.any) || classFqNameEquals(descriptor, FQ_NAMES.nothing);
    }

    public static boolean isAny(@NotNull ClassDescriptor descriptor) {
        return classFqNameEquals(descriptor, FQ_NAMES.any);
    }

    public static boolean isAny(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES.any);
    }

    public static boolean isBoolean(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._boolean);
    }

    public static boolean isBooleanOrNullableBoolean(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES._boolean);
    }

    public static boolean isBoolean(@NotNull ClassDescriptor classDescriptor) {
        return classFqNameEquals(classDescriptor, FQ_NAMES._boolean);
    }

    public static boolean isNumber(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES.number);
    }

    public static boolean isChar(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._char);
    }

    public static boolean isCharOrNullableChar(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES._char);
    }

    public static boolean isInt(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._int);
    }

    public static boolean isByte(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._byte);
    }

    public static boolean isLong(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._long);
    }

    public static boolean isLongOrNullableLong(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES._long);
    }

    public static boolean isShort(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._short);
    }

    public static boolean isFloat(@NotNull KotlinType type) {
        return isFloatOrNullableFloat(type) && !type.isMarkedNullable();
    }

    public static boolean isFloatOrNullableFloat(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES._float);
    }

    public static boolean isDouble(@NotNull KotlinType type) {
        return isDoubleOrNullableDouble(type) && !type.isMarkedNullable();
    }

    public static boolean isUByte(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES.uByteFqName.toUnsafe());
    }

    public static boolean isUShort(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES.uShortFqName.toUnsafe());
    }

    public static boolean isUInt(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES.uIntFqName.toUnsafe());
    }

    public static boolean isULong(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES.uLongFqName.toUnsafe());
    }

    public static boolean isDoubleOrNullableDouble(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES._double);
    }

    private static boolean isConstructedFromGivenClassAndNotNullable(@NotNull KotlinType type, @NotNull FqNameUnsafe fqName) {
        return isConstructedFromGivenClass(type, fqName) && !type.isMarkedNullable();
    }

    public static boolean isNothing(@NotNull KotlinType type) {
        return isNothingOrNullableNothing(type)
               && !TypeUtils.isNullableType(type);
    }

    public static boolean isNullableNothing(@NotNull KotlinType type) {
        return isNothingOrNullableNothing(type)
               && TypeUtils.isNullableType(type);
    }

    public static boolean isNothingOrNullableNothing(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.nothing);
    }

    public static boolean isAnyOrNullableAny(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.any);
    }

    public static boolean isNullableAny(@NotNull KotlinType type) {
        return isAnyOrNullableAny(type) && type.isMarkedNullable();
    }

    public static boolean isDefaultBound(@NotNull KotlinType type) {
        return isNullableAny(type);
    }

    public static boolean isUnit(@NotNull KotlinType type) {
        return isNotNullConstructedFromGivenClass(type, FQ_NAMES.unit);
    }

    public static boolean isUnitOrNullableUnit(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.unit);
    }

    public boolean isBooleanOrSubtype(@NotNull KotlinType type) {
        return KotlinTypeChecker.DEFAULT.isSubtypeOf(type, getBooleanType());
    }

    public boolean isMemberOfAny(@NotNull DeclarationDescriptor descriptor) {
        return descriptor.getContainingDeclaration() == getAny();
    }

    public static boolean isEnum(@NotNull ClassDescriptor descriptor) {
        return classFqNameEquals(descriptor, FQ_NAMES._enum);
    }

    public static boolean isEnum(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._enum);
    }

    public static boolean isComparable(@NotNull ClassDescriptor descriptor) {
        return classFqNameEquals(descriptor, FQ_NAMES.comparable.toUnsafe());
    }

    public static boolean isComparable(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES.comparable.toUnsafe());
    }


    public static boolean isCharSequence(@Nullable KotlinType type) {
        return type != null && isNotNullConstructedFromGivenClass(type, FQ_NAMES.charSequence);
    }

    public static boolean isString(@Nullable KotlinType type) {
        return type != null && isNotNullConstructedFromGivenClass(type, FQ_NAMES.string);
    }

    public static boolean isCharSequenceOrNullableCharSequence(@Nullable KotlinType type) {
        return type != null && isConstructedFromGivenClass(type, FQ_NAMES.charSequence);
    }

    public static boolean isStringOrNullableString(@Nullable KotlinType type) {
        return type != null && isConstructedFromGivenClass(type, FQ_NAMES.string);
    }

    public static boolean isCollectionOrNullableCollection(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.collection);
    }

    public static boolean isListOrNullableList(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.list);
    }

    public static boolean isSetOrNullableSet(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.set);
    }

    public static boolean isMapOrNullableMap(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.map);
    }

    public static boolean isIterableOrNullableIterable(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.iterable);
    }

    public static boolean isThrowableOrNullableThrowable(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.throwable);
    }

    public static boolean isKClass(@NotNull ClassDescriptor descriptor) {
        return classFqNameEquals(descriptor, FQ_NAMES.kClass);
    }

    public static boolean isNonPrimitiveArray(@NotNull ClassDescriptor descriptor) {
        return classFqNameEquals(descriptor, FQ_NAMES.array);
    }

    public static boolean isCloneable(@NotNull ClassDescriptor descriptor) {
        return classFqNameEquals(descriptor, FQ_NAMES.cloneable);
    }

    public static boolean isDeprecated(@NotNull DeclarationDescriptor declarationDescriptor) {
        if (declarationDescriptor.getOriginal().getAnnotations().hasAnnotation(FQ_NAMES.deprecated)) return true;

        if (declarationDescriptor instanceof PropertyDescriptor) {
            boolean isVar = ((PropertyDescriptor) declarationDescriptor).isVar();
            PropertyGetterDescriptor getter = ((PropertyDescriptor) declarationDescriptor).getGetter();
            PropertySetterDescriptor setter = ((PropertyDescriptor) declarationDescriptor).getSetter();
            return getter != null && isDeprecated(getter) && (!isVar || setter != null && isDeprecated(setter));
        }

        return false;
    }

    public static boolean isNotNullOrNullableFunctionSupertype(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.functionSupertype);
    }

    public static FqName getPrimitiveFqName(@NotNull PrimitiveType primitiveType) {
        return BUILT_INS_PACKAGE_FQ_NAME.child(primitiveType.getTypeName());
    }
}
