package org.jetbrains.jet.lang.types;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
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

    private final JetScope libraryScope;

    private final ClassDescriptor byteClass;
    private final ClassDescriptor charClass;
    private final ClassDescriptor shortClass;
    private final ClassDescriptor intClass;
    private final ClassDescriptor longClass;
    private final ClassDescriptor floatClass;
    private final ClassDescriptor doubleClass;
    private final ClassDescriptor booleanClass;
    private final ClassDescriptor stringClass;
    private final ClassDescriptor arrayClass;
    private final ClassDescriptor iterableClass;
    private final ClassDescriptor typeInfoClass;

    private final JetType byteType;
    private final JetType charType;
    private final JetType shortType;
    private final JetType intType;
    private final JetType longType;
    private final JetType floatType;
    private final JetType doubleType;
    private final JetType booleanType;
    private final JetType stringType;

    private final JetType nullableByteType;
    private final JetType nullableCharType;
    private final JetType nullableShortType;
    private final JetType nullableIntType;
    private final JetType nullableLongType;
    private final JetType nullableFloatType;
    private final JetType nullableDoubleType;
    private final JetType nullableBooleanType;
    private final JetType nullableTuple0Type;

    private final ClassDescriptor byteArrayClass;
    private final ClassDescriptor charArrayClass;
    private final ClassDescriptor shortArrayClass;
    private final ClassDescriptor intArrayClass;
    private final ClassDescriptor longArrayClass;
    private final ClassDescriptor floatArrayClass;
    private final ClassDescriptor doubleArrayClass;
    private final ClassDescriptor booleanArrayClass;

    private final JetType byteArrayType;
    private final JetType charArrayType;
    private final JetType shortArrayType;
    private final JetType intArrayType;
    private final JetType longArrayType;
    private final JetType floatArrayType;
    private final JetType doubleArrayType;
    private final JetType booleanArrayType;

    public JetType getTuple0Type() {
        return tuple0Type;
    }

    private final JetType tuple0Type;
    private final JetType nullableStringType;

    private final NamespaceDescriptor typeInfoNamespace;
    private final Set<FunctionDescriptor> typeInfoFunction;

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
//            this.libraryScope = bootstrappingTDA.process(JetStandardClasses.STANDARD_CLASSES, file.getRootNamespace().getDeclarations());
//            bootstrappingTDA.process(writableScope, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, file.getRootNamespace().getDeclarations());
            TopDownAnalyzer.processStandardLibraryNamespace(bootstrappingSemanticServices, bindingTraceContext, writableScope, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, file.getRootNamespace());
            this.libraryScope = JetStandardClasses.STANDARD_CLASSES_NAMESPACE.getMemberScope();

            AnalyzingUtils.throwExceptionOnErrors(bindingTraceContext.getBindingContext());

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
            typeInfoNamespace = libraryScope.getNamespace("typeinfo");
            this.typeInfoClass = (ClassDescriptor) typeInfoNamespace.getMemberScope().getClassifier("TypeInfo");
            typeInfoFunction = typeInfoNamespace.getMemberScope().getFunctions("typeinfo");

            this.byteType = new JetTypeImpl(getByte());
            this.charType = new JetTypeImpl(getChar());
            this.shortType = new JetTypeImpl(getShort());
            this.intType = new JetTypeImpl(getInt());
            this.longType = new JetTypeImpl(getLong());
            this.floatType = new JetTypeImpl(getFloat());
            this.doubleType = new JetTypeImpl(getDouble());
            this.booleanType = new JetTypeImpl(getBoolean());
            this.stringType = new JetTypeImpl(getString());
            this.tuple0Type = new JetTypeImpl(JetStandardClasses.getTuple(0));

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

            this.nullableByteType = TypeUtils.makeNullable(byteType);
            this.nullableCharType = TypeUtils.makeNullable(charType);
            this.nullableShortType = TypeUtils.makeNullable(shortType);
            this.nullableIntType = TypeUtils.makeNullable(intType);
            this.nullableLongType = TypeUtils.makeNullable(longType);
            this.nullableFloatType = TypeUtils.makeNullable(floatType);
            this.nullableDoubleType = TypeUtils.makeNullable(doubleType);
            this.nullableBooleanType = TypeUtils.makeNullable(booleanType);
            this.nullableStringType = TypeUtils.makeNullable(stringType);
            this.nullableTuple0Type = TypeUtils.makeNullable(tuple0Type);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public JetScope getLibraryScope() {
        return libraryScope;
    }

    @NotNull
    public ClassDescriptor getByte() {
        return byteClass;
    }

    @NotNull
    public ClassDescriptor getChar() {
        return charClass;
    }

    @NotNull
    public ClassDescriptor getShort() {
        return shortClass;
    }

    @NotNull
    public ClassDescriptor getInt() {
        return intClass;
    }

    @NotNull
    public ClassDescriptor getLong() {
        return longClass;
    }

    @NotNull
    public ClassDescriptor getFloat() {
        return floatClass;
    }

    @NotNull
    public ClassDescriptor getDouble() {
        return doubleClass;
    }

    @NotNull
    public ClassDescriptor getBoolean() {
        return booleanClass;
    }

    @NotNull
    public ClassDescriptor getString() {
        return stringClass;
    }

    @NotNull
    public ClassDescriptor getArray() {
        return arrayClass;
    }

    @NotNull
    public ClassDescriptor getIterable() {
        return iterableClass;
    }

    public NamespaceDescriptor getTypeInfoNamespace() {
        return typeInfoNamespace;
    }

    public ClassDescriptor getTypeInfo() {
        return typeInfoClass;
    }

    public Set<FunctionDescriptor> getTypeInfoFunctions() {
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
        return intType;
    }

    @NotNull
    public JetType getLongType() {
        return longType;
    }

    @NotNull
    public JetType getDoubleType() {
        return doubleType;
    }

    @NotNull
    public JetType getFloatType() {
        return floatType;
    }

    @NotNull
    public JetType getCharType() {
        return charType;
    }

    @NotNull
    public JetType getBooleanType() {
        return booleanType;
    }

    @NotNull
    public JetType getStringType() {
        return stringType;
    }

    @NotNull
    public JetType getByteType() {
        return byteType;
    }

    @NotNull
    public JetType getShortType() {
        return shortType;
    }

    @NotNull
    public JetType getArrayType(@NotNull JetType argument) {
        Variance variance = Variance.INVARIANT;
        return getArrayType(variance, argument);
    }

    @NotNull
    public JetType getArrayType(@NotNull Variance variance, @NotNull JetType argument) {
        List<TypeProjection> types = Collections.singletonList(new TypeProjection(variance, argument));
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
        return nullableStringType;
    }

    public JetType getNullableByteType() {
        return nullableByteType;
    }

    public JetType getNullableCharType() {
        return nullableCharType;
    }

    public JetType getNullableShortType() {
        return nullableShortType;
    }

    public JetType getNullableIntType() {
        return nullableIntType;
    }

    public JetType getNullableLongType() {
        return nullableLongType;
    }

    public JetType getNullableFloatType() {
        return nullableFloatType;
    }

    public JetType getNullableDoubleType() {
        return nullableDoubleType;
    }

    public JetType getNullableBooleanType() {
        return nullableBooleanType;
    }

    public JetType getNullableTuple0Type() {
        return nullableTuple0Type;
    }

    public JetType getBooleanArrayType() {
        return booleanArrayType;
    }

    public JetType getByteArrayType() {
        return byteArrayType;
    }

    public JetType getCharArrayType() {
        return charArrayType;
    }

    public JetType getShortArrayType() {
        return shortArrayType;
    }

    public JetType getIntArrayType() {
        return intArrayType;
    }

    public JetType getLongArrayType() {
        return longArrayType;
    }

    public JetType getFloatArrayType() {
        return floatArrayType;
    }

    public JetType getDoubleArrayType() {
        return doubleArrayType;
    }

    public ClassDescriptor getByteArrayClass() {
        return byteArrayClass;
    }

    public ClassDescriptor getCharArrayClass() {
        return charArrayClass;
    }

    public ClassDescriptor getShortArrayClass() {
        return shortArrayClass;
    }

    public ClassDescriptor getIntArrayClass() {
        return intArrayClass;
    }

    public ClassDescriptor getLongArrayClass() {
        return longArrayClass;
    }

    public ClassDescriptor getFloatArrayClass() {
        return floatArrayClass;
    }

    public ClassDescriptor getDoubleArrayClass() {
        return doubleArrayClass;
    }

    public ClassDescriptor getBooleanArrayClass() {
        return booleanArrayClass;
    }
}
