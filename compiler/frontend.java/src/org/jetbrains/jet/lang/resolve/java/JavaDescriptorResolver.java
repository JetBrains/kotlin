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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import gnu.trove.THashMap;
import gnu.trove.TObjectHashingStrategy;
import jet.typeinfo.TypeInfoVariance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.kt.JetMethodAnnotation;
import org.jetbrains.jet.lang.resolve.java.kt.PsiAnnotationWithFlags;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameBase;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.rt.signature.JetSignatureAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureExceptionsAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureReader;
import org.jetbrains.jet.rt.signature.JetSignatureVisitor;
import org.jetbrains.jet.utils.ExceptionUtils;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorResolver.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassObjectName;

/**
 * @author abreslav
 */
public class JavaDescriptorResolver implements DependencyClassByQualifiedNameResolver {

    private static final FqName OBJECT_FQ_NAME = new FqName("java.lang.Object");

    public static final Name JAVA_ROOT = Name.special("<java_root>");

    public static final ModuleDescriptor FAKE_ROOT_MODULE = new ModuleDescriptor(JAVA_ROOT);

    public static Visibility PACKAGE_VISIBILITY = new Visibility("package", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            NamespaceDescriptor parentPackage = DescriptorUtils.getParentOfType(what, NamespaceDescriptor.class);
            NamespaceDescriptor fromPackage = DescriptorUtils.getParentOfType(from, NamespaceDescriptor.class, false);
            assert parentPackage != null;
            return parentPackage.equals(fromPackage);
        }

        @Override
        protected Integer compareTo(@NotNull Visibility visibility) {
            if (this == visibility) return 0;
            if (visibility == Visibilities.PRIVATE) return 1;
            return -1;
        }
    };


    static abstract class ResolverScopeData {
        @Nullable
        final PsiClass psiClass;
        @Nullable
        final PsiPackage psiPackage;
        @Nullable
        final FqName fqName;
        final boolean staticMembers;
        final boolean kotlin;
        final ClassOrNamespaceDescriptor classOrNamespaceDescriptor;
        private List<String> alternativeSignatureErrors;

        protected ResolverScopeData(@Nullable PsiClass psiClass, @Nullable PsiPackage psiPackage, @Nullable FqName fqName, boolean staticMembers, @NotNull ClassOrNamespaceDescriptor descriptor) {
            checkPsiClassIsNotJet(psiClass);

            this.psiClass = psiClass;
            this.psiPackage = psiPackage;
            this.fqName = fqName;

            if (psiClass == null && psiPackage == null) {
                throw new IllegalStateException("both psiClass and psiPackage cannot be null");
            }

            this.staticMembers = staticMembers;
            this.kotlin = psiClass != null &&
                    (new PsiClassWrapper(psiClass).getJetClass().isDefined() || psiClass.getName().equals(JvmAbi.PACKAGE_CLASS));
            classOrNamespaceDescriptor = descriptor;

            if (fqName != null && fqName.lastSegmentIs(Name.identifier(JvmAbi.PACKAGE_CLASS)) && psiClass != null && kotlin) {
                throw new IllegalStateException("Kotlin namespace cannot have last segment " + JvmAbi.PACKAGE_CLASS + ": " + fqName);
            }
        }

        protected ResolverScopeData(boolean negative) {
            if (!negative) {
                throw new IllegalStateException();
            }
            this.psiClass = null;
            this.psiPackage = null;
            this.fqName = null;
            this.staticMembers = false;
            this.kotlin = false;
            this.classOrNamespaceDescriptor = null;
        }

        public boolean isPositive() {
            return this.classOrNamespaceDescriptor != null;
        }

        @NotNull
        public PsiElement getPsiPackageOrPsiClass() {
            if (psiPackage != null) {
                return psiPackage;
            }
            else {
                return psiClass;
            }
        }

        private Map<Name, NamedMembers> namedMembersMap;

        public void addAlternativeSignatureError(@NotNull String errorMessage) {
            if (alternativeSignatureErrors == null) {
                alternativeSignatureErrors = new ArrayList<String>();
            }
            alternativeSignatureErrors.add(errorMessage);
        }
        
        @NotNull
        public abstract List<TypeParameterDescriptor> getTypeParameters();
    }

    /** Class with instance members */
    static class ResolverBinaryClassData extends ResolverClassData {

        ResolverBinaryClassData(@NotNull PsiClass psiClass, @Nullable FqName fqName, @NotNull ClassDescriptorFromJvmBytecode classDescriptor) {
            super(psiClass, null, fqName, false, classDescriptor);
        }

        ResolverBinaryClassData(boolean negative) {
            super(negative);
        }

        static final ResolverClassData NEGATIVE = new ResolverBinaryClassData(true);

    }

    static class ResolverClassData extends ResolverScopeData {

        final ClassDescriptorFromJvmBytecode classDescriptor;

        List<JavaDescriptorSignatureResolver.TypeParameterDescriptorInitialization> typeParameters;

        protected ResolverClassData(boolean negative) {
            super(negative);
            this.classDescriptor = null;
        }
        

        protected ResolverClassData(
                @Nullable PsiClass psiClass,
                @Nullable PsiPackage psiPackage,
                @Nullable FqName fqName,
                boolean staticMembers,
                @NotNull ClassDescriptorFromJvmBytecode descriptor
        ) {
            super(psiClass, psiPackage, fqName, staticMembers, descriptor);
            classDescriptor = descriptor;
        }

        @NotNull
        public ClassDescriptor getClassDescriptor() {
            return classDescriptor;
        }

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getTypeParameters() {
            return getClassDescriptor().getTypeConstructor().getParameters();
        }

    }


    static class ResolverEnumClassObjectClassData extends ResolverClassData {

        protected ResolverEnumClassObjectClassData(
                @Nullable PsiClass psiClass,
                @Nullable FqName fqName,
                @NotNull ClassDescriptorFromJvmBytecode descriptor
        ) {
            super(psiClass, null, fqName, true, descriptor);
        }
    }

    /** Either package or class with static members */
    static class ResolverNamespaceData extends ResolverScopeData {
        private final NamespaceDescriptor namespaceDescriptor;

        ResolverNamespaceData(@Nullable PsiClass psiClass, @Nullable PsiPackage psiPackage, @NotNull FqName fqName, @NotNull NamespaceDescriptor namespaceDescriptor) {
            super(psiClass, psiPackage, fqName, true, namespaceDescriptor);
            this.namespaceDescriptor = namespaceDescriptor;
        }

        private ResolverNamespaceData(boolean negative) {
            super(negative);
            this.namespaceDescriptor = null;
        }

        static final ResolverNamespaceData NEGATIVE = new ResolverNamespaceData(true);

        private JavaPackageScope memberScope;

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getTypeParameters() {
            return new ArrayList<TypeParameterDescriptor>(0);
        }
    }

    // NOTE: this complexity is introduced because class descriptors do not always have valid fqnames (class objects) 
    protected final Map<FqNameBase, ResolverClassData> classDescriptorCache = new THashMap<FqNameBase, ResolverClassData>(new TObjectHashingStrategy<FqNameBase>() {
        @Override
        public int computeHashCode(FqNameBase o) {
            if (o instanceof FqName) {
                return ((FqName) o).toUnsafe().hashCode();
            }
            assert o instanceof FqNameUnsafe;
            return o.hashCode();
        }

        @Override
        public boolean equals(FqNameBase n1, FqNameBase n2) {
            return n1.equalsTo(n2.toString()) && n2.equalsTo(n1.toString());
        }
    });
    protected final Map<FqName, ResolverNamespaceData> namespaceDescriptorCacheByFqn = Maps.newHashMap();

    protected Project project;
    protected JavaSemanticServices semanticServices;
    private NamespaceFactory namespaceFactory;
    private BindingTrace trace;
    private PsiClassFinder psiClassFinder;
    private JavaDescriptorSignatureResolver javaDescriptorSignatureResolver;

    @Inject
    public void setProject(Project project) {
        this.project = project;
    }

    @Inject
    public void setNamespaceFactory(NamespaceFactoryImpl namespaceFactory) {
        this.namespaceFactory = namespaceFactory;
    }

    @Inject
    public void setSemanticServices(JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setPsiClassFinder(PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @Inject
    public void setJavaDescriptorSignatureResolver(JavaDescriptorSignatureResolver javaDescriptorSignatureResolver) {
        this.javaDescriptorSignatureResolver = javaDescriptorSignatureResolver;
    }


    @Nullable
    private ClassDescriptor resolveJavaLangObject() {
        ClassDescriptor clazz = resolveClass(OBJECT_FQ_NAME, DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
        if (clazz == null) {
            // TODO: warning
        }
        return clazz;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        List<Runnable> tasks = Lists.newArrayList();
        ClassDescriptor clazz = resolveClass(qualifiedName, searchRule, tasks);
        for (Runnable task : tasks) {
            task.run();
        }
        return clazz;
    }

    @Override
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName) {
        return resolveClass(qualifiedName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    private ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule, @NotNull List<Runnable> tasks) {
        if (qualifiedName.getFqName().endsWith(JvmAbi.TRAIT_IMPL_SUFFIX)) {
            // TODO: only if -$$TImpl class is created by Kotlin
            return null;
        }

        ClassDescriptor builtinClassDescriptor = semanticServices.getKotlinBuiltinClassDescriptor(qualifiedName);
        if (builtinClassDescriptor != null) {
            return builtinClassDescriptor;
        }

        // First, let's check that this is a real Java class, not a Java's view on a Kotlin class:
        ClassDescriptor kotlinClassDescriptor = semanticServices.getKotlinClassDescriptor(qualifiedName);
        if (kotlinClassDescriptor != null) {
            if (searchRule == DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN) {
                throw new IllegalStateException("class must not be found in kotlin: " + qualifiedName);
            }
            else if (searchRule == DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN) {
                return null;
            }
            else if (searchRule == DescriptorSearchRule.INCLUDE_KOTLIN) {
                return kotlinClassDescriptor;
            }
            else {
                throw new IllegalStateException("unknown searchRule: " + searchRule);
            }
        }

        // Not let's take a descriptor of a Java class
        ResolverClassData classData = classDescriptorCache.get(qualifiedName);
        if (classData == null) {
            PsiClass psiClass = psiClassFinder.findPsiClass(qualifiedName, PsiClassFinder.RuntimeClassesHandleMode.THROW);
            if (psiClass == null) {
                ResolverClassData oldValue = classDescriptorCache.put(qualifiedName, ResolverBinaryClassData.NEGATIVE);
                if (oldValue != null) {
                    throw new IllegalStateException("rewrite at " + qualifiedName);
                }
                return null;
            }
            classData = createJavaClassDescriptor(psiClass, tasks);
        }
        return classData.classDescriptor;
    }

    @NotNull
    private ResolverClassData createJavaClassDescriptor(@NotNull final PsiClass psiClass, List<Runnable> taskList) {
        FqName fqName = new FqName(psiClass.getQualifiedName());
        if (classDescriptorCache.containsKey(fqName)) {
            throw new IllegalStateException(psiClass.getQualifiedName());
        }

        checkPsiClassIsNotJet(psiClass);

        Name name = Name.identifier(psiClass.getName());
        ClassKind kind = psiClass.isInterface() ? (psiClass.isAnnotationType() ? ClassKind.ANNOTATION_CLASS : ClassKind.TRAIT) : (psiClass.isEnum() ? ClassKind.ENUM_CLASS : ClassKind.CLASS);
        ClassOrNamespaceDescriptor containingDeclaration = resolveParentDescriptor(psiClass);

        // class may be resolved during resolution of parent
        ResolverClassData classData = classDescriptorCache.get(fqName);
        if (classData != null) {
            return classData;
        }

        classData = new ClassDescriptorFromJvmBytecode(
                containingDeclaration, kind, psiClass, fqName, this)
                        .getResolverBinaryClassData();
        classDescriptorCache.put(fqName, classData);
        classData.classDescriptor.setName(name);

        List<JetType> supertypes = new ArrayList<JetType>();

        classData.typeParameters = javaDescriptorSignatureResolver.createUninitializedClassTypeParameters(psiClass, classData);
        
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>();
        for (JavaDescriptorSignatureResolver.TypeParameterDescriptorInitialization typeParameter : classData.typeParameters) {
            typeParameters.add(typeParameter.descriptor);
        }
        
        classData.classDescriptor.setTypeParameterDescriptors(typeParameters);
        classData.classDescriptor.setSupertypes(supertypes);
        classData.classDescriptor.setVisibility(resolveVisibility(psiClass, JetClassAnnotation.get(psiClass)));
        Modality modality;
        if (classData.classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
            modality = Modality.FINAL;
        }
        else {
            modality = Modality.convertFromFlags(
                    psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface(),
                    !psiClass.hasModifierProperty(PsiModifier.FINAL));
        }
        classData.classDescriptor.setModality(modality);
        classData.classDescriptor.createTypeConstructor();
        classData.classDescriptor.setScopeForMemberLookup(new JavaClassMembersScope(semanticServices, classData));

        javaDescriptorSignatureResolver.initializeTypeParameters(classData.typeParameters, classData.classDescriptor, "class " + psiClass.getQualifiedName());

        // TODO: ugly hack: tests crash if initializeTypeParameters called with class containing proper supertypes
        supertypes.addAll(getSupertypes(new PsiClassWrapper(psiClass), classData, classData.getTypeParameters()));

        MutableClassDescriptorLite classObject = createClassObjectDescriptor(classData.classDescriptor, psiClass);
        if (classObject != null) {
            classData.classDescriptor.getBuilder().setClassObjectDescriptor(classObject);
        }

        classData.classDescriptor.setAnnotations(resolveAnnotations(psiClass, taskList));

        trace.record(BindingContext.CLASS, psiClass, classData.classDescriptor);

        return classData;
    }

    @NotNull
    public Collection<ConstructorDescriptor> resolveConstructors(@NotNull ResolverClassData classData) {
        Collection<ConstructorDescriptor> constructors = Lists.newArrayList();

        PsiClass psiClass = classData.psiClass;

        ClassDescriptorFromJvmBytecode containingClass = classData.classDescriptor;
        TypeVariableResolver resolverForTypeParameters = TypeVariableResolvers.classTypeVariableResolver(
                containingClass, "class " + psiClass.getQualifiedName());

        List<TypeParameterDescriptor> typeParameters = containingClass.getTypeConstructor().getParameters();

        PsiMethod[] psiConstructors = psiClass.getConstructors();

        boolean isStatic = psiClass.hasModifierProperty(PsiModifier.STATIC);
        if (containingClass.getKind() == ClassKind.OBJECT || containingClass.getKind() == ClassKind.CLASS_OBJECT) {
            constructors.add(createPrimaryConstructorForObject(containingClass));
        }
        else if (psiConstructors.length == 0) {
            // We need to create default constructors for classes and abstract classes.
            // Example:
            // class Kotlin() : Java() {}
            // abstract public class Java {}
            if (!psiClass.isInterface()) {
                ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                        containingClass,
                        Collections.<AnnotationDescriptor>emptyList(),
                        false);
                constructorDescriptor.initialize(typeParameters, Collections.<ValueParameterDescriptor>emptyList(), containingClass
                        .getVisibility(), isStatic);
                constructors.add(constructorDescriptor);
                trace.record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
            }
            if (psiClass.isAnnotationType()) {
                // A constructor for an annotation type takes all the "methods" in the @interface as parameters
                ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                        containingClass,
                        Collections.<AnnotationDescriptor>emptyList(),
                        false);

                List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();
                PsiMethod[] methods = psiClass.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    PsiMethod method = methods[i];
                    if (method instanceof PsiAnnotationMethod) {
                        PsiAnnotationMethod annotationMethod = (PsiAnnotationMethod) method;
                        assert annotationMethod.getParameterList().getParameters().length == 0;

                        PsiType returnType = annotationMethod.getReturnType();

                        // We take the following heuristical convention:
                        // if the last method of the @interface is an array, we convert it into a vararg
                        JetType varargElementType = null;
                        if (i == methods.length - 1 && (returnType instanceof PsiArrayType)) {
                            varargElementType = semanticServices.getTypeTransformer().transformToType(((PsiArrayType) returnType).getComponentType(), resolverForTypeParameters);
                        }

                        valueParameters.add(new ValueParameterDescriptorImpl(
                                constructorDescriptor,
                                i,
                                Collections.<AnnotationDescriptor>emptyList(),
                                Name.identifier(method.getName()),
                                false,
                                semanticServices.getTypeTransformer().transformToType(returnType, resolverForTypeParameters),
                                annotationMethod.getDefaultValue() != null,
                                varargElementType));
                    }
                }

                constructorDescriptor.initialize(typeParameters, valueParameters, containingClass.getVisibility(), isStatic);
                constructors.add(constructorDescriptor);
                trace.record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
            }
        }
        else {
            for (PsiMethod psiConstructor : psiConstructors) {
                ConstructorDescriptor constructor = resolveConstructor(psiClass, classData, isStatic, psiConstructor);
                if (constructor != null) {
                    constructors.add(constructor);
                }
            }
        }

        for (ConstructorDescriptor constructor : constructors) {
            ((ConstructorDescriptorImpl) constructor).setReturnType(containingClass.getDefaultType());
        }

        return constructors;
    }

    @Nullable
    private ConstructorDescriptor resolveConstructor(PsiClass psiClass, ResolverClassData classData, boolean aStatic, PsiMethod psiConstructor) {
        PsiMethodWrapper constructor = new PsiMethodWrapper(psiConstructor);

        if (constructor.getJetConstructor().hidden()) {
            return null;
        }

        ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                classData.classDescriptor,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                false);
        String context = "constructor of class " + psiClass.getQualifiedName();
        ValueParameterDescriptors valueParameterDescriptors = resolveParameterDescriptors(constructorDescriptor,
                constructor.getParameters(),
                TypeVariableResolvers.classTypeVariableResolver(classData.classDescriptor, context));
        if (valueParameterDescriptors.receiverType != null) {
            throw new IllegalStateException();
        }
        constructorDescriptor.initialize(classData.classDescriptor.getTypeConstructor().getParameters(),
                valueParameterDescriptors.descriptors,
                resolveVisibility(psiConstructor, constructor.getJetConstructor()), aStatic);
        trace.record(BindingContext.CONSTRUCTOR, psiConstructor, constructorDescriptor);
        return constructorDescriptor;
    }

    static void checkPsiClassIsNotJet(PsiClass psiClass) {
        if (psiClass instanceof JetJavaMirrorMarker) {
            throw new IllegalStateException("trying to resolve fake jet PsiClass as regular PsiClass: " + psiClass.getQualifiedName());
        }
    }

    @Nullable
    private PsiClass getInnerClassClassObject(@NotNull PsiClass outer) {
        for (PsiClass inner : outer.getInnerClasses()) {
            if (inner.getName().equals(JvmAbi.CLASS_OBJECT_CLASS_NAME)) {
                return inner;
            }
        }
        return null;
    }

    /**
     * TODO
     * @see #createJavaNamespaceDescriptor(com.intellij.psi.PsiClass)
     */
    @Nullable
    private MutableClassDescriptorLite createClassObjectDescriptor(@NotNull ClassDescriptor containing, @NotNull PsiClass psiClass) {
        checkPsiClassIsNotJet(psiClass);

        if (psiClass.isEnum()) {
            return createClassObjectDescriptorForEnum(containing, psiClass);
        }

        PsiClass classObjectPsiClass = getInnerClassClassObject(psiClass);
        if (classObjectPsiClass == null) {
            return null;
        }

        FqName fqName = new FqName(classObjectPsiClass.getQualifiedName());
        ResolverClassData classData = new ClassDescriptorFromJvmBytecode(
                containing, ClassKind.CLASS_OBJECT, classObjectPsiClass, fqName, this)
                        .getResolverBinaryClassData();

        ClassDescriptorFromJvmBytecode classObjectDescriptor = classData.classDescriptor;
        classObjectDescriptor.setSupertypes(
                getSupertypes(new PsiClassWrapper(classObjectPsiClass), classData, new ArrayList<TypeParameterDescriptor>(0)));
        setUpClassObjectDescriptor(containing, fqName, classData, getClassObjectName(containing.getName()));
        return classObjectDescriptor;
    }

    @NotNull
    private MutableClassDescriptorLite createClassObjectDescriptorForEnum(@NotNull ClassDescriptor containing, @NotNull PsiClass psiClass) {
        String psiClassQualifiedName = psiClass.getQualifiedName();
        assert psiClassQualifiedName != null : "Reading java class with no qualified name";
        FqNameUnsafe fqName = new FqNameUnsafe(psiClassQualifiedName + "." + getClassObjectName(psiClass.getName()).getName());
        ClassDescriptorFromJvmBytecode classObjectDescriptor = new ClassDescriptorFromJvmBytecode(
                containing, ClassKind.CLASS_OBJECT, psiClass, null, this);

        ResolverEnumClassObjectClassData data = new ResolverEnumClassObjectClassData(psiClass, null, classObjectDescriptor);
        setUpClassObjectDescriptor(containing, fqName, data, getClassObjectName(containing.getName().getName()));

        classObjectDescriptor.getBuilder().addFunctionDescriptor(createEnumClassObjectValuesMethod(classObjectDescriptor, trace));
        classObjectDescriptor.getBuilder().addFunctionDescriptor(createEnumClassObjectValueOfMethod(classObjectDescriptor, trace));
        
        return classObjectDescriptor;
    }

    private void setUpClassObjectDescriptor(
            @NotNull ClassDescriptor containing,
            @NotNull FqNameBase fqName,
            @NotNull ResolverClassData data,
            @NotNull Name classObjectName
    ) {
        ClassDescriptorFromJvmBytecode classDescriptor = data.classDescriptor;
        classDescriptorCache.put(fqName, data);
        classDescriptor.setName(classObjectName);
        classDescriptor.setModality(Modality.FINAL);
        classDescriptor.setVisibility(containing.getVisibility());
        classDescriptor.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
        classDescriptor.createTypeConstructor();
        JavaClassMembersScope classMembersScope = new JavaClassMembersScope(semanticServices, data);
        WritableScopeImpl writableScope = new WritableScopeImpl(classMembersScope, classDescriptor, RedeclarationHandler.THROW_EXCEPTION, fqName.toString());
        writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        classDescriptor.setScopeForMemberLookup(writableScope);
    }

    static boolean isJavaLangObject(JetType type) {
        ClassifierDescriptor classifierDescriptor = type.getConstructor().getDeclarationDescriptor();
        return classifierDescriptor instanceof ClassDescriptor &&
               DescriptorUtils.getFQName(classifierDescriptor).equalsTo(OBJECT_FQ_NAME);
    }


    @NotNull
    private ClassOrNamespaceDescriptor resolveParentDescriptor(@NotNull PsiClass psiClass) {
        FqName fqName = new FqName(psiClass.getQualifiedName());

        PsiClass containingClass = psiClass.getContainingClass();
        if (containingClass != null) {
            FqName containerFqName = new FqName(containingClass.getQualifiedName());
            ClassDescriptor clazz = resolveClass(containerFqName, DescriptorSearchRule.INCLUDE_KOTLIN);
            if (clazz == null) {
                throw new IllegalStateException("PsiClass not found by name " + containerFqName + ", required to be container declaration of " + fqName);
            }
            return clazz;
        }

        NamespaceDescriptor ns = resolveNamespace(fqName.parent(), DescriptorSearchRule.INCLUDE_KOTLIN);
        if (ns == null) {
            throw new IllegalStateException("cannot resolve namespace " + fqName.parent() + ", required to be container for " + fqName);
        }
        return ns;
    }

    private Collection<JetType> getSupertypes(PsiClassWrapper psiClass, ResolverClassData classData, List<TypeParameterDescriptor> typeParameters) {
        ClassDescriptor classDescriptor = classData.classDescriptor;

        final List<JetType> result = new ArrayList<JetType>();

        String context = "class " + psiClass.getQualifiedName();

        if (psiClass.getJetClass().signature().length() > 0) {
            final TypeVariableResolver typeVariableResolver = TypeVariableResolvers.typeVariableResolverFromTypeParameters(typeParameters, classDescriptor, context);
            
            new JetSignatureReader(psiClass.getJetClass().signature()).accept(new JetSignatureExceptionsAdapter() {
                @Override
                public JetSignatureVisitor visitFormalTypeParameter(String name, TypeInfoVariance variance, boolean reified) {
                    // TODO: collect
                    return new JetSignatureAdapter();
                }

                @Override
                public JetSignatureVisitor visitSuperclass() {
                    return new JetTypeJetSignatureReader(semanticServices, JetStandardLibrary.getInstance(), typeVariableResolver) {
                        @Override
                        protected void done(@NotNull JetType jetType) {
                            if (!jetType.equals(JetStandardClasses.getAnyType())) {
                                result.add(jetType);
                            }
                        }
                    };
                }

                @Override
                public JetSignatureVisitor visitInterface() {
                    return visitSuperclass();
                }
            });
        }
        else {
            TypeVariableResolver typeVariableResolverForSupertypes = TypeVariableResolvers.typeVariableResolverFromTypeParameters(typeParameters, classDescriptor, context);
            transformSupertypeList(result, psiClass.getPsiClass().getExtendsListTypes(), typeVariableResolverForSupertypes);
            transformSupertypeList(result, psiClass.getPsiClass().getImplementsListTypes(), typeVariableResolverForSupertypes);
        }
        
        for (JetType supertype : result) {
            if (ErrorUtils.isErrorType(supertype)) {
                trace.record(BindingContext.INCOMPLETE_HIERARCHY, classDescriptor);
            }
        }
        
        if (result.isEmpty()) {
            if (classData.kotlin
                    || OBJECT_FQ_NAME.equalsTo(psiClass.getQualifiedName())
                    // TODO: annotations
                    || classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
                result.add(JetStandardClasses.getAnyType());
            }
            else {
                ClassDescriptor object = resolveJavaLangObject();
                if (object != null) {
                    result.add(object.getDefaultType());
                }
                else {
                    result.add(JetStandardClasses.getAnyType());
                }
            }
        }
        return result;
    }

    private void transformSupertypeList(List<JetType> result, PsiClassType[] extendsListTypes, TypeVariableResolver typeVariableResolver) {
        for (PsiClassType type : extendsListTypes) {
            PsiClass resolved = type.resolve();
            if (resolved != null && JvmStdlibNames.JET_OBJECT.getFqName().equalsTo(resolved.getQualifiedName())) {
                continue;
            }

            JetType transform = semanticServices.getTypeTransformer().transformToType(type, JavaTypeTransformer.TypeUsage.SUPERTYPE, typeVariableResolver);
            if (ErrorUtils.isErrorType(transform)) {
                continue;
            }

            result.add(TypeUtils.makeNotNullable(transform));
        }
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        // First, let's check that there is no Kotlin package:
        NamespaceDescriptor kotlinNamespaceDescriptor = semanticServices.getKotlinNamespaceDescriptor(qualifiedName);
        if (kotlinNamespaceDescriptor != null) {
            if (searchRule == DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN) {
                throw new IllegalStateException("class must not be found in kotlin: " + qualifiedName);
            }
            else if (searchRule == DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN) {
                return null;
            }
            else if (searchRule == DescriptorSearchRule.INCLUDE_KOTLIN) {
                // TODO: probably this is evil
                return kotlinNamespaceDescriptor;
            }
            else {
                throw new IllegalStateException("unknown searchRule: " + searchRule);
            }
        }

        ResolverNamespaceData namespaceData = namespaceDescriptorCacheByFqn.get(qualifiedName);
        if (namespaceData != null) {
            return namespaceData.namespaceDescriptor;
        }

        NamespaceDescriptorParent parentNs = resolveParentNamespace(qualifiedName);
        if (parentNs == null) {
            return null;
        }

        JavaNamespaceDescriptor ns = new JavaNamespaceDescriptor(
                parentNs,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                qualifiedName
        );

        ResolverNamespaceData scopeData = createNamespaceResolverScopeData(qualifiedName, ns);
        if (scopeData == null) {
            return null;
        }

        trace.record(BindingContext.NAMESPACE, scopeData.getPsiPackageOrPsiClass(), ns);

        ns.setMemberScope(scopeData.memberScope);

        return scopeData.namespaceDescriptor;
    }

    @Override
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName) {
        return resolveNamespace(qualifiedName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    private NamespaceDescriptorParent resolveParentNamespace(FqName fqName) {
        if (fqName.isRoot()) {
            return FAKE_ROOT_MODULE;
        }
        else {
            return resolveNamespace(fqName.parent(), DescriptorSearchRule.INCLUDE_KOTLIN);
        }
    }

    @Nullable
    private ResolverNamespaceData createNamespaceResolverScopeData(@NotNull FqName fqName, @NotNull NamespaceDescriptor ns) {
        PsiPackage psiPackage;
        PsiClass psiClass;

        lookingForPsi:
        {
            psiClass = getPsiClassForJavaPackageScope(fqName);
            psiPackage = semanticServices.getPsiClassFinder().findPsiPackage(fqName);
            if (psiClass != null || psiPackage != null) {
                trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, ns, JavaNamespaceKind.PROPER);
                break lookingForPsi;
            }

            psiClass = psiClassFinder.findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
            if (psiClass != null && !psiClass.isEnum()) {
                trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, ns, JavaNamespaceKind.CLASS_STATICS);
                break lookingForPsi;
            }

            ResolverNamespaceData oldValue = namespaceDescriptorCacheByFqn.put(fqName, ResolverNamespaceData.NEGATIVE);
            if (oldValue != null) {
                throw new IllegalStateException("rewrite at " + fqName);
            }
            return null;
        }

        ResolverNamespaceData namespaceData = new ResolverNamespaceData(psiClass, psiPackage, fqName, ns);

        namespaceData.memberScope = new JavaPackageScope(fqName, semanticServices, namespaceData);

        ResolverNamespaceData oldValue = namespaceDescriptorCacheByFqn.put(fqName, namespaceData);
        if (oldValue != null) {
            throw new IllegalStateException("rewrite at "  + fqName);
        }

        return namespaceData;
    }

    @Nullable
    public JavaPackageScope getJavaPackageScope(@NotNull FqName fqName, @NotNull NamespaceDescriptor ns) {
        ResolverNamespaceData resolverNamespaceData = namespaceDescriptorCacheByFqn.get(fqName);
        if (resolverNamespaceData == null) {
            resolverNamespaceData = createNamespaceResolverScopeData(fqName, ns);
        }
        if (resolverNamespaceData == null) {
            return null;
        }
        if (resolverNamespaceData == ResolverNamespaceData.NEGATIVE) {
            throw new IllegalStateException("This means that we are trying to create a Java package, but have a package with the same FQN defined in Kotlin: " + fqName);
        }
        JavaPackageScope scope = resolverNamespaceData.memberScope;
        if (scope == null) {
            throw new IllegalStateException("fqn: " + fqName);
        }
        return scope;
    }

    @Nullable
    private PsiClass getPsiClassForJavaPackageScope(@NotNull FqName packageFQN) {
        return psiClassFinder.findPsiClass(packageFQN.child(Name.identifier(JvmAbi.PACKAGE_CLASS)), PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
    }

    static class ValueParameterDescriptors {
        final JetType receiverType;
        final List<ValueParameterDescriptor> descriptors;

        ValueParameterDescriptors(@Nullable JetType receiverType, List<ValueParameterDescriptor> descriptors) {
            this.receiverType = receiverType;
            this.descriptors = descriptors;
        }
    }

    private enum JvmMethodParameterKind {
        REGULAR,
        RECEIVER,
        TYPE_INFO,
    }
    
    private static class JvmMethodParameterMeaning {
        private final JvmMethodParameterKind kind;
        private final JetType receiverType;
        private final ValueParameterDescriptor valueParameterDescriptor;
        private final Object typeInfo;

        private JvmMethodParameterMeaning(JvmMethodParameterKind kind, JetType receiverType, ValueParameterDescriptor valueParameterDescriptor, Object typeInfo) {
            this.kind = kind;
            this.receiverType = receiverType;
            this.valueParameterDescriptor = valueParameterDescriptor;
            this.typeInfo = typeInfo;
        }
        
        public static JvmMethodParameterMeaning receiver(@NotNull JetType receiverType) {
            return new JvmMethodParameterMeaning(JvmMethodParameterKind.RECEIVER, receiverType, null, null);
        }
        
        public static JvmMethodParameterMeaning regular(@NotNull ValueParameterDescriptor valueParameterDescriptor) {
            return new JvmMethodParameterMeaning(JvmMethodParameterKind.REGULAR, null, valueParameterDescriptor, null);
        }
        
        public static JvmMethodParameterMeaning typeInfo(@NotNull Object typeInfo) {
            return new JvmMethodParameterMeaning(JvmMethodParameterKind.TYPE_INFO, null, null, typeInfo);
        }
    }

    @NotNull
    private JvmMethodParameterMeaning resolveParameterDescriptor(DeclarationDescriptor containingDeclaration, int i,
            PsiParameterWrapper parameter, TypeVariableResolver typeVariableResolver) {

        if (parameter.getJetTypeParameter().isDefined()) {
            return JvmMethodParameterMeaning.typeInfo(new Object());
        }

        PsiType psiType = parameter.getPsiParameter().getType();

        // TODO: must be very slow, make it lazy?
        Name name = Name.identifier(parameter.getPsiParameter().getName() != null ? parameter.getPsiParameter().getName() : "p" + i);

        if (parameter.getJetValueParameter().name().length() > 0) {
            name = Name.identifier(parameter.getJetValueParameter().name());
        }
        
        String typeFromAnnotation = parameter.getJetValueParameter().type();
        boolean receiver = parameter.getJetValueParameter().receiver();
        boolean hasDefaultValue = parameter.getJetValueParameter().hasDefaultValue();

        JetType outType;
        if (typeFromAnnotation.length() > 0) {
            outType = semanticServices.getTypeTransformer().transformToType(typeFromAnnotation, typeVariableResolver);
        }
        else {
            outType = semanticServices.getTypeTransformer().transformToType(psiType, JavaTypeTransformer.TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT, typeVariableResolver);
        }

        JetType varargElementType;
        if (psiType instanceof PsiEllipsisType) {
            varargElementType = JetStandardLibrary.getInstance().getArrayElementType(TypeUtils.makeNotNullable(outType));
            outType = TypeUtils.makeNotNullable(outType);
        }
        else {
            varargElementType = null;
        }

        if (receiver) {
            return JvmMethodParameterMeaning.receiver(outType);
        }
        else {

            JetType transformedType;
            if (parameter.getJetValueParameter().nullable()) {
                transformedType = TypeUtils.makeNullableAsSpecified(outType, parameter.getJetValueParameter().nullable());
            }
            else if (findAnnotation(parameter.getPsiParameter(), JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) != null) {
                transformedType = TypeUtils.makeNullableAsSpecified(outType, false);
            }
            else {
                transformedType = outType;
            }
            return JvmMethodParameterMeaning.regular(new ValueParameterDescriptorImpl(
                    containingDeclaration,
                    i,
                    Collections.<AnnotationDescriptor>emptyList(), // TODO
                    name,
                    false,
                    transformedType,
                    hasDefaultValue,
                    varargElementType
            ));
        }
    }

    public Set<VariableDescriptor> resolveFieldGroupByName(@NotNull Name fieldName, @NotNull ResolverScopeData scopeData) {

        if (scopeData.psiClass == null) {
            return Collections.emptySet();
        }

        getResolverScopeData(scopeData);

        NamedMembers namedMembers = scopeData.namedMembersMap.get(fieldName);
        if (namedMembers == null) {
            return Collections.emptySet();
        }

        resolveNamedGroupProperties(scopeData.classOrNamespaceDescriptor, scopeData, namedMembers, fieldName,
                "class or namespace " + scopeData.psiClass.getQualifiedName());

        return namedMembers.propertyDescriptors;
    }
    
    @NotNull
    public Set<VariableDescriptor> resolveFieldGroup(@NotNull ResolverScopeData scopeData) {

        getResolverScopeData(scopeData);

        Set<VariableDescriptor> descriptors = Sets.newHashSet();
        Map<Name, NamedMembers> membersForProperties = scopeData.namedMembersMap;
        for (Map.Entry<Name, NamedMembers> entry : membersForProperties.entrySet()) {
            NamedMembers namedMembers = entry.getValue();
            Name propertyName = entry.getKey();

            resolveNamedGroupProperties(scopeData.classOrNamespaceDescriptor, scopeData, namedMembers, propertyName, "class or namespace " + scopeData.psiClass.getQualifiedName());
            descriptors.addAll(namedMembers.propertyDescriptors);
        }

        return descriptors;
    }
    
    private Object key(TypeSource typeSource) {
        if (typeSource == null) {
            return "";
        }
        else if (typeSource.getTypeString().length() > 0) {
            return typeSource.getTypeString();
        }
        else {
            return psiTypeToKey(typeSource.getPsiType());
        }
    }

    private Object psiTypeToKey(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            return ((PsiClassType) psiType).getClassName();
        }
        else if (psiType instanceof PsiPrimitiveType) {
            return psiType.getPresentableText();
        }
        else if (psiType instanceof PsiArrayType) {
            return Pair.create("[", psiTypeToKey(((PsiArrayType) psiType).getComponentType()));
        }
        else {
            throw new IllegalStateException("" + psiType.getClass());
        }
    }

    private Object propertyKeyForGrouping(PropertyAccessorData propertyAccessor) {
        Object type = key(propertyAccessor.getType());
        Object receiverType = key(propertyAccessor.getReceiverType());
        return Pair.create(type, receiverType);
    }

    private void resolveNamedGroupProperties(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull ResolverScopeData scopeData,
            @NotNull NamedMembers namedMembers, @NotNull Name propertyName,
            @NotNull String context) {
        getResolverScopeData(scopeData);

        if (namedMembers.propertyDescriptors != null) {
            return;
        }
        
        if (namedMembers.propertyAccessors == null) {
            namedMembers.propertyAccessors = Collections.emptyList();
        }

        class GroupingValue {
            PropertyAccessorData getter;
            PropertyAccessorData setter;
            PropertyAccessorData field;
            boolean ext;
        }
        
        Map<Object, GroupingValue> map = new HashMap<Object, GroupingValue>();

        for (PropertyAccessorData propertyAccessor : namedMembers.propertyAccessors) {

            Object key = propertyKeyForGrouping(propertyAccessor);
            
            GroupingValue value = map.get(key);
            if (value == null) {
                value = new GroupingValue();
                value.ext = propertyAccessor.getReceiverType() != null;
                map.put(key, value);
            }

            if (value.ext != (propertyAccessor.getReceiverType() != null)) {
                throw new IllegalStateException("internal error, incorrect key");
            }

            if (propertyAccessor.isGetter()) {
                if (value.getter != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.getter = propertyAccessor;
            }
            else if (propertyAccessor.isSetter()) {
                if (value.setter != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.setter = propertyAccessor;
            }
            else if (propertyAccessor.isField()) {
                if (value.field != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.field = propertyAccessor;
            }
            else {
                throw new IllegalStateException();
            }
        }

        
        Set<PropertyDescriptor> propertiesFromCurrent = new HashSet<PropertyDescriptor>(1);

        int regularProperitesCount = 0;
        for (GroupingValue members : map.values()) {
            if (!members.ext) {
                ++regularProperitesCount;
            }
        }

        for (GroupingValue members : map.values()) {

            // we cannot have more then one property with given name even if java code
            // has several fields, getters and setter of different types
            if (!members.ext && regularProperitesCount > 1) {
                continue;
            }

            boolean isFinal;
            if (!scopeData.kotlin) {
                isFinal = true;
            }
            else if (members.setter == null && members.getter == null) {
                isFinal = false;
            }
            else if (members.getter != null) {
                isFinal = members.getter.getMember().isFinal();
            }
            else if (members.setter != null) {
                isFinal = members.setter.getMember().isFinal();
            }
            else {
                isFinal = false;
            }

            PropertyAccessorData anyMember;
            if (members.getter != null) {
                anyMember = members.getter;
            }
            else if (members.field != null) {
                anyMember = members.field;
            }
            else if (members.setter != null) {
                anyMember = members.setter;
            }
            else {
                throw new IllegalStateException();
            }

            boolean isVar;
            if (members.getter == null && members.setter == null) {
                isVar = !members.field.getMember().isFinal();
            }
            else {
                isVar = members.setter != null;
            }

            Visibility visibility = resolveVisibility(anyMember.getMember().psiMember, null);
            CallableMemberDescriptor.Kind kind = CallableMemberDescriptor.Kind.DECLARATION;

            if (members.getter != null && members.getter.getMember() instanceof PsiMethodWrapper) {
                JetMethodAnnotation jetMethod = ((PsiMethodWrapper) members.getter.getMember()).getJetMethod();
                visibility = resolveVisibility(anyMember.getMember().psiMember, jetMethod);
                kind = DescriptorKindUtils.intToKind(jetMethod.kind());
            }

            DeclarationDescriptor realOwner = getRealOwner(owner, scopeData, anyMember.getMember().isStatic());

            boolean isEnumEntry = DescriptorUtils.isEnumClassObject(realOwner);
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                    realOwner,
                    resolveAnnotations(anyMember.getMember().psiMember),
                    resolveModality(anyMember.getMember(), isFinal || isEnumEntry),
                    visibility,
                    isVar,
                    propertyName,
                    kind);

            //TODO: this is a hack to indicate that this enum entry is an object
            // class descriptor for enum entries is not used by backends so for now this should be safe to use
            // remove this when JavaDescriptorResolver gets rewritten
            if (isEnumEntry) {
                ClassDescriptorImpl dummyClassDescriptorForEnumEntryObject =
                        new ClassDescriptorImpl(realOwner, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, propertyName);
                dummyClassDescriptorForEnumEntryObject.initialize(
                                    true,
                                    Collections.<TypeParameterDescriptor>emptyList(),
                                    Collections.<JetType>emptyList(), JetScope.EMPTY,
                                    Collections.<ConstructorDescriptor>emptySet(), null);
                trace.record(BindingContext.OBJECT_DECLARATION_CLASS, propertyDescriptor, dummyClassDescriptorForEnumEntryObject);
            }

            PropertyGetterDescriptor getterDescriptor = null;
            PropertySetterDescriptor setterDescriptor = null;
            if (members.getter != null) {
                getterDescriptor = new PropertyGetterDescriptor(
                        propertyDescriptor,
                        resolveAnnotations(members.getter.getMember().psiMember),
                        Modality.OPEN,
                        visibility,
                        true,
                        false,
                        kind);
            }
            if (members.setter != null) {
                Visibility setterVisibility = resolveVisibility(members.setter.getMember().psiMember, null);
                if (members.setter.getMember() instanceof PsiMethodWrapper) {
                    setterVisibility = resolveVisibility(members.setter.getMember().psiMember,
                                                         ((PsiMethodWrapper) members.setter.getMember()).getJetMethod());
                }
                setterDescriptor = new PropertySetterDescriptor(
                        propertyDescriptor,
                        resolveAnnotations(members.setter.getMember().psiMember),
                        Modality.OPEN,
                        setterVisibility,
                        true,
                        false,
                        kind);
            }

            propertyDescriptor.initialize(getterDescriptor, setterDescriptor);

            List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(0);

            if (members.setter != null) {
                PsiMethodWrapper method = (PsiMethodWrapper) members.setter.getMember();

                if (anyMember == members.setter) {
                    typeParameters = javaDescriptorSignatureResolver.resolveMethodTypeParameters(method, propertyDescriptor);
                }
            }
            if (members.getter != null) {
                PsiMethodWrapper method = (PsiMethodWrapper) members.getter.getMember();

                if (anyMember == members.getter) {
                    typeParameters = javaDescriptorSignatureResolver.resolveMethodTypeParameters(method, propertyDescriptor);
                }
            }

            TypeVariableResolver typeVariableResolverForPropertyInternals = TypeVariableResolvers.typeVariableResolverFromTypeParameters(typeParameters, propertyDescriptor, "property " + propertyName + " in " + context);

            JetType propertyType;
            if (anyMember.getType().getTypeString().length() > 0) {
                propertyType = semanticServices.getTypeTransformer().transformToType(anyMember.getType().getTypeString(), typeVariableResolverForPropertyInternals);
            }
            else {
                propertyType = semanticServices.getTypeTransformer().transformToType(anyMember.getType().getPsiType(), typeVariableResolverForPropertyInternals);
                if (findAnnotation(anyMember.getType().getPsiNotNullOwner(), JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) != null) {
                    propertyType = TypeUtils.makeNullableAsSpecified(propertyType, false);
                }
                else if (members.getter == null && members.setter == null && members.field.getMember().isFinal() && members.field.getMember().isStatic()) {
                    // http://youtrack.jetbrains.com/issue/KT-1388
                    propertyType = TypeUtils.makeNotNullable(propertyType);
                }
            }
            
            JetType receiverType;
            if (anyMember.getReceiverType() == null) {
                receiverType = null;
            }
            else if (anyMember.getReceiverType().getTypeString().length() > 0) {
                receiverType = semanticServices.getTypeTransformer().transformToType(anyMember.getReceiverType().getTypeString(), typeVariableResolverForPropertyInternals);
            }
            else {
                receiverType = semanticServices.getTypeTransformer().transformToType(anyMember.getReceiverType().getPsiType(), typeVariableResolverForPropertyInternals);
            }

            propertyDescriptor.setType(
                    propertyType,
                    typeParameters,
                    DescriptorUtils.getExpectedThisObjectIfNeeded(realOwner),
                    receiverType
            );
            if (getterDescriptor != null) {
                getterDescriptor.initialize(propertyType);
            }
            if (setterDescriptor != null) {
                setterDescriptor.initialize(new ValueParameterDescriptorImpl(setterDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("p0") /*TODO*/, false, propertyDescriptor.getType(), false, null));
            }

            if (kind == CallableMemberDescriptor.Kind.DECLARATION) {
                trace.record(BindingContext.VARIABLE, anyMember.getMember().psiMember, propertyDescriptor);
            }
            
            propertiesFromCurrent.add(propertyDescriptor);
        }


        Set<PropertyDescriptor> propertiesFromSupertypes = getPropertiesFromSupertypes(scopeData, propertyName);

        final Set<VariableDescriptor> properties = Sets.newHashSet();

        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            OverrideResolver.generateOverridesInFunctionGroup(propertyName, propertiesFromSupertypes, propertiesFromCurrent, classDescriptor,
                                                              new OverrideResolver.DescriptorSink() {
                @Override
                public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                    properties.add((PropertyDescriptor) fakeOverride);
                }

                @Override
                public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                    // nop
                }
            });
        }

        properties.addAll(propertiesFromCurrent);

        namedMembers.propertyDescriptors = properties;
    }

    @NotNull
    private ClassOrNamespaceDescriptor getRealOwner(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull ResolverScopeData scopeData,
            boolean isStatic
    ) {
        boolean isEnum = scopeData.psiClass.isEnum();
        if (isEnum && isStatic) {
            return resolveClass(new FqName(scopeData.psiClass.getQualifiedName())).getClassObjectDescriptor();
        }
        else {
            return owner;
        }
    }

    private void resolveNamedGroupFunctions(@NotNull ClassOrNamespaceDescriptor owner, PsiClass psiClass,
            TypeSubstitutor typeSubstitutorForGenericSuperclasses, NamedMembers namedMembers, Name methodName, ResolverScopeData scopeData) {
        if (namedMembers.functionDescriptors != null) {
            return;
        }

        TemporaryBindingTrace tempTrace = TemporaryBindingTrace.create(trace);

        final Set<FunctionDescriptor> functions = new HashSet<FunctionDescriptor>();

        Set<SimpleFunctionDescriptor> functionsFromCurrent = Sets.newHashSet();
        for (PsiMethodWrapper method : namedMembers.methods) {
            SimpleFunctionDescriptor function = resolveMethodToFunctionDescriptor(psiClass, method, scopeData, tempTrace);
            if (function != null) {
                functionsFromCurrent.add(function);
            }
        }

        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            Set<SimpleFunctionDescriptor> functionsFromSupertypes = getFunctionsFromSupertypes(scopeData, methodName);

            OverrideResolver.generateOverridesInFunctionGroup(methodName, functionsFromSupertypes, functionsFromCurrent, classDescriptor,
                                                              new OverrideResolver.DescriptorSink() {
                @Override
                public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                    functions.add((FunctionDescriptor) fakeOverride);
                }

                @Override
                public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                    // nop
                }
            });

        }

        functions.addAll(functionsFromCurrent);

        if (DescriptorUtils.isEnumClassObject(owner)) {
            for (FunctionDescriptor functionDescriptor : Lists.newArrayList(functions)) {
                if (isEnumSpecialMethod(functionDescriptor)) {
                    functions.remove(functionDescriptor);
                }
            }
        }
        
        try {
            namedMembers.functionDescriptors = functions;
            tempTrace.commit();
        } catch (Throwable e) {
            assert false : "No errors are expected while saving state";
            throw ExceptionUtils.rethrow(e);
        }
    }
    
    private Set<SimpleFunctionDescriptor> getFunctionsFromSupertypes(ResolverScopeData scopeData, Name methodName) {
        Set<SimpleFunctionDescriptor> r = Sets.newLinkedHashSet();
        for (JetType supertype : getSupertypes(scopeData)) {
            for (FunctionDescriptor function : supertype.getMemberScope().getFunctions(methodName)) {
                r.add((SimpleFunctionDescriptor) function);
            }
        }
        return r;
    }

    private Set<PropertyDescriptor> getPropertiesFromSupertypes(ResolverScopeData scopeData, Name propertyName) {
        Set<PropertyDescriptor> r = new HashSet<PropertyDescriptor>();
        for (JetType supertype : getSupertypes(scopeData)) {
            for (VariableDescriptor property : supertype.getMemberScope().getProperties(propertyName)) {
                r.add((PropertyDescriptor) property);
            }
        }
        return r;
    }

    private void getResolverScopeData(@NotNull ResolverScopeData scopeData) {
        if (scopeData.namedMembersMap == null) {
            scopeData.namedMembersMap = JavaDescriptorResolverHelper.getNamedMembers(scopeData);
        }
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroup(@NotNull Name methodName, @NotNull ResolverScopeData scopeData) {

        getResolverScopeData(scopeData);

        Map<Name, NamedMembers> namedMembersMap = scopeData.namedMembersMap;

        NamedMembers namedMembers = namedMembersMap.get(methodName);
        if (namedMembers != null && namedMembers.methods != null) {
            TypeSubstitutor typeSubstitutor = typeSubstitutorForGenericSupertypes(scopeData);

            resolveNamedGroupFunctions(scopeData.classOrNamespaceDescriptor, scopeData.psiClass, typeSubstitutor, namedMembers, methodName, scopeData);

            return namedMembers.functionDescriptors;
        }
        else {
            return Collections.emptySet();
        }
    }

    private TypeSubstitutor createSubstitutorForGenericSupertypes(@Nullable ClassDescriptor classDescriptor) {
        TypeSubstitutor typeSubstitutor;
        if (classDescriptor != null) {
            typeSubstitutor = SubstitutionUtils.buildDeepSubstitutor(classDescriptor.getDefaultType());
        }
        else {
            typeSubstitutor = TypeSubstitutor.EMPTY;
        }
        return typeSubstitutor;
    }

    private ValueParameterDescriptors resolveParameterDescriptors(DeclarationDescriptor containingDeclaration,
            List<PsiParameterWrapper> parameters, TypeVariableResolver typeVariableResolver) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        JetType receiverType = null;
        int indexDelta = 0;
        for (int i = 0, parametersLength = parameters.size(); i < parametersLength; i++) {
            PsiParameterWrapper parameter = parameters.get(i);
            JvmMethodParameterMeaning meaning = resolveParameterDescriptor(containingDeclaration, i + indexDelta, parameter, typeVariableResolver);
            if (meaning.kind == JvmMethodParameterKind.TYPE_INFO) {
                // TODO
                --indexDelta;
            }
            else if (meaning.kind == JvmMethodParameterKind.REGULAR) {
                result.add(meaning.valueParameterDescriptor);
            }
            else if (meaning.kind == JvmMethodParameterKind.RECEIVER) {
                if (receiverType != null) {
                    throw new IllegalStateException("more than one receiver");
                }
                --indexDelta;
                receiverType = meaning.receiverType;
            }
        }
        return new ValueParameterDescriptors(receiverType, result);
    }

    @Nullable
    private SimpleFunctionDescriptor resolveMethodToFunctionDescriptor(
            @NotNull final PsiClass psiClass, final PsiMethodWrapper method,
            @NotNull ResolverScopeData scopeData, BindingTrace tempTrace) {

        getResolverScopeData(scopeData);

        PsiType returnPsiType = method.getReturnType();
        if (returnPsiType == null) {
            return null;
        }

        // TODO: ugly
        if (method.getJetMethod().flags().get(JvmStdlibNames.FLAG_PROPERTY_BIT)) {
            return null;
        }

        if (scopeData.kotlin) {
            // TODO: unless maybe class explicitly extends Object
            String ownerClassName = method.getPsiMethod().getContainingClass().getQualifiedName();
            if (OBJECT_FQ_NAME.getFqName().equals(ownerClassName)) {
                return null;
            }
        }

        SimpleFunctionDescriptorImpl functionDescriptorImpl = new SimpleFunctionDescriptorImpl(
                scopeData.classOrNamespaceDescriptor,
                resolveAnnotations(method.getPsiMethod()),
                Name.identifier(method.getName()),
                DescriptorKindUtils.intToKind(method.getJetMethod().kind())
        );

        String context = "method " + method.getName() + " in class " + psiClass.getQualifiedName();

        List<TypeParameterDescriptor> methodTypeParameters = javaDescriptorSignatureResolver.resolveMethodTypeParameters(method, functionDescriptorImpl);

        TypeVariableResolver methodTypeVariableResolver = TypeVariableResolvers.typeVariableResolverFromTypeParameters(methodTypeParameters, functionDescriptorImpl, context);


        ValueParameterDescriptors valueParameterDescriptors = resolveParameterDescriptors(functionDescriptorImpl, method.getParameters(), methodTypeVariableResolver);
        JetType returnType = makeReturnType(returnPsiType, method, methodTypeVariableResolver);

        // TODO consider better place for this check
        AlternativeSignatureData alternativeSignatureData =
                new AlternativeSignatureData(method, valueParameterDescriptors, returnType, methodTypeParameters);
        if (!alternativeSignatureData.isNone() && alternativeSignatureData.getError() == null) {
            valueParameterDescriptors = alternativeSignatureData.getValueParameters();
            returnType = alternativeSignatureData.getReturnType();
            methodTypeParameters = alternativeSignatureData.getTypeParameters();
        }

        functionDescriptorImpl.initialize(
                valueParameterDescriptors.receiverType,
                DescriptorUtils.getExpectedThisObjectIfNeeded(scopeData.classOrNamespaceDescriptor),
                methodTypeParameters,
                valueParameterDescriptors.descriptors,
                returnType,
                resolveModality(method, method.isFinal()),
                resolveVisibility(method.getPsiMethod(), method.getJetMethod()),
                /*isInline = */ false
        );

        if (functionDescriptorImpl.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
            BindingContextUtils.recordFunctionDeclarationToDescriptor(tempTrace, method.getPsiMethod(), functionDescriptorImpl);
        }

        if (method.getPsiMethod().getContainingClass() != psiClass && !method.isStatic()) {
            throw new IllegalStateException("non-static method in subclass");
        }
        return functionDescriptorImpl;
    }

    private boolean isEnumSpecialMethod(@NotNull FunctionDescriptor functionDescriptor) {
        List<ValueParameterDescriptor> methodTypeParameters = functionDescriptor.getValueParameters();
        String methodName = functionDescriptor.getName().getName();
        JetType nullableString = TypeUtils.makeNullable(JetStandardLibrary.getInstance().getStringType());
        if (methodName.equals("valueOf") && methodTypeParameters.size() == 1
            && JetTypeChecker.INSTANCE.isSubtypeOf(methodTypeParameters.get(0).getType(), nullableString)) {
            return true;
        }
        return (methodName.equals("values") && methodTypeParameters.isEmpty());
    }

    private List<AnnotationDescriptor> resolveAnnotations(PsiModifierListOwner owner, @NotNull List<Runnable> tasks) {
        PsiAnnotation[] psiAnnotations = getAllAnnotations(owner);
        List<AnnotationDescriptor> r = Lists.newArrayListWithCapacity(psiAnnotations.length);
        for (PsiAnnotation psiAnnotation : psiAnnotations) {
            AnnotationDescriptor annotation = resolveAnnotation(psiAnnotation, tasks);
            if (annotation != null) {
                r.add(annotation);
            }
        }
        return r;
    }

    private List<AnnotationDescriptor> resolveAnnotations(PsiModifierListOwner owner) {
        List<Runnable> tasks = Lists.newArrayList();
        List<AnnotationDescriptor> annotations = resolveAnnotations(owner, tasks);
        for (Runnable task : tasks) {
            task.run();
        }
        return annotations;
    }

    @Nullable
    private AnnotationDescriptor resolveAnnotation(PsiAnnotation psiAnnotation, @NotNull List<Runnable> taskList) {
        final AnnotationDescriptor annotation = new AnnotationDescriptor();
        String qname = psiAnnotation.getQualifiedName();
        if (qname == null) {
            return null;
        }

        // Don't process internal jet annotations and jetbrains NotNull annotations
        if (qname.startsWith("jet.runtime.typeinfo.") || qname.equals(JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName())) {
            return null;
        }

        FqName annotationFqName = new FqName(qname);
        final ClassDescriptor clazz = resolveClass(annotationFqName, DescriptorSearchRule.INCLUDE_KOTLIN, taskList);
        if (clazz == null) {
            return null;
        }

        taskList.add(new Runnable() {
            @Override
            public void run() {
                annotation.setAnnotationType(clazz.getDefaultType());
            }
        });


        PsiAnnotationParameterList parameterList = psiAnnotation.getParameterList();
        for (PsiNameValuePair psiNameValuePair : parameterList.getAttributes()) {
            PsiAnnotationMemberValue value = psiNameValuePair.getValue();
            String name = psiNameValuePair.getName();
            if (name == null) name = "value";
            Name identifier = Name.identifier(name);

            CompileTimeConstant compileTimeConst = getCompileTimeConstFromExpression(annotationFqName, identifier, value, taskList);
            if (compileTimeConst != null) {
                ValueParameterDescriptor valueParameterDescriptor = getValueParameterDescriptorForAnnotationParameter(identifier, clazz);
                if (valueParameterDescriptor != null) {
                    annotation.setValueArgument(valueParameterDescriptor, compileTimeConst);
                }
            }
        }

        return annotation;
    }

    @Nullable
    public static ValueParameterDescriptor getValueParameterDescriptorForAnnotationParameter(
            Name argumentName,
            ClassDescriptor classDescriptor
    ) {
        Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
        assert constructors.size() == 1 : "Annotation class descriptor must have only one constructor";
        List<ValueParameterDescriptor> valueParameters = constructors.iterator().next().getValueParameters();

        for (ValueParameterDescriptor parameter : valueParameters) {
            Name parameterName = parameter.getName();
            if (parameterName.equals(argumentName)) {
                return parameter;
            }
        }
        return null;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromExpression(
            FqName annotationFqName, Name parameterName,
            PsiAnnotationMemberValue value, List<Runnable> taskList
    ) {
        if (value instanceof PsiLiteralExpression) {
            return getCompileTimeConstFromLiteralExpression((PsiLiteralExpression) value);
        }
        // Enum
        else if (value instanceof PsiReferenceExpression) {
            return getCompileTimeConstFromReferenceExpression((PsiReferenceExpression) value, taskList);
        }
        // Array
        else if (value instanceof PsiArrayInitializerMemberValue) {
            return getCompileTimeConstFromArrayExpression(annotationFqName, parameterName, (PsiArrayInitializerMemberValue) value, taskList);
        }
        // Annotation
        else if (value instanceof PsiAnnotation) {
            return getCompileTimeConstFromAnnotation((PsiAnnotation) value, taskList);
        }
        return null;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromAnnotation(PsiAnnotation value, List<Runnable> taskList) {
        AnnotationDescriptor annotationDescriptor = resolveAnnotation(value, taskList);
        if (annotationDescriptor != null) {
            return new AnnotationValue(annotationDescriptor);
        }
        return null;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromArrayExpression(
            FqName annotationFqName,
            Name valueName, PsiArrayInitializerMemberValue value,
            List<Runnable> taskList
    ) {
        PsiAnnotationMemberValue[] initializers = value.getInitializers();
        List<CompileTimeConstant<?>> values = getCompileTimeConstantForArrayValues(annotationFqName, valueName, taskList, initializers);

        ClassDescriptor classDescriptor = resolveClass(annotationFqName, DescriptorSearchRule.INCLUDE_KOTLIN, taskList);

        ValueParameterDescriptor valueParameterDescriptor = getValueParameterDescriptorForAnnotationParameter(valueName, classDescriptor);
        if (valueParameterDescriptor == null) {
            return null;
        }
        JetType expectedArrayType = valueParameterDescriptor.getType();
        return new ArrayValue(values, expectedArrayType);
    }

    private List<CompileTimeConstant<?>> getCompileTimeConstantForArrayValues(
            FqName annotationQualifiedName,
            Name valueName,
            List<Runnable> taskList,
            PsiAnnotationMemberValue[] initializers
    ) {
        List<CompileTimeConstant<?>> values = new ArrayList<CompileTimeConstant<?>>();
        for (PsiAnnotationMemberValue initializer : initializers) {
            CompileTimeConstant<?> compileTimeConstant =
                    getCompileTimeConstFromExpression(annotationQualifiedName, valueName, initializer, taskList);
            if (compileTimeConstant == null) {
                compileTimeConstant = NullValue.NULL;
            }
            values.add(compileTimeConstant);
        }
        return values;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromReferenceExpression(PsiReferenceExpression value, List<Runnable> taskList) {
        PsiElement resolveElement = value.resolve();
        if (resolveElement instanceof PsiEnumConstant) {
            PsiElement psiElement = resolveElement.getParent();
            if (psiElement instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) psiElement;
                String fqName = psiClass.getQualifiedName();
                if (fqName == null) {
                    return null;
                }

                JetScope scope;
                ClassDescriptor classDescriptor = resolveClass(new FqName(fqName), DescriptorSearchRule.INCLUDE_KOTLIN, taskList);
                if (classDescriptor == null) {
                    return null;
                }
                ClassDescriptor classObjectDescriptor = classDescriptor.getClassObjectDescriptor();
                if (classObjectDescriptor == null) {
                    return null;
                }
                scope = classObjectDescriptor.getMemberScope(Lists.<TypeProjection>newArrayList());

                Name identifier = Name.identifier(((PsiEnumConstant) resolveElement).getName());
                Collection<VariableDescriptor> properties = scope.getProperties(identifier);
                for (VariableDescriptor variableDescriptor : properties) {
                    if (!variableDescriptor.getReceiverParameter().exists()) {
                        return new EnumValue((PropertyDescriptor) variableDescriptor);
                    }
                }
                return null;
            }
        }
        return null;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromLiteralExpression(PsiLiteralExpression value) {
        Object literalValue = value.getValue();
        if (literalValue instanceof String) {
            return new StringValue((String) literalValue);
        }
        else if (literalValue instanceof Byte) {
            return new ByteValue((Byte) literalValue);
        }
        else if (literalValue instanceof Short) {
            return new ShortValue((Short) literalValue);
        }
        else if (literalValue instanceof Character) {
            return new CharValue((Character) literalValue);
        }
        else if (literalValue instanceof Integer) {
            return new IntValue((Integer) literalValue);
        }
        else if (literalValue instanceof Long) {
            return new LongValue((Long) literalValue);
        }
        else if (literalValue instanceof Float) {
            return new FloatValue((Float) literalValue);
        }
        else if (literalValue instanceof Double) {
            return new DoubleValue((Double) literalValue);
        }
        else if (literalValue == null) {
            return NullValue.NULL;
        }
        return null;
    }

    public List<FunctionDescriptor> resolveMethods(@NotNull ResolverScopeData scopeData) {

        getResolverScopeData(scopeData);

        TypeSubstitutor substitutorForGenericSupertypes = typeSubstitutorForGenericSupertypes(scopeData);

        List<FunctionDescriptor> functions = new ArrayList<FunctionDescriptor>();

        for (Map.Entry<Name, NamedMembers> entry : scopeData.namedMembersMap.entrySet()) {
            Name methodName = entry.getKey();
            NamedMembers namedMembers = entry.getValue();
            resolveNamedGroupFunctions(scopeData.classOrNamespaceDescriptor, scopeData.psiClass, substitutorForGenericSupertypes,
                                       namedMembers, methodName, scopeData);
            functions.addAll(namedMembers.functionDescriptors);
        }

        return functions;
    }

    private Collection<JetType> getSupertypes(ResolverScopeData scope) {
        if (scope instanceof ResolverClassData) {
            return ((ResolverClassData) scope).classDescriptor.getSupertypes();
        }
        else if (scope instanceof ResolverNamespaceData) {
            return Collections.emptyList();
        }
        else {
            throw new IllegalStateException();
        }
    }

    private TypeSubstitutor typeSubstitutorForGenericSupertypes(ResolverScopeData scopeData) {
        if (scopeData instanceof ResolverClassData) {
            return createSubstitutorForGenericSupertypes(((ResolverClassData) scopeData).getClassDescriptor());
        }
        else {
            return TypeSubstitutor.EMPTY;
        }
    }

    private JetType makeReturnType(PsiType returnType, PsiMethodWrapper method,
            @NotNull TypeVariableResolver typeVariableResolver) {

        String returnTypeFromAnnotation = method.getJetMethod().returnType();

        JetType transformedType;
        if (returnTypeFromAnnotation.length() > 0) {
            transformedType = semanticServices.getTypeTransformer().transformToType(returnTypeFromAnnotation, typeVariableResolver);
        }
        else {
            transformedType = semanticServices.getTypeTransformer().transformToType(
                    returnType, JavaTypeTransformer.TypeUsage.MEMBER_SIGNATURE_COVARIANT, typeVariableResolver);
        }
        if (method.getJetMethod().returnTypeNullable()) {
            return TypeUtils.makeNullableAsSpecified(transformedType, true);
        }
        else if (findAnnotation(method.getPsiMethod(), JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) != null) {
            return TypeUtils.makeNullableAsSpecified(transformedType, false);
        }
        else {
            return transformedType;
        }
    }

    private static Modality resolveModality(PsiMemberWrapper memberWrapper, boolean isFinal) {
        if (memberWrapper instanceof PsiMethodWrapper) {
            PsiMethodWrapper method = (PsiMethodWrapper) memberWrapper;
            if (method.getJetMethod().flags().get(JvmStdlibNames.FLAG_FORCE_OPEN_BIT)) {
                return Modality.OPEN;
            }
            if (method.getJetMethod().flags().get(JvmStdlibNames.FLAG_FORCE_FINAL_BIT)) {
                return Modality.FINAL;
            }
        }

        return Modality.convertFromFlags(memberWrapper.isAbstract(), !isFinal);
    }

    private static Visibility resolveVisibility(PsiModifierListOwner modifierListOwner,
            @Nullable PsiAnnotationWithFlags annotation) {
        if (annotation != null) {
            BitSet flags = annotation.flags();
            if (flags.get(JvmStdlibNames.FLAG_PRIVATE_BIT)) {
                return Visibilities.PRIVATE;
            }
            else if (flags.get(JvmStdlibNames.FLAG_INTERNAL_BIT)) {
                return Visibilities.INTERNAL;
            }
        }
        return modifierListOwner.hasModifierProperty(PsiModifier.PUBLIC) ? Visibilities.PUBLIC :
               (modifierListOwner.hasModifierProperty(PsiModifier.PRIVATE) ? Visibilities.PRIVATE :
                (modifierListOwner.hasModifierProperty(PsiModifier.PROTECTED) ? Visibilities.PROTECTED :
                 //Visibilities.PUBLIC));
                 PACKAGE_VISIBILITY));
    }

    public List<ClassDescriptor> resolveInnerClasses(DeclarationDescriptor owner, PsiClass psiClass, boolean staticMembers) {
        if (staticMembers) {
            return new ArrayList<ClassDescriptor>(0);
        }

        PsiClass[] innerPsiClasses = psiClass.getInnerClasses();
        List<ClassDescriptor> r = new ArrayList<ClassDescriptor>(innerPsiClasses.length);
        for (PsiClass innerPsiClass : innerPsiClasses) {
            if (innerPsiClass.hasModifierProperty(PsiModifier.PRIVATE)) {
                // TODO: hack against inner classes
                continue;
            }
            if (innerPsiClass.getName().equals(JvmAbi.CLASS_OBJECT_CLASS_NAME)) {
                continue;
            }
            ClassDescriptor classDescriptor = resolveClass(new FqName(innerPsiClass.getQualifiedName()),
                                                           DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
            assert classDescriptor != null: "couldn't resolve class " + innerPsiClass.getQualifiedName();
            r.add(classDescriptor);
        }
        return r;
    }

    @NotNull
    public static PsiAnnotation[] getAllAnnotations(@NotNull PsiModifierListOwner owner) {
        List<PsiAnnotation> result = new ArrayList<PsiAnnotation>();

        PsiModifierList list = owner.getModifierList();
        if (list != null) {
            result.addAll(Arrays.asList(list.getAnnotations()));
        }

        PsiAnnotation[] externalAnnotations = ExternalAnnotationsManager.getInstance(owner.getProject()).findExternalAnnotations(owner);
        if (externalAnnotations != null) {
            result.addAll(Arrays.asList(externalAnnotations));
        }

        return result.toArray(new PsiAnnotation[result.size()]);
    }

    @Nullable
    public static PsiAnnotation findAnnotation(@NotNull PsiModifierListOwner owner, @NotNull String fqName) {
        PsiModifierList list = owner.getModifierList();
        if (list != null) {
            PsiAnnotation found = list.findAnnotation(fqName);
            if (found != null) {
                return found;
            }
        }

        return ExternalAnnotationsManager.getInstance(owner.getProject()).findExternalAnnotation(owner, fqName);
    }
}
