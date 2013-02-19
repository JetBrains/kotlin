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

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static org.jetbrains.jet.lang.types.lang.PrimitiveType.*;

public class KotlinBuiltIns {
    public static final Name UNIT_ALIAS = Name.identifier("Unit");
    public static final JetScope STUB = JetScope.EMPTY;

    public static final String BUILT_INS_DIR = "jet";
    private static final String BUILT_INS_PACKAGE_NAME_STRING = "jet";
    private static final Name BUILT_INS_PACKAGE_NAME = Name.identifier(BUILT_INS_PACKAGE_NAME_STRING);
    public static final FqName BUILT_INS_PACKAGE_FQ_NAME = FqName.topLevel(BUILT_INS_PACKAGE_NAME);

    private static final List<String> LIBRARY_FILES = Arrays.asList(
            BUILT_INS_DIR + "/Library.jet",
            BUILT_INS_DIR + "/Numbers.jet",
            BUILT_INS_DIR + "/Ranges.jet",
            BUILT_INS_DIR + "/Progressions.jet",
            BUILT_INS_DIR + "/Iterators.jet",
            BUILT_INS_DIR + "/Arrays.jet",
            BUILT_INS_DIR + "/Enum.jet",
            BUILT_INS_DIR + "/Collections.jet",
            BUILT_INS_DIR + "/Any.jet",
            BUILT_INS_DIR + "/ExtensionFunctions.jet",
            BUILT_INS_DIR + "/Functions.jet",
            BUILT_INS_DIR + "/Nothing.jet",
            BUILT_INS_DIR + "/Tuples.jet",
            BUILT_INS_DIR + "/Unit.jet"
    );

    private static final Map<FqName, Name> ALIASES = ImmutableMap.<FqName, Name>builder()
            .put(new FqName("jet.Unit"), Name.identifier("Tuple0"))
            .build();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static volatile KotlinBuiltIns instance = null;

    private static volatile boolean initializing;
    private static Throwable initializationFailed;

    // This method must be called at least once per application run, on any project
    // before any type checking is run
    public static synchronized void initialize(@NotNull Project project) {
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
                instance = new KotlinBuiltIns(project);
                instance.initialize();
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

    @NotNull // This asserts that initialize() is called before any resolution happens
    public static KotlinBuiltIns getInstance() {
        if (initializing) {
            synchronized (KotlinBuiltIns.class) {
                assert instance != null : "Built-ins are not initialized (note: We are under the same lock as initializing and instance)";
                return instance;
            }
        }
        if (instance == null) {
            throw new IllegalStateException("Initialize standard library first");
        }
        return instance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final KotlinCodeAnalyzer analyzer;
    private final ModuleDescriptor builtInsModule;

    private volatile ImmutableSet<ClassDescriptor> nonPhysicalClasses;

    private final ImmutableSet<ClassDescriptor> functionClassesSet;

    private final ImmutableSet<ClassDescriptor> extensionFunctionClassesSet;

    @Deprecated
    private final ImmutableSet<ClassDescriptor> tupleClassesSet;

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

    private KotlinBuiltIns(@NotNull Project project) {
        try {
            this.builtInsModule = new ModuleDescriptor(Name.special("<built-ins lazy module>"));
            this.analyzer = createLazyResolveSession(project);

            this.functionClassesSet = computeIndexedClasses("Function", getFunctionTraitCount());
            this.extensionFunctionClassesSet = computeIndexedClasses("ExtensionFunction", getFunctionTraitCount());
            this.tupleClassesSet = computeIndexedClasses("Tuple", getFunctionTraitCount());

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
            this.functionClasses = new ClassDescriptor[getFunctionTraitCount()];
            for (int i = 0; i < functionClasses.length; i++) {
                functionClasses[i] = getBuiltInClassByName("Function" + i);
            }
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void initialize() {
        anyType = getBuiltInTypeByClassName("Any");
        nullableAnyType = TypeUtils.makeNullable(anyType);
        nothingType = getBuiltInTypeByClassName("Nothing");
        nullableNothingType = TypeUtils.makeNullable(nothingType);
        unitType = getBuiltInTypeByClassName("Tuple0");
        stringType = getBuiltInTypeByClassName("String");
        annotationType = getBuiltInTypeByClassName("Annotation");

        for (PrimitiveType primitive : PrimitiveType.values()) {
            makePrimitive(primitive);
        }

        nonPhysicalClasses = computeNonPhysicalClasses();

        analyzer.forceResolveAll();

        AnalyzingUtils.throwExceptionOnErrors(analyzer.getBindingContext());
    }

    @NotNull
    private KotlinCodeAnalyzer createLazyResolveSession(@NotNull Project project) throws IOException {
        List<JetFile> files = loadResourcesAsJetFiles(project, LIBRARY_FILES);
        LockBasedStorageManager storageManager = new LockBasedStorageManager();
        return new ResolveSession(
                project,
                storageManager,
                builtInsModule,
                new SpecialModuleConfiguration(),
                new FileBasedDeclarationProviderFactory(storageManager, files),
                new Function<FqName, Name>() {
                    @Override
                    public Name fun(FqName name) {
                        return ALIASES.get(name);
                    }
                },
                Predicates.in(Sets.newHashSet(new FqNameUnsafe("jet.Any"), new FqNameUnsafe("jet.Nothing"))),
                new BindingTraceContext());
    }

    @NotNull
    private static List<JetFile> loadResourcesAsJetFiles(@NotNull Project project, @NotNull List<String> libraryFiles)
            throws IOException, ProcessCanceledException
    {
        List<JetFile> files = new LinkedList<JetFile>();
        for(String path : libraryFiles) {
            InputStream stream = KotlinBuiltIns.class.getClassLoader().getResourceAsStream(path);

            if (stream == null) {
                throw new IllegalStateException("Resource not found in classpath: " + path);
            }

            //noinspection IOResourceOpenedButNotSafelyClosed
            String text = FileUtil.loadTextAndClose(new InputStreamReader(stream));
            JetFile file = (JetFile) PsiFileFactory.getInstance(project).createFileFromText(path,
                    JetFileType.INSTANCE, StringUtil.convertLineSeparators(text));
            files.add(file);
        }
        return files;
    }

    private static class SpecialModuleConfiguration implements ModuleConfiguration {

        private SpecialModuleConfiguration() {
        }

        @Override
        public List<ImportPath> getDefaultImports() {
            return DefaultModuleConfiguration.DEFAULT_JET_IMPORTS;
        }

        @Override
        public void extendNamespaceScope(@NotNull BindingTrace trace,
                @NotNull NamespaceDescriptor namespaceDescriptor,
                @NotNull WritableScope namespaceMemberScope) {
            // DO nothing
        }

        @NotNull
        @Override
        public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
            return PlatformToKotlinClassMap.EMPTY;
        }
    }

    private void makePrimitive(PrimitiveType primitiveType) {
        ClassDescriptor theClass = getBuiltInClassByName(primitiveType.getTypeName().getName());
        JetType type = new JetTypeImpl(theClass);
        ClassDescriptor arrayClass = getBuiltInClassByName(primitiveType.getArrayTypeName().getName());
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
    public ModuleDescriptor getBuiltInsModule() {
        return builtInsModule;
    }

    @NotNull
    public NamespaceDescriptor getBuiltInsPackage() {
        NamespaceDescriptor namespace = getBuiltInsModule().getRootNamespace().getMemberScope().getNamespace(BUILT_INS_PACKAGE_NAME);
        assert namespace != null : "Built ins namespace not found: " + BUILT_INS_PACKAGE_NAME;
        return namespace;
    }

    @NotNull
    public FqName getBuiltInsPackageFqName() {
        return getBuiltInsPackage().getFqName();
    }

    @NotNull
    public JetScope getBuiltInsScope() {
        return getBuiltInsPackage().getMemberScope();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // GET CLASS

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public ClassDescriptor getBuiltInClassByName(@NotNull Name simpleName) {
        ClassifierDescriptor classifier = getBuiltInsScope().getClassifier(simpleName);
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
        return getBuiltInClassByName(type.getTypeName().getName());
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
        return getBuiltInClassByName(type.getArrayTypeName().getName());
    }

    @NotNull
    public ClassDescriptor getNumber() {
        return getBuiltInClassByName("Number");
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
    public ClassDescriptor getDataClassAnnotation() {
        return getBuiltInClassByName("data");
    }

    @NotNull
    public ClassDescriptor getVolatileAnnotationClass() {
        return getBuiltInClassByName("volatile");
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
        List<TypeProjection> types = Collections.singletonList(new TypeProjection(projectionType, argument));
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
        List<TypeProjection> types = Collections.singletonList(new TypeProjection(projectionType, argument));
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
    public JetType getFunctionType(
            @NotNull List<AnnotationDescriptor> annotations,
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
        int size = parameterTypes.size();
        ClassDescriptor classDescriptor = receiverType == null ? getFunction(size) : getExtensionFunction(size);
        TypeConstructor constructor = classDescriptor.getTypeConstructor();
        return new JetTypeImpl(annotations, constructor, false, arguments, classDescriptor.getMemberScope(arguments));
    }

    private static TypeProjection defaultProjection(JetType returnType) {
        return new TypeProjection(Variance.INVARIANT, returnType);
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

    // Functions

    public int getFunctionTraitCount() {
        return 23;
    }

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
        return setContainsClassOf(functionClassesSet, type);
    }

    public boolean isExtensionFunctionType(@NotNull JetType type) {
        return setContainsClassOf(extensionFunctionClassesSet, type);
    }

    @Nullable
    public JetType getReceiverType(@NotNull JetType type) {
        assert isFunctionOrExtensionFunctionType(type) : type;
        if (setContainsClassOf(extensionFunctionClassesSet, type)) {
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
                    Name.identifier("p" + (i + 1)), false, parameterType.getType(), false, null);
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
        int first = setContainsClassOf(extensionFunctionClassesSet, type) ? 1 : 0;
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

    public boolean isAny(@NotNull JetType type) {
        return !(type instanceof NamespaceType) &&
               type.getConstructor() == getAny().getTypeConstructor();
    }

    public boolean isUnit(@NotNull JetType type) {
        return !(type instanceof NamespaceType) &&
               type.getConstructor() == getUnitType().getConstructor();
    }

    public boolean isData(@NotNull ClassDescriptor classDescriptor) {
        return containsAnnotation(classDescriptor, getDataClassAnnotation());
    }

    public boolean isDeprecated(@NotNull DeclarationDescriptor declarationDescriptor) {
        return containsAnnotation(declarationDescriptor, getDeprecatedAnnotation());
    }

    private static boolean containsAnnotation(DeclarationDescriptor descriptor, ClassDescriptor annotationClass) {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TUPLES

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Deprecated
    @NotNull
    public List<JetType> getTupleElementTypes(@NotNull JetType type) {
        assert isTupleType(type);
        List<JetType> result = Lists.newArrayList();
        for (TypeProjection typeProjection : type.getArguments()) {
            result.add(typeProjection.getType());
        }
        return result;
    }

    @NotNull
    @Deprecated
    public ClassDescriptor getTuple(int size) {
        return getBuiltInClassByName("Tuple" + size);
    }

    @Deprecated
    public boolean isTupleType(@NotNull JetType type) {
        return setContainsClassOf(tupleClassesSet, type);
    }

    @NotNull
    @Deprecated
    public JetType getTupleType(@NotNull List<JetType> arguments) {
        return getTupleType(Collections.<AnnotationDescriptor>emptyList(), arguments);
    }

    @NotNull
    @Deprecated
    public JetType getTupleType(@NotNull JetType... arguments) {
        return getTupleType(Collections.<AnnotationDescriptor>emptyList(), Arrays.asList(arguments));
    }

    @Deprecated
    private JetType getTupleType(List<AnnotationDescriptor> annotations, List<JetType> arguments) {
        if (annotations.isEmpty() && arguments.isEmpty()) {
            return getUnitType();
        }
        ClassDescriptor tuple = getTuple(arguments.size());
        List<TypeProjection> typeArguments = toProjections(arguments);
        return new JetTypeImpl(annotations, tuple.getTypeConstructor(), false, typeArguments, tuple.getMemberScope(typeArguments));
    }

    private static List<TypeProjection> toProjections(List<JetType> arguments) {
        List<TypeProjection> result = new ArrayList<TypeProjection>();
        for (JetType argument : arguments) {
            result.add(new TypeProjection(Variance.OUT_VARIANCE, argument));
        }
        return result;
    }

    private static boolean setContainsClassOf(ImmutableSet<ClassDescriptor> set, JetType type) {
        //noinspection SuspiciousMethodCalls
        return set.contains(type.getConstructor().getDeclarationDescriptor());
    }
}
