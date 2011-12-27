package org.jetbrains.jet.lang.types;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class JetStandardLibrary {

    // TODO : consider releasing this memory
    private static JetStandardLibrary cachedLibrary = null;
    //    private static final Map<Project, JetStandardLibrary> standardLibraryCache = new HashMap<Project, JetStandardLibrary>();

    // TODO : double checked locking
    synchronized
    public static JetStandardLibrary getJetStandardLibrary(@NotNull Project project) {
        if (cachedLibrary == null) {
            cachedLibrary = new JetStandardLibrary(project);
        }
        return cachedLibrary;
//        JetStandardLibrary standardLibrary = standardLibraryCache.get(project);
//        if (standardLibrary == null) {
//            standardLibrary = new JetStandardLibrary(project);
//            standardLibraryCache.put(project, standardLibrary);
//        }
//        return standardLibrary;
    }

    private JetScope libraryScope;

    private ClassDescriptor numberClass;
    private ClassDescriptor byteClass;
    private ClassDescriptor charClass;
    private ClassDescriptor shortClass;
    private ClassDescriptor intClass;
    private ClassDescriptor longClass;
    private ClassDescriptor floatClass;
    private ClassDescriptor doubleClass;
    private ClassDescriptor booleanClass;

    private ClassDescriptor stringClass;
    private ClassDescriptor arrayClass;
    private ClassDescriptor iterableClass;
    private ClassDescriptor typeInfoClass;

    private JetType byteType;
    private JetType charType;
    private JetType shortType;
    private JetType intType;
    private JetType longType;
    private JetType floatType;
    private JetType doubleType;
    private JetType booleanType;

    private JetType stringType;

    private JetType nullableByteType;
    private JetType nullableCharType;
    private JetType nullableShortType;
    private JetType nullableIntType;
    private JetType nullableLongType;
    private JetType nullableFloatType;
    private JetType nullableDoubleType;
    private JetType nullableBooleanType;
    private JetType nullableTuple0Type;

    private ClassDescriptor byteArrayClass;
    private ClassDescriptor charArrayClass;
    private ClassDescriptor shortArrayClass;
    private ClassDescriptor intArrayClass;
    private ClassDescriptor longArrayClass;
    private ClassDescriptor floatArrayClass;
    private ClassDescriptor doubleArrayClass;
    private ClassDescriptor booleanArrayClass;

    private JetType byteArrayType;
    private JetType charArrayType;
    private JetType shortArrayType;
    private JetType intArrayType;
    private JetType longArrayType;
    private JetType floatArrayType;
    private JetType doubleArrayType;
    private JetType booleanArrayType;

    private JetType nullableByteArrayType;
    private JetType nullableCharArrayType;
    private JetType nullableShortArrayType;
    private JetType nullableIntArrayType;
    private JetType nullableLongArrayType;
    private JetType nullableFloatArrayType;
    private JetType nullableDoubleArrayType;
    private JetType nullableBooleanArrayType;

    public JetType getTuple0Type() {
        return tuple0Type;
    }

    private JetType tuple0Type;
    private JetType nullableStringType;

    private Set<FunctionDescriptor> typeInfoFunction;

    private JetStandardLibrary(@NotNull Project project) {
        // TODO : review
        InputStream stream = JetStandardClasses.class.getClassLoader().getResourceAsStream("jet/Library.jet");
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            JetFile file = (JetFile) PsiFileFactory.getInstance(project).createFileFromText("Library.jet",
                    JetFileType.INSTANCE, FileUtil.loadTextAndClose(new InputStreamReader(stream)));

            JetSemanticServices bootstrappingSemanticServices = JetSemanticServices.createSemanticServices(this);
            BindingTraceContext bindingTraceContext = new BindingTraceContext();
            WritableScopeImpl writableScope = new WritableScopeImpl(JetStandardClasses.STANDARD_CLASSES, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, RedeclarationHandler.THROW_EXCEPTION).setDebugName("Root bootstrap scope");
            writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);
//            this.libraryScope = bootstrappingTDA.process(JetStandardClasses.STANDARD_CLASSES, file.getRootNamespace().getDeclarations());
//            bootstrappingTDA.process(writableScope, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, file.getRootNamespace().getDeclarations());
            TopDownAnalyzer.processStandardLibraryNamespace(bootstrappingSemanticServices, bindingTraceContext, writableScope, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, file.getRootNamespace());
//            this.libraryScope = JetStandardClasses.STANDARD_CLASSES_NAMESPACE.getMemberScope();

            AnalyzingUtils.throwExceptionOnErrors(bindingTraceContext.getBindingContext());
            initStdClasses();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public JetScope getLibraryScope() {
        initStdClasses();
        return libraryScope;
    }

    private void initStdClasses() {
        if(libraryScope == null) {
            this.libraryScope = JetStandardClasses.STANDARD_CLASSES_NAMESPACE.getMemberScope();
            this.numberClass = (ClassDescriptor) libraryScope.getClassifier("Number");
            this.byteClass = (ClassDescriptor) libraryScope.getClassifier("Byte");
            this.charClass = (ClassDescriptor) libraryScope.getClassifier("Char");
            this.shortClass = (ClassDescriptor) libraryScope.getClassifier("Short");
            this.intClass = (ClassDescriptor) libraryScope.getClassifier("Int");
            this.longClass = (ClassDescriptor) libraryScope.getClassifier("Long");
            this.floatClass = (ClassDescriptor) libraryScope.getClassifier("Float");
            this.doubleClass = (ClassDescriptor) libraryScope.getClassifier("Double");
            this.booleanClass = (ClassDescriptor) libraryScope.getClassifier("Boolean");
            this.stringClass = (ClassDescriptor) libraryScope.getClassifier("String");
            this.arrayClass = (ClassDescriptor) libraryScope.getClassifier("Array");

            this.iterableClass = (ClassDescriptor) libraryScope.getClassifier("Iterable");
//            typeInfoNamespace = libraryScope.getNamespace("typeinfo");
            this.typeInfoClass = (ClassDescriptor) libraryScope.getClassifier("TypeInfo");
            this.typeInfoFunction = libraryScope.getFunctions("typeinfo");

            this.byteType = new JetTypeImpl(getByte());
            this.charType = new JetTypeImpl(getChar());
            this.shortType = new JetTypeImpl(getShort());
            this.intType = new JetTypeImpl(getInt());
            this.longType = new JetTypeImpl(getLong());
            this.floatType = new JetTypeImpl(getFloat());
            this.doubleType = new JetTypeImpl(getDouble());
            this.booleanType = new JetTypeImpl(getBoolean());

            this.stringType = new JetTypeImpl(getString());
            this.nullableStringType = TypeUtils.makeNullable(stringType);

            this.tuple0Type = new JetTypeImpl(JetStandardClasses.getTuple(0));
            this.nullableTuple0Type = TypeUtils.makeNullable(tuple0Type);

            this.nullableByteType = TypeUtils.makeNullable(byteType);
            this.nullableCharType = TypeUtils.makeNullable(charType);
            this.nullableShortType = TypeUtils.makeNullable(shortType);
            this.nullableIntType = TypeUtils.makeNullable(intType);
            this.nullableLongType = TypeUtils.makeNullable(longType);
            this.nullableFloatType = TypeUtils.makeNullable(floatType);
            this.nullableDoubleType = TypeUtils.makeNullable(doubleType);
            this.nullableBooleanType = TypeUtils.makeNullable(booleanType);

            this.byteArrayClass = (ClassDescriptor) libraryScope.getClassifier("ByteArray");
            this.charArrayClass = (ClassDescriptor) libraryScope.getClassifier("CharArray");
            this.shortArrayClass = (ClassDescriptor) libraryScope.getClassifier("ShortArray");
            this.intArrayClass = (ClassDescriptor) libraryScope.getClassifier("IntArray");
            this.longArrayClass = (ClassDescriptor) libraryScope.getClassifier("LongArray");
            this.floatArrayClass = (ClassDescriptor) libraryScope.getClassifier("FloatArray");
            this.doubleArrayClass = (ClassDescriptor) libraryScope.getClassifier("DoubleArray");
            this.booleanArrayClass = (ClassDescriptor) libraryScope.getClassifier("BooleanArray");

            this.byteArrayType = new JetTypeImpl(byteArrayClass);
            this.charArrayType = new JetTypeImpl(charArrayClass);
            this.shortArrayType = new JetTypeImpl(shortArrayClass);
            this.intArrayType = new JetTypeImpl(intArrayClass);
            this.longArrayType = new JetTypeImpl(longArrayClass);
            this.floatArrayType = new JetTypeImpl(floatArrayClass);
            this.doubleArrayType = new JetTypeImpl(doubleArrayClass);
            this.booleanArrayType = new JetTypeImpl(booleanArrayClass);

            this.nullableByteArrayType = TypeUtils.makeNullable(byteArrayType);
            this.nullableCharArrayType = TypeUtils.makeNullable(charArrayType);
            this.nullableShortArrayType = TypeUtils.makeNullable(shortArrayType);
            this.nullableIntArrayType = TypeUtils.makeNullable(intArrayType);
            this.nullableLongArrayType = TypeUtils.makeNullable(longArrayType);
            this.nullableFloatArrayType = TypeUtils.makeNullable(floatArrayType);
            this.nullableDoubleArrayType = TypeUtils.makeNullable(doubleArrayType);
            this.nullableBooleanArrayType = TypeUtils.makeNullable(booleanArrayType);
        }
    }

    @NotNull
    public ClassDescriptor getNumber() {
        initStdClasses();
        return numberClass;
    }

    @NotNull
    public ClassDescriptor getByte() {
        initStdClasses();
        return byteClass;
    }

    @NotNull
    public ClassDescriptor getChar() {
        initStdClasses();
        return charClass;
    }

    @NotNull
    public ClassDescriptor getShort() {
        initStdClasses();
        return shortClass;
    }

    @NotNull
    public ClassDescriptor getInt() {
        initStdClasses();
        return intClass;
    }

    @NotNull
    public ClassDescriptor getLong() {
        initStdClasses();
        return longClass;
    }

    @NotNull
    public ClassDescriptor getFloat() {
        initStdClasses();
        return floatClass;
    }

    @NotNull
    public ClassDescriptor getDouble() {
        initStdClasses();
        return doubleClass;
    }

    @NotNull
    public ClassDescriptor getBoolean() {
        initStdClasses();
        return booleanClass;
    }

    @NotNull
    public ClassDescriptor getString() {
        initStdClasses();
        return stringClass;
    }

    @NotNull
    public ClassDescriptor  getArray() {
        initStdClasses();
        return arrayClass;
    }

    @NotNull
    public ClassDescriptor getIterable() {
        initStdClasses();
        return iterableClass;
    }

//    public NamespaceDescriptor getTypeInfoNamespace() {
//        initStdClasses();
//        return typeInfoNamespace;
//    }
//
    public ClassDescriptor getTypeInfo() {
        initStdClasses();
        return typeInfoClass;
    }

    public Set<FunctionDescriptor> getTypeInfoFunctions() {
        initStdClasses();
        return typeInfoFunction;
    }

    @NotNull
    public JetType getTypeInfoType(@NotNull JetType type) {
        TypeProjection typeProjection = new TypeProjection(type);
        List<TypeProjection> arguments = Collections.singletonList(typeProjection);
        return new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), getTypeInfo().getTypeConstructor(), false, arguments, getTypeInfo().getMemberScope(arguments));
    }

    @NotNull
    public JetType getIntType() {
        initStdClasses();
        return intType;
    }

    @NotNull
    public JetType getLongType() {
        initStdClasses();
        return longType;
    }

    @NotNull
    public JetType getDoubleType() {
        initStdClasses();
        return doubleType;
    }

    @NotNull
    public JetType getFloatType() {
        initStdClasses();
        return floatType;
    }

    @NotNull
    public JetType getCharType() {
        initStdClasses();
        return charType;
    }

    @NotNull
    public JetType getBooleanType() {
        initStdClasses();
        return booleanType;
    }

    @NotNull
    public JetType getStringType() {
        initStdClasses();
        return stringType;
    }

    @NotNull
    public JetType getByteType() {
        initStdClasses();
        return byteType;
    }

    @NotNull
    public JetType getShortType() {
        initStdClasses();
        return shortType;
    }

    @NotNull
    public JetType getArrayType(@NotNull JetType argument) {
        return getArrayType(Variance.INVARIANT, argument);
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
    public JetType getIterableType(@NotNull JetType argument) {
        List<TypeProjection> types = Collections.singletonList(new TypeProjection(Variance.INVARIANT, argument));
        return new JetTypeImpl(
                Collections.<AnnotationDescriptor>emptyList(),
                getIterable().getTypeConstructor(),
                false,
                types,
                getIterable().getMemberScope(types)
        );
    }

    public JetType getNullableStringType() {
        initStdClasses();
        return nullableStringType;
    }

    public JetType getNullableByteType() {
        initStdClasses();
        return nullableByteType;
    }

    public JetType getNullableCharType() {
        initStdClasses();
        return nullableCharType;
    }

    public JetType getNullableShortType() {
        initStdClasses();
        return nullableShortType;
    }

    public JetType getNullableIntType() {
        initStdClasses();
        return nullableIntType;
    }

    public JetType getNullableLongType() {
        initStdClasses();
        return nullableLongType;
    }

    public JetType getNullableFloatType() {
        initStdClasses();
        return nullableFloatType;
    }

    public JetType getNullableDoubleType() {
        initStdClasses();
        return nullableDoubleType;
    }

    public JetType getNullableBooleanType() {
        initStdClasses();
        return nullableBooleanType;
    }

    public JetType getNullableTuple0Type() {
        initStdClasses();
        return nullableTuple0Type;
    }

    public JetType getBooleanArrayType() {
        initStdClasses();
        return booleanArrayType;
    }

    public JetType getByteArrayType() {
        initStdClasses();
        return byteArrayType;
    }

    public JetType getCharArrayType() {
        initStdClasses();
        return charArrayType;
    }

    public JetType getShortArrayType() {
        initStdClasses();
        return shortArrayType;
    }

    public JetType getIntArrayType() {
        initStdClasses();
        return intArrayType;
    }

    public JetType getLongArrayType() {
        initStdClasses();
        return longArrayType;
    }

    public JetType getFloatArrayType() {
        initStdClasses();
        return floatArrayType;
    }

    public JetType getDoubleArrayType() {
        initStdClasses();
        return doubleArrayType;
    }

    public ClassDescriptor getByteArrayClass() {
        initStdClasses();
        return byteArrayClass;
    }

    public ClassDescriptor getCharArrayClass() {
        initStdClasses();
        return charArrayClass;
    }

    public ClassDescriptor getShortArrayClass() {
        initStdClasses();
        return shortArrayClass;
    }

    public ClassDescriptor getIntArrayClass() {
        initStdClasses();
        return intArrayClass;
    }

    public ClassDescriptor getLongArrayClass() {
        initStdClasses();
        return longArrayClass;
    }

    public ClassDescriptor getFloatArrayClass() {
        initStdClasses();
        return floatArrayClass;
    }

    public ClassDescriptor getDoubleArrayClass() {
        initStdClasses();
        return doubleArrayClass;
    }

    public ClassDescriptor getBooleanArrayClass() {
        initStdClasses();
        return booleanArrayClass;
    }

    public JetType getNullableByteArrayType() {
        return nullableByteArrayType;
    }

    public JetType getNullableCharArrayType() {
        return nullableCharArrayType;
    }

    public JetType getNullableShortArrayType() {
        return nullableShortArrayType;
    }

    public JetType getNullableIntArrayType() {
        return nullableIntArrayType;
    }

    public JetType getNullableLongArrayType() {
        return nullableLongArrayType;
    }

    public JetType getNullableFloatArrayType() {
        return nullableFloatArrayType;
    }

    public JetType getNullableDoubleArrayType() {
        return nullableDoubleArrayType;
    }

    public JetType getNullableBooleanArrayType() {
        return nullableBooleanArrayType;
    }
}
