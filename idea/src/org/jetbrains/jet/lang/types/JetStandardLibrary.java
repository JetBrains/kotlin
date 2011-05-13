package org.jetbrains.jet.lang.types;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetFileType;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.Annotation;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class JetStandardLibrary {

    // TODO : consider releasing this memory
    private static final Map<Project, JetStandardLibrary> standardLibraryCache = new HashMap<Project, JetStandardLibrary>();
    public static JetStandardLibrary getJetStandardLibrary(@NotNull Project project) {
        JetStandardLibrary standardLibrary = standardLibraryCache.get(project);
        if (standardLibrary == null) {
            standardLibrary = new JetStandardLibrary(project);
            standardLibraryCache.put(project, standardLibrary);
        }
        return standardLibrary;
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
    private final JetType nullableStringType;

    private JetStandardLibrary(@NotNull Project project) {
        // TODO : review
        InputStream stream = JetStandardClasses.class.getClassLoader().getResourceAsStream("jet/Library.jet");
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            JetFile file = (JetFile) PsiFileFactory.getInstance(project).createFileFromText("Library.jet",
                    JetFileType.INSTANCE, FileUtil.loadTextAndClose(new InputStreamReader(stream)));

            JetSemanticServices bootstrappingSemanticServices = JetSemanticServices.createSemanticServices(this);
            BindingTraceContext bindingTraceContext = new BindingTraceContext();
            TopDownAnalyzer bootstrappingTDA = new TopDownAnalyzer(bootstrappingSemanticServices, bindingTraceContext);
            WritableScopeImpl writableScope = new WritableScopeImpl(JetStandardClasses.STANDARD_CLASSES, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, ErrorHandler.THROW_EXCEPTION, null);
//            this.libraryScope = bootstrappingTDA.process(JetStandardClasses.STANDARD_CLASSES, file.getRootNamespace().getDeclarations());
            bootstrappingTDA.process(writableScope, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, file.getRootNamespace().getDeclarations());
            this.libraryScope = JetStandardClasses.STANDARD_CLASSES_NAMESPACE.getMemberScope();
            AnalyzingUtils.applyHandler(ErrorHandler.THROW_EXCEPTION, bindingTraceContext);

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
            this.typeInfoClass = (ClassDescriptor) libraryScope.getClassifier("TypeInfo");

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

    public ClassDescriptor getTypeInfo() {
        return typeInfoClass;
    }

    @NotNull
    public JetType getTypeInfoType(@NotNull JetType type) {
        TypeProjection typeProjection = new TypeProjection(type);
        List<TypeProjection> arguments = Collections.singletonList(typeProjection);
        return new JetTypeImpl(Collections.<Annotation>emptyList(), getTypeInfo().getTypeConstructor(), false, arguments, getTypeInfo().getMemberScope(arguments));
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
                Collections.<Annotation>emptyList(),
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
                Collections.<Annotation>emptyList(),
                getIterable().getTypeConstructor(),
                false,
                types,
                getIterable().getMemberScope(types)
        );
    }

    public JetType getNullableStringType() {
        return nullableStringType;
    }
}
