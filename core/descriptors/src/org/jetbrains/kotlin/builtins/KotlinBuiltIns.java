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

import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.*;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.serialization.deserialization.AdditionalSupertypes;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.io.InputStream;
import java.util.*;

import static kotlin.collections.CollectionsKt.*;
import static kotlin.collections.SetsKt.setOf;
import static org.jetbrains.kotlin.builtins.PrimitiveType.*;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName;

public abstract class KotlinBuiltIns {
    public static final Name BUILT_INS_PACKAGE_NAME = Name.identifier("kotlin");
    public static final FqName BUILT_INS_PACKAGE_FQ_NAME = FqName.topLevel(BUILT_INS_PACKAGE_NAME);
    public static final FqName ANNOTATION_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("annotation"));
    public static final FqName COLLECTIONS_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("collections"));
    public static final FqName RANGES_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("ranges"));

    public static final Set<FqName> BUILT_INS_PACKAGE_FQ_NAMES = setOf(
            BUILT_INS_PACKAGE_FQ_NAME,
            COLLECTIONS_PACKAGE_FQ_NAME,
            RANGES_PACKAGE_FQ_NAME,
            ANNOTATION_PACKAGE_FQ_NAME,
            ReflectionTypesKt.getKOTLIN_REFLECT_FQ_NAME(),
            BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("internal"))
    );

    protected final ModuleDescriptorImpl builtInsModule;
    private final BuiltinsPackageFragment builtinsPackageFragment;
    private final BuiltinsPackageFragment collectionsPackageFragment;
    private final BuiltinsPackageFragment rangesPackageFragment;
    private final BuiltinsPackageFragment annotationPackageFragment;

    private final Set<BuiltinsPackageFragment> builtinsPackageFragments;

    private final Map<PrimitiveType, KotlinType> primitiveTypeToArrayKotlinType;
    private final Map<KotlinType, KotlinType> primitiveKotlinTypeToKotlinArrayType;
    private final Map<KotlinType, KotlinType> kotlinArrayTypeToPrimitiveKotlinType;
    private final Map<FqName, BuiltinsPackageFragment> packageNameToPackageFragment;

    public static final FqNames FQ_NAMES = new FqNames();

    protected KotlinBuiltIns() {
        LockBasedStorageManager storageManager = new LockBasedStorageManager();
        builtInsModule = new ModuleDescriptorImpl(
                Name.special("<built-ins module>"), storageManager, ModuleParameters.Empty.INSTANCE, this
        );

        PackageFragmentProvider packageFragmentProvider = BuiltInsPackageFragmentProviderKt.createBuiltInPackageFragmentProvider(
                storageManager, builtInsModule, BUILT_INS_PACKAGE_FQ_NAMES,
                new BuiltInFictitiousFunctionClassFactory(storageManager, builtInsModule),
                getAdditionalSupertypesProvider(),
                new Function1<String, InputStream>() {
                    @Override
                    public InputStream invoke(String path) {
                        return KotlinBuiltIns.class.getClassLoader().getResourceAsStream(path);
                    }
                }
        );

        builtInsModule.initialize(packageFragmentProvider);
        builtInsModule.setDependencies(builtInsModule);

        packageNameToPackageFragment = new LinkedHashMap<FqName, BuiltinsPackageFragment>();

        builtinsPackageFragment = getBuiltinsPackageFragment(packageFragmentProvider, packageNameToPackageFragment, BUILT_INS_PACKAGE_FQ_NAME);
        collectionsPackageFragment = getBuiltinsPackageFragment(packageFragmentProvider, packageNameToPackageFragment, COLLECTIONS_PACKAGE_FQ_NAME);
        rangesPackageFragment = getBuiltinsPackageFragment(packageFragmentProvider, packageNameToPackageFragment, RANGES_PACKAGE_FQ_NAME);
        annotationPackageFragment = getBuiltinsPackageFragment(packageFragmentProvider, packageNameToPackageFragment, ANNOTATION_PACKAGE_FQ_NAME);

        builtinsPackageFragments = new LinkedHashSet<BuiltinsPackageFragment>(packageNameToPackageFragment.values());

        primitiveTypeToArrayKotlinType = new EnumMap<PrimitiveType, KotlinType>(PrimitiveType.class);
        primitiveKotlinTypeToKotlinArrayType = new HashMap<KotlinType, KotlinType>();
        kotlinArrayTypeToPrimitiveKotlinType = new HashMap<KotlinType, KotlinType>();
        for (PrimitiveType primitive : PrimitiveType.values()) {
            makePrimitive(primitive);
        }
    }

    @NotNull
    protected AdditionalSupertypes getAdditionalSupertypesProvider() {
        return AdditionalSupertypes.None.INSTANCE;
    }

    private void makePrimitive(@NotNull PrimitiveType primitiveType) {
        KotlinType type = getBuiltInTypeByClassName(primitiveType.getTypeName().asString());
        KotlinType arrayType = getBuiltInTypeByClassName(primitiveType.getArrayTypeName().asString());

        primitiveTypeToArrayKotlinType.put(primitiveType, arrayType);
        primitiveKotlinTypeToKotlinArrayType.put(type, arrayType);
        kotlinArrayTypeToPrimitiveKotlinType.put(arrayType, type);
    }


    @NotNull
    private BuiltinsPackageFragment getBuiltinsPackageFragment(
            PackageFragmentProvider fragmentProvider,
            Map<FqName, BuiltinsPackageFragment> packageNameToPackageFragment,
            FqName packageFqName
    ) {
        BuiltinsPackageFragment packageFragment = (BuiltinsPackageFragment) single(fragmentProvider.getPackageFragments(packageFqName));
        packageNameToPackageFragment.put(packageFqName, packageFragment);
        return packageFragment;
    }

    public static class FqNames {
        public final FqNameUnsafe any = fqNameUnsafe("Any");
        public final FqNameUnsafe nothing = fqNameUnsafe("Nothing");
        public final FqNameUnsafe cloneable = fqNameUnsafe("Cloneable");
        public final FqNameUnsafe suppress = fqNameUnsafe("Suppress");
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



        public final FqName throwable = fqName("Throwable");

        public final FqName deprecated = fqName("Deprecated");
        public final FqName deprecationLevel = fqName("DeprecationLevel");
        public final FqName extensionFunctionType = fqName("ExtensionFunctionType");
        public final FqName target = annotationName("Target");
        public final FqName annotationTarget = annotationName("AnnotationTarget");
        public final FqName annotationRetention = annotationName("AnnotationRetention");
        public final FqName retention = annotationName("Retention");
        public final FqName repeatable = annotationName("Repeatable");
        public final FqName mustBeDocumented = annotationName("MustBeDocumented");
        public final FqName unsafeVariance = fqName("UnsafeVariance");

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

        private final FqNameUnsafe _collection = collection.toUnsafe();
        private final FqNameUnsafe _list = list.toUnsafe();
        private final FqNameUnsafe _set = set.toUnsafe();
        private final FqNameUnsafe _iterable = iterable.toUnsafe();

        public final FqNameUnsafe kClass = reflect("KClass");
        public final FqNameUnsafe kCallable = reflect("KCallable");
        public final ClassId kProperty = ClassId.topLevel(reflect("KProperty").toSafe());

        // TODO: remove in 1.0
        public final FqName deprecatedExtensionAnnotation = fqName("Extension");

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
        private static FqName collectionsFqName(@NotNull String simpleName) {
            return COLLECTIONS_PACKAGE_FQ_NAME.child(Name.identifier(simpleName));
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public ModuleDescriptorImpl getBuiltInsModule() {
        return builtInsModule;
    }

    @NotNull
    public Set<BuiltinsPackageFragment> getBuiltinsPackageFragments() {
        return builtinsPackageFragments;
    }

    @NotNull
    public PackageFragmentDescriptor getBuiltInsPackageFragment() {
        return builtinsPackageFragment;
    }

    @NotNull
    public BuiltinsPackageFragment getCollectionsPackageFragment() {
        return collectionsPackageFragment;
    }

    @NotNull
    public BuiltinsPackageFragment getRangesPackageFragment() {
        return rangesPackageFragment;
    }

    @NotNull
    public BuiltinsPackageFragment getAnnotationPackageFragment() {
        return annotationPackageFragment;
    }

    public boolean isBuiltInPackageFragment(@Nullable PackageFragmentDescriptor packageFragment) {
        return packageFragment != null && packageFragment.getContainingDeclaration() == getBuiltInsModule();
    }

    @NotNull
    public MemberScope getBuiltInsPackageScope() {
        return builtinsPackageFragment.getMemberScope();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // GET CLASS

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public ClassDescriptor getAnnotationClassByName(@NotNull Name simpleName) {
        return getBuiltInClassByName(simpleName, annotationPackageFragment);
    }

    @NotNull
    public ClassDescriptor getBuiltInClassByName(@NotNull Name simpleName) {
        return getBuiltInClassByName(simpleName, getBuiltInsPackageFragment());
    }

    @NotNull
    private static ClassDescriptor getBuiltInClassByName(@NotNull Name simpleName, @NotNull PackageFragmentDescriptor packageFragment) {
        ClassDescriptor classDescriptor = getBuiltInClassByNameNullable(simpleName, packageFragment);
        assert classDescriptor != null : "Built-in class " + simpleName + " is not found";
        return classDescriptor;
    }

    @Nullable
    public ClassDescriptor getBuiltInClassByNameNullable(@NotNull Name simpleName) {
        return getBuiltInClassByNameNullable(simpleName, getBuiltInsPackageFragment());
    }

    @Nullable
    public ClassDescriptor getBuiltInClassByFqNameNullable(@NotNull FqName fqName) {
        if (!fqName.isRoot()) {
            FqName parent = fqName.parent();
            BuiltinsPackageFragment packageFragment = packageNameToPackageFragment.get(parent);
            if (packageFragment != null) {
                return getBuiltInClassByNameNullable(fqName.shortName(), packageFragment);
            }
        }
        return null;
    }

    @Nullable
    private static ClassDescriptor getBuiltInClassByNameNullable(@NotNull Name simpleName, @NotNull PackageFragmentDescriptor packageFragment) {
        ClassifierDescriptor classifier = packageFragment.getMemberScope().getContributedClassifier(
                simpleName,
                NoLookupLocation.FROM_BUILTINS);

        assert classifier == null ||
               classifier instanceof ClassDescriptor : "Must be a class descriptor " + simpleName + ", but was " + classifier;
        return (ClassDescriptor) classifier;
    }

    @NotNull
    private ClassDescriptor getBuiltInClassByName(@NotNull String simpleName) {
        return getBuiltInClassByName(Name.identifier(simpleName));
    }

    @NotNull
    private static ClassDescriptor getBuiltInClassByName(@NotNull String simpleName, PackageFragmentDescriptor packageFragment) {
        return getBuiltInClassByName(Name.identifier(simpleName), packageFragment);
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
        return SetsKt.<DeclarationDescriptor>setOf(
                getBuiltInClassByName("CharRange", rangesPackageFragment),
                getBuiltInClassByName("IntRange", rangesPackageFragment)
                // TODO: contains in LongRange should be optimized too
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
    public static String getFunctionName(int parameterCount) {
        return "Function" + parameterCount;
    }

    @NotNull
    public static String getExtensionFunctionName(int parameterCount) {
        return getFunctionName(parameterCount + 1);
    }

    @NotNull
    public ClassDescriptor getFunction(int parameterCount) {
        return getBuiltInClassByName(getFunctionName(parameterCount));
    }

    /**
     * @return the descriptor representing the class kotlin.Function{parameterCount + 1}
     * @deprecated there are no ExtensionFunction classes anymore, use {@link #getFunction(int)} instead
     */
    @Deprecated
    @NotNull
    public ClassDescriptor getExtensionFunction(int parameterCount) {
        return getBuiltInClassByName(getExtensionFunctionName((parameterCount)));
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
        return getBuiltInClassByName(FQ_NAMES.deprecated.shortName());
    }

    @NotNull
    public ClassDescriptor getDeprecationLevelEnum() {
        return getBuiltInClassByName(FQ_NAMES.deprecationLevel.shortName());
    }

    @Nullable
    private static ClassDescriptor getEnumEntry(@NotNull ClassDescriptor enumDescriptor, @NotNull String entryName) {
        ClassifierDescriptor result = enumDescriptor.getUnsubstitutedInnerClassesScope().getContributedClassifier(
                Name.identifier(entryName), NoLookupLocation.FROM_BUILTINS
        );
        return result instanceof ClassDescriptor ? (ClassDescriptor) result : null;
    }

    @Nullable
    public ClassDescriptor getDeprecationLevelEnumEntry(@NotNull String level) {
        return getEnumEntry(getDeprecationLevelEnum(), level);
    }

    @NotNull
    public ClassDescriptor getTargetAnnotation() {
        return getAnnotationClassByName(FQ_NAMES.target.shortName());
    }

    @NotNull
    public ClassDescriptor getRetentionAnnotation() {
        return getAnnotationClassByName(FQ_NAMES.retention.shortName());
    }

    @NotNull
    public ClassDescriptor getRepeatableAnnotation() {
        return getAnnotationClassByName(FQ_NAMES.repeatable.shortName());
    }

    @NotNull
    public ClassDescriptor getMustBeDocumentedAnnotation() {
        return getAnnotationClassByName(FQ_NAMES.mustBeDocumented.shortName());
    }

    @NotNull
    public ClassDescriptor getAnnotationTargetEnum() {
        return getAnnotationClassByName(FQ_NAMES.annotationTarget.shortName());
    }

    @Nullable
    public ClassDescriptor getAnnotationTargetEnumEntry(@NotNull KotlinTarget target) {
        return getEnumEntry(getAnnotationTargetEnum(), target.name());
    }

    @NotNull
    public ClassDescriptor getAnnotationRetentionEnum() {
        return getAnnotationClassByName(FQ_NAMES.annotationRetention.shortName());
    }

    @Nullable
    public ClassDescriptor getAnnotationRetentionEnumEntry(@NotNull KotlinRetention retention) {
        return getEnumEntry(getAnnotationRetentionEnum(), retention.name());
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
        return getBuiltInClassByName("Iterator", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getIterable() {
        return getBuiltInClassByName("Iterable", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getMutableIterable() {
        return getBuiltInClassByName("MutableIterable", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getMutableIterator() {
        return getBuiltInClassByName("MutableIterator", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getCollection() {
        return getBuiltInClassByName("Collection", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getMutableCollection() {
        return getBuiltInClassByName("MutableCollection", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getList() {
        return getBuiltInClassByName("List", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getMutableList() {
        return getBuiltInClassByName("MutableList", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getSet() {
        return getBuiltInClassByName("Set", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getMutableSet() {
        return getBuiltInClassByName("MutableSet", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getMap() {
        return getBuiltInClassByName("Map", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getMutableMap() {
        return getBuiltInClassByName("MutableMap", collectionsPackageFragment);
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
        return getBuiltInClassByName("ListIterator", collectionsPackageFragment);
    }

    @NotNull
    public ClassDescriptor getMutableListIterator() {
        return getBuiltInClassByName("MutableListIterator", collectionsPackageFragment);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // GET TYPE

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    private KotlinType getBuiltInTypeByClassName(@NotNull String classSimpleName) {
        return getBuiltInClassByName(classSimpleName).getDefaultType();
    }

    // Special

    @NotNull
    public KotlinType getNothingType() {
        return getNothing().getDefaultType();
    }

    @NotNull
    public KotlinType getNullableNothingType() {
        return TypeUtils.makeNullable(getNothingType());
    }

    @NotNull
    public KotlinType getAnyType() {
        return getAny().getDefaultType();
    }

    @NotNull
    public KotlinType getNullableAnyType() {
        return TypeUtils.makeNullable(getAnyType());
    }

    // Primitive

    @NotNull
    public KotlinType getPrimitiveKotlinType(@NotNull PrimitiveType type) {
        return getPrimitiveClassDescriptor(type).getDefaultType();
    }

    @NotNull
    public KotlinType getByteType() {
        return getPrimitiveKotlinType(BYTE);
    }

    @NotNull
    public KotlinType getShortType() {
        return getPrimitiveKotlinType(SHORT);
    }

    @NotNull
    public KotlinType getIntType() {
        return getPrimitiveKotlinType(INT);
    }

    @NotNull
    public KotlinType getLongType() {
        return getPrimitiveKotlinType(LONG);
    }

    @NotNull
    public KotlinType getFloatType() {
        return getPrimitiveKotlinType(FLOAT);
    }

    @NotNull
    public KotlinType getDoubleType() {
        return getPrimitiveKotlinType(DOUBLE);
    }

    @NotNull
    public KotlinType getCharType() {
        return getPrimitiveKotlinType(CHAR);
    }

    @NotNull
    public KotlinType getBooleanType() {
        return getPrimitiveKotlinType(BOOLEAN);
    }

    // Recognized

    @NotNull
    public KotlinType getUnitType() {
        return getUnit().getDefaultType();
    }

    @NotNull
    public KotlinType getStringType() {
        return getString().getDefaultType();
    }

    @NotNull
    public KotlinType getArrayElementType(@NotNull KotlinType arrayType) {
        if (isArray(arrayType)) {
            if (arrayType.getArguments().size() != 1) {
                throw new IllegalStateException();
            }
            return arrayType.getArguments().get(0).getType();
        }
        KotlinType primitiveType = kotlinArrayTypeToPrimitiveKotlinType.get(TypeUtils.makeNotNullable(arrayType));
        if (primitiveType == null) {
            throw new IllegalStateException("not array: " + arrayType);
        }
        return primitiveType;
    }

    @NotNull
    public KotlinType getPrimitiveArrayKotlinType(@NotNull PrimitiveType primitiveType) {
        return primitiveTypeToArrayKotlinType.get(primitiveType);
    }

    /**
     * @return {@code null} if not primitive
     */
    @Nullable
    public KotlinType getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(@NotNull KotlinType kotlinType) {
        return primitiveKotlinTypeToKotlinArrayType.get(kotlinType);
    }

    public static boolean isPrimitiveArray(@NotNull FqNameUnsafe arrayFqName) {
        return getPrimitiveTypeByArrayClassFqName(arrayFqName) != null;
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
    public KotlinType getArrayType(@NotNull Variance projectionType, @NotNull KotlinType argument) {
        List<TypeProjectionImpl> types = Collections.singletonList(new TypeProjectionImpl(projectionType, argument));
        return KotlinTypeImpl.create(
                Annotations.Companion.getEMPTY(),
                getArray(),
                false,
                types
        );
    }

    @NotNull
    public KotlinType getEnumType(@NotNull KotlinType argument) {
        Variance projectionType = Variance.INVARIANT;
        List<TypeProjectionImpl> types = Collections.singletonList(new TypeProjectionImpl(projectionType, argument));
        return KotlinTypeImpl.create(
                Annotations.Companion.getEMPTY(),
                getEnum(),
                false,
                types
        );
    }

    @NotNull
    public KotlinType getAnnotationType() {
        return getAnnotation().getDefaultType();
    }

    @NotNull
    public AnnotationDescriptor createExtensionAnnotation() {
        return new AnnotationDescriptorImpl(getBuiltInClassByName(FQ_NAMES.extensionFunctionType.shortName()).getDefaultType(),
                                            Collections.<ValueParameterDescriptor, ConstantValue<?>>emptyMap(), SourceElement.NO_SOURCE);
    }

    private static boolean isTypeAnnotatedWithExtension(@NotNull KotlinType type) {
        return type.getAnnotations().findAnnotation(FQ_NAMES.extensionFunctionType) != null ||
               type.getAnnotations().findAnnotation(FQ_NAMES.deprecatedExtensionAnnotation) != null;
    }

    @NotNull
    public KotlinType getFunctionType(
            @NotNull Annotations annotations,
            @Nullable KotlinType receiverType,
            @NotNull List<KotlinType> parameterTypes,
            @NotNull KotlinType returnType
    ) {
        List<TypeProjection> arguments = getFunctionTypeArgumentProjections(receiverType, parameterTypes, returnType);
        int size = parameterTypes.size();
        ClassDescriptor classDescriptor = receiverType == null ? getFunction(size) : getExtensionFunction(size);

        Annotations typeAnnotations = receiverType == null ? annotations : addExtensionFunctionTypeAnnotation(annotations);

        return KotlinTypeImpl.create(typeAnnotations, classDescriptor, false, arguments);
    }

    @NotNull
    private Annotations addExtensionFunctionTypeAnnotation(@NotNull Annotations annotations) {
        if (annotations.findAnnotation(FQ_NAMES.extensionFunctionType) != null) return annotations;

        // TODO: preserve laziness of given annotations
        return new AnnotationsImpl(plus(annotations, listOf(createExtensionAnnotation())));
    }

    @NotNull
    public static List<TypeProjection> getFunctionTypeArgumentProjections(
            @Nullable KotlinType receiverType,
            @NotNull List<KotlinType> parameterTypes,
            @NotNull KotlinType returnType
    ) {
        List<TypeProjection> arguments = new ArrayList<TypeProjection>(parameterTypes.size() + (receiverType != null ? 1 : 0) + 1);
        if (receiverType != null) {
            arguments.add(defaultProjection(receiverType));
        }
        for (KotlinType parameterType : parameterTypes) {
            arguments.add(defaultProjection(parameterType));
        }
        arguments.add(defaultProjection(returnType));
        return arguments;
    }

    private static TypeProjection defaultProjection(KotlinType returnType) {
        return new TypeProjectionImpl(Variance.INVARIANT, returnType);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // IS TYPE

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean isArray(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES.array);
    }

    public static boolean isPrimitiveArray(@NotNull KotlinType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor != null && getPrimitiveTypeByArrayClassFqName(getFqName(descriptor)) != null;
    }

    public static boolean isPrimitiveType(@NotNull KotlinType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return !type.isMarkedNullable() && descriptor instanceof ClassDescriptor && isPrimitiveClass((ClassDescriptor) descriptor);
    }

    public static boolean isPrimitiveClass(@NotNull ClassDescriptor descriptor) {
        return getPrimitiveTypeByFqName(getFqName(descriptor)) != null;
    }

    // Functions

    public static boolean isFunctionOrExtensionFunctionType(@NotNull KotlinType type) {
        return isFunctionType(type) || isExtensionFunctionType(type);
    }

    public static boolean isFunctionType(@NotNull KotlinType type) {
        if (isExactFunctionType(type)) return true;

        for (KotlinType superType : type.getConstructor().getSupertypes()) {
            if (isFunctionType(superType)) return true;
        }

        return false;
    }

    public static boolean isExtensionFunctionType(@NotNull KotlinType type) {
        if (isExactExtensionFunctionType(type)) return true;

        for (KotlinType superType : type.getConstructor().getSupertypes()) {
            if (isExtensionFunctionType(superType)) return true;
        }

        return false;
    }

    public static boolean isExactFunctionOrExtensionFunctionType(@NotNull KotlinType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor != null && isNumberedFunctionClassFqName(getFqName(descriptor));
    }

    public static boolean isExactFunctionType(@NotNull KotlinType type) {
        return isExactFunctionOrExtensionFunctionType(type) && !isTypeAnnotatedWithExtension(type);
    }

    public static boolean isExactExtensionFunctionType(@NotNull KotlinType type) {
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
        return BuiltInFictitiousFunctionClassFactory.isFunctionClassName(shortName, BUILT_INS_PACKAGE_FQ_NAME);
    }

    @Nullable
    public static KotlinType getReceiverType(@NotNull KotlinType type) {
        assert isFunctionOrExtensionFunctionType(type) : type;
        if (isExtensionFunctionType(type)) {
            // TODO: this is incorrect when a class extends from an extension function and swaps type arguments
            return type.getArguments().get(0).getType();
        }
        return null;
    }

    @NotNull
    public static List<ValueParameterDescriptor> getValueParameters(@NotNull FunctionDescriptor functionDescriptor, @NotNull KotlinType type) {
        assert isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> parameterTypes = getParameterTypeProjectionsFromFunctionType(type);
        List<ValueParameterDescriptor> valueParameters = new ArrayList<ValueParameterDescriptor>(parameterTypes.size());
        for (int i = 0; i < parameterTypes.size(); i++) {
            TypeProjection parameterType = parameterTypes.get(i);
            ValueParameterDescriptorImpl valueParameterDescriptor = new ValueParameterDescriptorImpl(
                    functionDescriptor, null, i, Annotations.Companion.getEMPTY(),
                    Name.identifier("p" + (i + 1)), parameterType.getType(),
                    /* declaresDefaultValue = */ false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    null, SourceElement.NO_SOURCE
            );
            valueParameters.add(valueParameterDescriptor);
        }
        return valueParameters;
    }

    @NotNull
    public static KotlinType getReturnTypeFromFunctionType(@NotNull KotlinType type) {
        assert isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        return arguments.get(arguments.size() - 1).getType();
    }

    @NotNull
    public static List<TypeProjection> getParameterTypeProjectionsFromFunctionType(@NotNull KotlinType type) {
        assert isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        int first = isExtensionFunctionType(type) ? 1 : 0;
        int last = arguments.size() - 2;
        // TODO: fix bugs associated with this here and in neighboring methods, see KT-9820
        assert first <= last + 1 : "Not an exact function type: " + type;
        List<TypeProjection> parameterTypes = new ArrayList<TypeProjection>(last - first + 1);
        for (int i = first; i <= last; i++) {
            parameterTypes.add(arguments.get(i));
        }
        return parameterTypes;
    }

    // Recognized & special

    private static boolean isConstructedFromGivenClass(@NotNull KotlinType type, @NotNull FqNameUnsafe fqName) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor != null &&
               /* quick check to avoid creation of full FqName instance */ descriptor.getName().equals(fqName.shortName()) &&
               fqName.equals(getFqName(descriptor));
    }

    private static boolean isNotNullConstructedFromGivenClass(@NotNull KotlinType type, @NotNull FqNameUnsafe fqName) {
        return !type.isMarkedNullable() && isConstructedFromGivenClass(type, fqName);
    }

    public static boolean isSpecialClassWithNoSupertypes(@NotNull ClassDescriptor descriptor) {
        FqNameUnsafe fqName = getFqName(descriptor);
        return FQ_NAMES.any.equals(fqName) || FQ_NAMES.nothing.equals(fqName);
    }

    public static boolean isAny(@NotNull ClassDescriptor descriptor) {
        return isAny(getFqName(descriptor));
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
        return FQ_NAMES._boolean.equals(getFqName(classDescriptor));
    }

    public static boolean isChar(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._char);
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

    public static boolean isShort(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._short);
    }

    public static boolean isFloat(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._float);
    }

    public static boolean isDouble(@NotNull KotlinType type) {
        return isConstructedFromGivenClassAndNotNullable(type, FQ_NAMES._double);
    }

    private static boolean isConstructedFromGivenClassAndNotNullable(@NotNull KotlinType type, @NotNull FqNameUnsafe fqName) {
        return isConstructedFromGivenClass(type, fqName) && !type.isMarkedNullable();
    }

    public static boolean isAny(@NotNull FqNameUnsafe fqName) {
        return FQ_NAMES.any.equals(fqName);
    }

    public static boolean isNothing(@NotNull KotlinType type) {
        return isNothingOrNullableNothing(type)
               && !type.isMarkedNullable();
    }

    public static boolean isNullableNothing(@NotNull KotlinType type) {
        return isNothingOrNullableNothing(type)
               && type.isMarkedNullable();
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

    public boolean isBooleanOrSubtype(@NotNull KotlinType type) {
        return KotlinTypeChecker.DEFAULT.isSubtypeOf(type, getBooleanType());
    }

    public boolean isMemberOfAny(@NotNull DeclarationDescriptor descriptor) {
        return descriptor.getContainingDeclaration() == getAny();
    }

    public static boolean isString(@Nullable KotlinType type) {
        return type != null && isNotNullConstructedFromGivenClass(type, FQ_NAMES.string);
    }

    public static boolean isCollectionOrNullableCollection(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES._collection);
    }

    public static boolean isListOrNullableList(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES._list);
    }

    public static boolean isSetOrNullableSet(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES._set);
    }

    public static boolean isIterableOrNullableIterable(@NotNull KotlinType type) {
        return isConstructedFromGivenClass(type, FQ_NAMES._iterable);
    }

    public static boolean isKClass(@NotNull ClassDescriptor descriptor) {
        return FQ_NAMES.kClass.equals(getFqName(descriptor));
    }

    public static boolean isNonPrimitiveArray(@NotNull ClassDescriptor descriptor) {
        return FQ_NAMES.array.equals(getFqName(descriptor));
    }

    public static boolean isCloneable(@NotNull ClassDescriptor descriptor) {
        return FQ_NAMES.cloneable.equals(getFqName(descriptor));
    }

    public static boolean isDeprecated(@NotNull DeclarationDescriptor declarationDescriptor) {
        if (containsAnnotation(declarationDescriptor, FQ_NAMES.deprecated)) return true;

        if (declarationDescriptor instanceof PropertyDescriptor) {
            boolean isVar = ((PropertyDescriptor) declarationDescriptor).isVar();
            PropertyGetterDescriptor getter = ((PropertyDescriptor) declarationDescriptor).getGetter();
            PropertySetterDescriptor setter = ((PropertyDescriptor) declarationDescriptor).getSetter();
            return getter != null && isDeprecated(getter) && (!isVar || setter != null && isDeprecated(setter));
        }

        return false;
    }

    public static boolean isSuppressAnnotation(@NotNull AnnotationDescriptor annotationDescriptor) {
        return isConstructedFromGivenClass(annotationDescriptor.getType(), FQ_NAMES.suppress);
    }

    private static boolean containsAnnotation(DeclarationDescriptor descriptor, FqName annotationClassFqName) {
        DeclarationDescriptor original = descriptor.getOriginal();
        Annotations annotations = original.getAnnotations();

        if (annotations.findAnnotation(annotationClassFqName) != null) return true;

        AnnotationUseSiteTarget associatedUseSiteTarget = AnnotationUseSiteTarget.Companion.getAssociatedUseSiteTarget(descriptor);
        if (associatedUseSiteTarget != null) {
            if (Annotations.Companion.findUseSiteTargetedAnnotation(annotations, associatedUseSiteTarget, annotationClassFqName) != null) {
                return true;
            }
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public KotlinType getDefaultBound() {
        return getNullableAnyType();
    }


}
