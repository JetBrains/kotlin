package org.jetbrains.jet.lang.types;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
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
import java.util.*;

/**
 * @author abreslav
 */
public class JetStandardLibrary {

    // TODO : consider releasing this memory
    private static JetStandardLibrary cachedLibrary = null;
    // A temporary try to find a reason of KT-224
    private static int wasProcessCanceledException = 0;
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

    private final Project project;
    
    private JetScope libraryScope;

    private ClassDescriptor numberClass;

    private ClassDescriptor charSequenceClass;
    private ClassDescriptor stringClass;
    private ClassDescriptor arrayClass;
    private ClassDescriptor iterableClass;
    private ClassDescriptor typeInfoClass;
    private ClassDescriptor comparableClass;
    private ClassDescriptor volatileClass;

    private JetType stringType;
    private JetType volatileType;
    private JetType nullableStringType;
    private JetType charSequenceType;
    private JetType nullableCharSequenceType;

    private JetType nullableTuple0Type;

    public JetType getTuple0Type() {
        return tuple0Type;
    }

    private JetType tuple0Type;

    private Set<FunctionDescriptor> typeInfoFunction;

    private EnumMap<PrimitiveType, ClassDescriptor> primitiveTypeToClass;
    private EnumMap<PrimitiveType, ClassDescriptor> primitiveTypeToArrayClass;
    private EnumMap<PrimitiveType, JetType> primitiveTypeToJetType;
    private EnumMap<PrimitiveType, JetType> primitiveTypeToNullableJetType;
    private EnumMap<PrimitiveType, JetType> primitiveTypeToArrayJetType;
    private EnumMap<PrimitiveType, JetType> primitiveTypeToNullableArrayJetType;
    private Map<JetType, JetType> primitiveJetTypeToJetArrayType;
    private Map<JetType, JetType> jetArrayTypeToPrimitiveJetType;

    private JetStandardLibrary(@NotNull Project project) {
        this.project = project;

        // TODO : review
        List<String> libraryFiles = Arrays.asList(
                "Library.jet",
                "Numbers.jet",
                "Ranges.jet",
                "Iterables.jet",
                "Iterators.jet",
                "Arrays.jet"
        );
        try {
            List<JetFile> files = new LinkedList<JetFile>();
            for(String fileName : libraryFiles) {
                InputStream stream = JetStandardClasses.class.getClassLoader().getResourceAsStream("jet/" + fileName);

                //noinspection IOResourceOpenedButNotSafelyClosed
                JetFile file = (JetFile) PsiFileFactory.getInstance(project).createFileFromText(fileName,
                        JetFileType.INSTANCE, FileUtil.loadTextAndClose(new InputStreamReader(stream)));
                files.add(file);
            }

            JetSemanticServices bootstrappingSemanticServices = JetSemanticServices.createSemanticServices(this);
            BindingTraceContext bindingTraceContext = new BindingTraceContext();
            WritableScopeImpl writableScope = new WritableScopeImpl(JetStandardClasses.STANDARD_CLASSES, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, RedeclarationHandler.THROW_EXCEPTION).setDebugName("Root bootstrap scope");
            writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);
//            this.libraryScope = bootstrappingTDA.process(JetStandardClasses.STANDARD_CLASSES, file.getRootNamespace().getDeclarations());
//            bootstrappingTDA.process(writableScope, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, file.getRootNamespace().getDeclarations());
            TopDownAnalyzer.processStandardLibraryNamespace(bootstrappingSemanticServices, bindingTraceContext, writableScope, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, files);
//            this.libraryScope = JetStandardClasses.STANDARD_CLASSES_NAMESPACE.getMemberScope();

            AnalyzingUtils.throwExceptionOnErrors(bindingTraceContext.getBindingContext());
            initStdClasses();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ProcessCanceledException e) {
            wasProcessCanceledException++;
            throw e;
        }
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    public JetScope getLibraryScope() {
        initStdClasses();
        return libraryScope;
    }

    private void initStdClasses() {
        if(libraryScope == null) {
            this.libraryScope = JetStandardClasses.STANDARD_CLASSES_NAMESPACE.getMemberScope();
            this.numberClass = (ClassDescriptor) libraryScope.getClassifier("Number");
            this.stringClass = (ClassDescriptor) libraryScope.getClassifier("String");
            this.charSequenceClass = (ClassDescriptor) libraryScope.getClassifier("CharSequence");
            this.arrayClass = (ClassDescriptor) libraryScope.getClassifier("Array");
            this.volatileClass = (ClassDescriptor) libraryScope.getClassifier("volatile");

            this.iterableClass = (ClassDescriptor) libraryScope.getClassifier("Iterable");
            this.comparableClass = (ClassDescriptor) libraryScope.getClassifier("Comparable");
//            typeInfoNamespace = libraryScope.getNamespace("typeinfo");
            this.typeInfoClass = (ClassDescriptor) libraryScope.getClassifier("TypeInfo");
            this.typeInfoFunction = libraryScope.getFunctions("typeinfo");

            this.stringType = new JetTypeImpl(getString());
            this.charSequenceType = new JetTypeImpl(getCharSequence());
            this.nullableCharSequenceType = TypeUtils.makeNullable(charSequenceType);
            this.nullableStringType = TypeUtils.makeNullable(stringType);
            this.volatileType = new JetTypeImpl(getVolatile());

            this.tuple0Type = new JetTypeImpl(JetStandardClasses.getTuple(0));
            this.nullableTuple0Type = TypeUtils.makeNullable(tuple0Type);
            
            primitiveTypeToClass = new EnumMap<PrimitiveType, ClassDescriptor>(PrimitiveType.class);
            primitiveTypeToJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
            primitiveTypeToNullableJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
            primitiveTypeToArrayClass = new EnumMap<PrimitiveType, ClassDescriptor>(PrimitiveType.class);
            primitiveTypeToArrayJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
            primitiveTypeToNullableArrayJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
            primitiveJetTypeToJetArrayType = new HashMap<JetType, JetType>();
            jetArrayTypeToPrimitiveJetType = new HashMap<JetType, JetType>();

            for (PrimitiveType primitive : PrimitiveType.values()) {
                makePrimitive(primitive);
            }
        }
    }

    private void makePrimitive(PrimitiveType primitiveType) {
        ClassDescriptor clazz = (ClassDescriptor) libraryScope.getClassifier(primitiveType.getTypeName());
        ClassDescriptor arrayClazz = (ClassDescriptor) libraryScope.getClassifier(primitiveType.getTypeName() + "Array");
        JetTypeImpl type = new JetTypeImpl(clazz);
        JetTypeImpl arrayType = new JetTypeImpl(arrayClazz);

        primitiveTypeToClass.put(primitiveType, clazz);
        primitiveTypeToJetType.put(primitiveType, type);
        primitiveTypeToNullableJetType.put(primitiveType, TypeUtils.makeNullable(type));
        primitiveTypeToArrayClass.put(primitiveType, arrayClazz);
        primitiveTypeToArrayJetType.put(primitiveType, arrayType);
        primitiveTypeToNullableArrayJetType.put(primitiveType, TypeUtils.makeNullable(arrayType));
        primitiveJetTypeToJetArrayType.put(type, arrayType);
        jetArrayTypeToPrimitiveJetType.put(arrayType, type);
    }

    @NotNull
    public ClassDescriptor getNumber() {
        initStdClasses();
        return numberClass;
    }

    @NotNull
    public ClassDescriptor getPrimitiveClassDescriptor(PrimitiveType primitiveType) {
        initStdClasses();
        return primitiveTypeToClass.get(primitiveType);
    }

    @NotNull
    public ClassDescriptor getByte() {
        return getPrimitiveClassDescriptor(PrimitiveType.BYTE);
    }

    @NotNull
    public ClassDescriptor getChar() {
        return getPrimitiveClassDescriptor(PrimitiveType.CHAR);
    }

    @NotNull
    public ClassDescriptor getShort() {
        return getPrimitiveClassDescriptor(PrimitiveType.SHORT);
    }

    @NotNull
    public ClassDescriptor getInt() {
        return getPrimitiveClassDescriptor(PrimitiveType.INT);
    }

    @NotNull
    public ClassDescriptor getLong() {
        return getPrimitiveClassDescriptor(PrimitiveType.LONG);
    }

    @NotNull
    public ClassDescriptor getFloat() {
        return getPrimitiveClassDescriptor(PrimitiveType.FLOAT);
    }

    @NotNull
    public ClassDescriptor getDouble() {
        return getPrimitiveClassDescriptor(PrimitiveType.DOUBLE);
    }

    @NotNull
    public ClassDescriptor getBoolean() {
        return getPrimitiveClassDescriptor(PrimitiveType.BOOLEAN);
    }

    @NotNull
    public ClassDescriptor getString() {
        initStdClasses();
        return stringClass;
    }

    @NotNull
    public ClassDescriptor getCharSequence() {
        initStdClasses();
        return charSequenceClass;
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

    @NotNull
    public ClassDescriptor getComparable() {
        initStdClasses();
        return comparableClass;
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
    public JetType getPrimitiveJetType(PrimitiveType primitiveType) {
        return primitiveTypeToJetType.get(primitiveType);
    }

    @NotNull
    public JetType getIntType() {
        return getPrimitiveJetType(PrimitiveType.INT);
    }

    @NotNull
    public JetType getLongType() {
        return getPrimitiveJetType(PrimitiveType.LONG);
    }

    @NotNull
    public JetType getDoubleType() {
        return getPrimitiveJetType(PrimitiveType.DOUBLE);
    }

    @NotNull
    public JetType getFloatType() {
        return getPrimitiveJetType(PrimitiveType.FLOAT);
    }

    @NotNull
    public JetType getCharType() {
        return getPrimitiveJetType(PrimitiveType.CHAR);
    }

    @NotNull
    public JetType getBooleanType() {
        return getPrimitiveJetType(PrimitiveType.BOOLEAN);
    }

    @NotNull
    public JetType getStringType() {
        initStdClasses();
        return stringType;
    }

    @NotNull
    public JetType getCharSequenceType() {
        initStdClasses();
        return charSequenceType;
    }

    @NotNull
    public JetType getByteType() {
        return getPrimitiveJetType(PrimitiveType.BYTE);
    }

    @NotNull
    public JetType getShortType() {
        return getPrimitiveJetType(PrimitiveType.SHORT);
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
    public JetType getArrayElementType(@NotNull JetType arrayType) {
        // make non-null?
        if (arrayType.getConstructor().getDeclarationDescriptor() == getArray()) {
            if (arrayType.getArguments().size() != 1) {
                throw new IllegalStateException();
            }
            return arrayType.getArguments().get(0).getType();
        }
        JetType primitiveType = jetArrayTypeToPrimitiveJetType.get(arrayType);
        if (primitiveType == null) {
            throw new IllegalStateException("not array: " + arrayType);
        }
        return primitiveType;
    }

    @NotNull
    public JetType getIterableType(@NotNull JetType argument) {
        return getIterableType(Variance.INVARIANT, argument);
    }

    @NotNull
    public JetType getIterableType(@NotNull Variance projectionType, @NotNull JetType argument) {
        List<TypeProjection> types = Collections.singletonList(new TypeProjection(projectionType, argument));
        return new JetTypeImpl(
                Collections.<AnnotationDescriptor>emptyList(),
                getIterable().getTypeConstructor(),
                false,
                types,
                getIterable().getMemberScope(types)
        );
    }

    @NotNull
    public JetType getNullableStringType() {
        initStdClasses();
        return nullableStringType;
    }

    @NotNull
    public JetType getNullableCharSequenceType() {
        initStdClasses();
        return nullableCharSequenceType;
    }

    @NotNull
    public JetType getNullablePrimitiveJetType(PrimitiveType primitiveType) {
        initStdClasses();
        return primitiveTypeToNullableJetType.get(primitiveType);
    }

    public JetType getNullableTuple0Type() {
        initStdClasses();
        return nullableTuple0Type;
    }
    
    @NotNull
    public JetType getPrimitiveArrayJetType(PrimitiveType primitiveType) {
        initStdClasses();
        return primitiveTypeToArrayJetType.get(primitiveType);
    }

    /**
     * @return <code>null</code> if not primitive
     */
    @Nullable
    public JetType getPrimitiveArrayJetTypeByPrimitiveJetType(JetType jetType) {
        return primitiveJetTypeToJetArrayType.get(jetType);
    }

    @NotNull
    public ClassDescriptor getPrimitiveArrayClassDescriptor(PrimitiveType primitiveType) {
        initStdClasses();
        return primitiveTypeToArrayClass.get(primitiveType);
    }


    @NotNull
    public JetType getNullablePrimitiveArrayJetType(PrimitiveType primitiveType) {
        initStdClasses();
        return primitiveTypeToNullableArrayJetType.get(primitiveType);
    }

    public ClassDescriptor getVolatile() {
        return volatileClass;
    }

    public JetType getVolatileType() {
        return volatileType;
    }

    public final boolean isVolatile(PropertyDescriptor descriptor) {
        List<AnnotationDescriptor> annotations = descriptor.getOriginal().getAnnotations();
        if(annotations != null) {
            for(AnnotationDescriptor d: annotations) {
                if(d.getType().equals(getVolatileType()))
                    return true;
            }
        }
        return false;
    }
}
