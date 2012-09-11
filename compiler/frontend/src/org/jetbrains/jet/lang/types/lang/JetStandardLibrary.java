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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author abreslav
 */
public class JetStandardLibrary {

    private static JetStandardLibrary instance = null;

    private static boolean initializing;
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
                instance = new JetStandardLibrary(project);
            }
            catch (Throwable e) {
                initializationFailed = e;
                throw new RuntimeException("builtin library initialization failed: " + e, e);
            }
            initializing = false;
        }
    }

    @NotNull // This asserts that initialize() is called before any resolution happens
    public static JetStandardLibrary getInstance() {
        return instance;
    }

    private JetScope libraryScope;

    private ClassDescriptor numberClass;

    private ClassDescriptor charSequenceClass;
    private ClassDescriptor stringClass;
    private ClassDescriptor arrayClass;
    private ClassDescriptor iterableClass;
    private ClassDescriptor iteratorClass;
    private ClassDescriptor mutableIterableClass;
    private ClassDescriptor mutableIteratorClass;
    private ClassDescriptor comparableClass;
    private ClassDescriptor throwableClass;
    private ClassDescriptor enumClass;
    private ClassDescriptor annotationClass;
    private ClassDescriptor volatileClass;
    private ClassDescriptor dataClass;

    private JetType stringType;
    private JetType annotationType;
    private JetType tuple0Type;

    private EnumMap<PrimitiveType, ClassDescriptor> primitiveTypeToClass;
    private EnumMap<PrimitiveType, ClassDescriptor> primitiveTypeToArrayClass;
    private EnumMap<PrimitiveType, JetType> primitiveTypeToJetType;
    private EnumMap<PrimitiveType, JetType> primitiveTypeToNullableJetType;
    private EnumMap<PrimitiveType, JetType> primitiveTypeToArrayJetType;
    private Map<JetType, JetType> primitiveJetTypeToJetArrayType;
    private Map<JetType, JetType> jetArrayTypeToPrimitiveJetType;

    private JetStandardLibrary(@NotNull Project project) {
        // TODO : review
        List<String> libraryFiles = Arrays.asList(
                "Library.jet",
                "Numbers.jet",
                "Ranges.jet",
                "Iterables.jet",
                "Iterators.jet",
                "Arrays.jet",
                "Enum.jet",
                "Collections.jet"
        );
        try {
            List<JetFile> files = new LinkedList<JetFile>();
            for(String fileName : libraryFiles) {
                String path = "jet/" + fileName;
                InputStream stream = JetStandardClasses.class.getClassLoader().getResourceAsStream(path);
                
                if (stream == null) {
                    throw new IllegalStateException("resource not found in classpath: " + path);
                }

                //noinspection IOResourceOpenedButNotSafelyClosed
                JetFile file = (JetFile) PsiFileFactory.getInstance(project).createFileFromText(fileName,
                        JetFileType.INSTANCE, FileUtil.loadTextAndClose(new InputStreamReader(stream)));
                files.add(file);
            }

            BindingTraceContext bindingTraceContext = new BindingTraceContext();
            WritableScopeImpl writableScope = new WritableScopeImpl(
                    JetStandardClasses.STANDARD_CLASSES, JetStandardClasses.STANDARD_CLASSES_NAMESPACE,
                    RedeclarationHandler.THROW_EXCEPTION, "Root bootstrap scope");
            writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);
            TopDownAnalyzer.processStandardLibraryNamespace(project, bindingTraceContext, writableScope, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, files);

            AnalyzingUtils.throwExceptionOnErrors(bindingTraceContext.getBindingContext());
            initStdClasses();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ProcessCanceledException e) {
            throw e;
        }
    }

    public JetScope getLibraryScope() {
        initStdClasses();
        return libraryScope;
    }

    private void initStdClasses() {
        if (libraryScope == null) {
            this.libraryScope = JetStandardClasses.STANDARD_CLASSES_NAMESPACE.getMemberScope();

            this.numberClass = getStdClassByName("Number");
            this.stringClass = getStdClassByName("String");
            this.charSequenceClass = getStdClassByName("CharSequence");
            this.arrayClass = getStdClassByName("Array");
            this.throwableClass = getStdClassByName("Throwable");
            this.enumClass = getStdClassByName("Enum");

            this.volatileClass = getStdClassByName("volatile");
            this.dataClass = getStdClassByName("data");

            this.iterableClass = getStdClassByName("Iterable");
            this.iteratorClass = getStdClassByName("Iterator");
            this.mutableIterableClass = getStdClassByName("MutableIterable");
            this.mutableIteratorClass = getStdClassByName("MutableIterator");
            this.comparableClass = getStdClassByName("Comparable");

            this.stringType = new JetTypeImpl(getString());
            this.tuple0Type = new JetTypeImpl(JetStandardClasses.getTuple(0));

            this.annotationClass = getStdClassByName("Annotation");
            this.annotationType = new JetTypeImpl(annotationClass);

            primitiveTypeToClass = new EnumMap<PrimitiveType, ClassDescriptor>(PrimitiveType.class);
            primitiveTypeToJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
            primitiveTypeToNullableJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
            primitiveTypeToArrayClass = new EnumMap<PrimitiveType, ClassDescriptor>(PrimitiveType.class);
            primitiveTypeToArrayJetType = new EnumMap<PrimitiveType, JetType>(PrimitiveType.class);
            primitiveJetTypeToJetArrayType = new HashMap<JetType, JetType>();
            jetArrayTypeToPrimitiveJetType = new HashMap<JetType, JetType>();

            for (PrimitiveType primitive : PrimitiveType.values()) {
                makePrimitive(primitive);
            }
        }
    }

    @NotNull
    private ClassDescriptor getStdClassByName(String className) {
        ClassDescriptor classDescriptor = (ClassDescriptor) libraryScope.getClassifier(Name.identifier(className));
        assert classDescriptor != null : "Standard class not found: " + className;
        return classDescriptor;
    }

    private void makePrimitive(PrimitiveType primitiveType) {
        ClassDescriptor clazz = (ClassDescriptor) libraryScope.getClassifier(primitiveType.getTypeName());
        ClassDescriptor arrayClazz = (ClassDescriptor) libraryScope.getClassifier(primitiveType.getArrayTypeName());
        JetTypeImpl type = new JetTypeImpl(clazz);
        JetTypeImpl arrayType = new JetTypeImpl(arrayClazz);

        primitiveTypeToClass.put(primitiveType, clazz);
        primitiveTypeToJetType.put(primitiveType, type);
        primitiveTypeToNullableJetType.put(primitiveType, TypeUtils.makeNullable(type));
        primitiveTypeToArrayClass.put(primitiveType, arrayClazz);
        primitiveTypeToArrayJetType.put(primitiveType, arrayType);
        primitiveJetTypeToJetArrayType.put(type, arrayType);
        jetArrayTypeToPrimitiveJetType.put(arrayType, type);
    }

    public Set<DeclarationDescriptor> getIntegralRanges() {
        initStdClasses();

        return ImmutableSet.<DeclarationDescriptor>of(
                getStdClassByName("ByteRange"),
                getStdClassByName("ShortRange"),
                getStdClassByName("CharRange"),
                getStdClassByName("IntRange"),
                getStdClassByName("LongRange")
        );
    }

    public Collection<ClassDescriptor> getStandardTypes() {
        initStdClasses();

        Collection<ClassDescriptor> classDescriptors = new ArrayList<ClassDescriptor>(primitiveTypeToClass.values());
        classDescriptors.add(numberClass);
        classDescriptors.add(stringClass);
        classDescriptors.add(charSequenceClass);
        classDescriptors.add(arrayClass);
        classDescriptors.add(throwableClass);
        classDescriptors.add(iterableClass);
        classDescriptors.add(iteratorClass);
        classDescriptors.add(mutableIterableClass);
        classDescriptors.add(mutableIteratorClass);
        classDescriptors.add(comparableClass);
        classDescriptors.add(enumClass);

        return classDescriptors;
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
    public ClassDescriptor getPrimitiveArrayClassDescriptor(PrimitiveType primitiveType) {
        initStdClasses();
        return primitiveTypeToArrayClass.get(primitiveType);
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
    public ClassDescriptor getArray() {
        initStdClasses();
        return arrayClass;
    }

    @NotNull
    public ClassDescriptor getIterable() {
        initStdClasses();
        return iterableClass;
    }

    @NotNull
    public ClassDescriptor getIterator() {
        initStdClasses();
        return iteratorClass;
    }

    @NotNull
    public ClassDescriptor getMutableIterable() {
        initStdClasses();
        return mutableIterableClass;
    }

    @NotNull
    public ClassDescriptor getMutableIterator() {
        initStdClasses();
        return mutableIteratorClass;
    }

    @NotNull
    public ClassDescriptor getComparable() {
        initStdClasses();
        return comparableClass;
    }

    @NotNull
    public ClassDescriptor getThrowable() {
        initStdClasses();
        return throwableClass;
    }

    @NotNull
    public ClassDescriptor getEnum() {
        initStdClasses();
        return enumClass;
    }

    @NotNull
    public ClassDescriptor getAnnotation() {
        initStdClasses();
        return annotationClass;
    }

    @NotNull
    public JetType getAnnotationType() {
        initStdClasses();
        return annotationType;
    }

    @NotNull
    public ClassDescriptor getCollection() {
        return getStdClassByName("Collection");
    }

    @NotNull
    public ClassDescriptor getMutableCollection() {
        return getStdClassByName("MutableCollection");
    }

    @NotNull
    public ClassDescriptor getList() {
        return getStdClassByName("List");
    }

    @NotNull
    public ClassDescriptor getMutableList() {
        return getStdClassByName("MutableList");
    }

    @NotNull
    public ClassDescriptor getListIterator() {
        return getStdClassByName("ListIterator");
    }

    @NotNull
    public ClassDescriptor getMutableListIterator() {
        return getStdClassByName("MutableListIterator");
    }

    @NotNull
    public ClassDescriptor getSet() {
        return getStdClassByName("Set");
    }

    @NotNull
    public ClassDescriptor getMutableSet() {
        return getStdClassByName("MutableSet");
    }

    @NotNull
    public ClassDescriptor getMap() {
        return getStdClassByName("Map");
    }

    @NotNull
    public ClassDescriptor getMutableMap() {
        return getStdClassByName("MutableMap");
    }

    @NotNull
    public ClassDescriptor getMapEntry() {
        ClassifierDescriptor entry = DescriptorUtils.getInnerClassByName(getMap(), "Entry");
        assert entry instanceof ClassDescriptor;
        return (ClassDescriptor) entry;
    }

    @NotNull
    public ClassDescriptor getMutableMapEntry() {
        ClassifierDescriptor entry = DescriptorUtils.getInnerClassByName(getMutableMap(), "MutableEntry");
        assert entry instanceof ClassDescriptor;
        return (ClassDescriptor) entry;
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
    public JetType getEnumType(@NotNull JetType argument) {
        return getEnumType(Variance.INVARIANT, argument);
    }

    @NotNull
    public ClassDescriptor getDataClassAnnotation() {
        return dataClass;
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
    public JetType getEnumType(@NotNull Variance projectionType, @NotNull JetType argument) {
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
    public JetType getNullablePrimitiveJetType(PrimitiveType primitiveType) {
        initStdClasses();
        return primitiveTypeToNullableJetType.get(primitiveType);
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

    public boolean isVolatile(@NotNull PropertyDescriptor descriptor) {
        return containsAnnotation(descriptor, volatileClass);
    }

    public static boolean isData(@NotNull ClassDescriptor descriptor) {
        if (initializing) {
            // This is a hack to make this method callable while resolving standard library
            // (otherwise getInstance() would throw an Exception)
            // This also means that "data" annotation has no effect in standard library
            return false;
        }
        return containsAnnotation(descriptor, getInstance().dataClass);
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

    public boolean isArray(@NotNull JetType type) {
        return getArray().equals(type.getConstructor().getDeclarationDescriptor());
    }

    public JetType getTuple0Type() {
        return tuple0Type;
    }
}
