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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.Variance;
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
    private ClassDescriptor comparableClass;
    private ClassDescriptor volatileClass;
    private ClassDescriptor throwableClass;

    private JetType numberType;
    private JetType stringType;
    private JetType volatileType;
    private JetType nullableStringType;
    private JetType charSequenceType;
    private JetType nullableCharSequenceType;
    private JetType nullableTuple0Type;
    private JetType throwableType;
    private JetType nullableThrowableType;

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
            WritableScopeImpl writableScope = new WritableScopeImpl(JetStandardClasses.STANDARD_CLASSES, JetStandardClasses.STANDARD_CLASSES_NAMESPACE, RedeclarationHandler.THROW_EXCEPTION).setDebugName("Root bootstrap scope");
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
        if(libraryScope == null) {
            this.libraryScope = JetStandardClasses.STANDARD_CLASSES_NAMESPACE.getMemberScope();

            this.numberClass = (ClassDescriptor) libraryScope.getClassifier("Number");
            this.stringClass = (ClassDescriptor) libraryScope.getClassifier("String");
            this.charSequenceClass = (ClassDescriptor) libraryScope.getClassifier("CharSequence");
            this.arrayClass = (ClassDescriptor) libraryScope.getClassifier("Array");
            this.volatileClass = (ClassDescriptor) libraryScope.getClassifier("volatile");
            this.throwableClass = (ClassDescriptor) libraryScope.getClassifier("Throwable");            

            this.iterableClass = (ClassDescriptor) libraryScope.getClassifier("Iterable");
            this.comparableClass = (ClassDescriptor) libraryScope.getClassifier("Comparable");
            this.typeInfoFunction = libraryScope.getFunctions("typeinfo");

            this.numberType = new JetTypeImpl(getNumber());
            this.stringType = new JetTypeImpl(getString());
            this.charSequenceType = new JetTypeImpl(getCharSequence());
            this.nullableCharSequenceType = TypeUtils.makeNullable(charSequenceType);
            this.nullableStringType = TypeUtils.makeNullable(stringType);
            this.volatileType = new JetTypeImpl(getVolatile());
            this.throwableType = new JetTypeImpl(getThrowable());
            this.nullableThrowableType = TypeUtils.makeNullable(throwableType);

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

    public Collection<ClassDescriptor> getStandardTypes() {
        initStdClasses();

        Collection<ClassDescriptor> classDescriptors = new ArrayList<ClassDescriptor>(primitiveTypeToClass.values());
        classDescriptors.add(numberClass);
        classDescriptors.add(stringClass);
        classDescriptors.add(charSequenceClass);
        classDescriptors.add(arrayClass);
        classDescriptors.add(volatileClass);
        classDescriptors.add(throwableClass);
        classDescriptors.add(iterableClass);
        classDescriptors.add(comparableClass);

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
    public ClassDescriptor getComparable() {
        initStdClasses();
        return comparableClass;
    }

    @NotNull
    public ClassDescriptor getThrowable() {
        initStdClasses();
        return throwableClass;
    }
    
    public Set<FunctionDescriptor> getTypeInfoFunctions() {
        initStdClasses();
        return typeInfoFunction;
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
    public JetType getThrowableType() {
        initStdClasses();
        return throwableType;
    }

    public JetType getNullableThrowableType() {
        initStdClasses();
        return nullableThrowableType;
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

    public JetType getTuple0Type() {
        return tuple0Type;
    }

    public JetType getNumberType() {
        return numberType;
    }
}
