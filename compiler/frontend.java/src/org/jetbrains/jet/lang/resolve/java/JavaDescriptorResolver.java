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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterListOwner;
import jet.typeinfo.TypeInfoVariance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorWithVisibility;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.MutableClassDescriptorLite;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.NamespaceFactory;
import org.jetbrains.jet.lang.resolve.NamespaceFactoryImpl;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.constants.ByteValue;
import org.jetbrains.jet.lang.resolve.constants.CharValue;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.DoubleValue;
import org.jetbrains.jet.lang.resolve.constants.FloatValue;
import org.jetbrains.jet.lang.resolve.constants.IntValue;
import org.jetbrains.jet.lang.resolve.constants.LongValue;
import org.jetbrains.jet.lang.resolve.constants.NullValue;
import org.jetbrains.jet.lang.resolve.constants.ShortValue;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.rt.signature.JetSignatureAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureExceptionsAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureReader;
import org.jetbrains.jet.rt.signature.JetSignatureVisitor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
public class JavaDescriptorResolver {
    
    public static final String JAVA_ROOT = "<java_root>";

    public static final ModuleDescriptor FAKE_ROOT_MODULE = new ModuleDescriptor(JAVA_ROOT);

    /*package*/ static final DeclarationDescriptor JAVA_METHOD_TYPE_PARAMETER_PARENT = new DeclarationDescriptorImpl(null, Collections.<AnnotationDescriptor>emptyList(), "<java_generic_method>") {

        @Override
        public DeclarationDescriptor substitute(TypeSubstitutor substitutor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
            return visitor.visitDeclarationDescriptor(this, data);
        }
    };

    /*package*/ static final DeclarationDescriptor JAVA_CLASS_OBJECT = new DeclarationDescriptorImpl(null, Collections.<AnnotationDescriptor>emptyList(), "<java_class_object_emulation>") {
        @NotNull
        @Override
        public DeclarationDescriptor substitute(TypeSubstitutor substitutor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
            return visitor.visitDeclarationDescriptor(this, data);
        }
    };

    private static Visibility PACKAGE_VISIBILITY = new Visibility("package", false) {
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

    private enum TypeParameterDescriptorOrigin {
        JAVA,
        KOTLIN,
    }

    public static class TypeParameterDescriptorInitialization {
        @NotNull
        private final TypeParameterDescriptorOrigin origin;
        @NotNull
        final TypeParameterDescriptor descriptor;
        final PsiTypeParameter psiTypeParameter;
        @Nullable
        private final List<JetType> upperBoundsForKotlin;
        @Nullable
        private final List<JetType> lowerBoundsForKotlin;

        private TypeParameterDescriptorInitialization(@NotNull TypeParameterDescriptor descriptor, @NotNull PsiTypeParameter psiTypeParameter) {
            this.origin = TypeParameterDescriptorOrigin.JAVA;
            this.descriptor = descriptor;
            this.psiTypeParameter = psiTypeParameter;
            this.upperBoundsForKotlin = null;
            this.lowerBoundsForKotlin = null;
        }

        private TypeParameterDescriptorInitialization(@NotNull TypeParameterDescriptor descriptor, @NotNull PsiTypeParameter psiTypeParameter,
                List<JetType> upperBoundsForKotlin, List<JetType> lowerBoundsForKotlin) {
            this.origin = TypeParameterDescriptorOrigin.KOTLIN;
            this.descriptor = descriptor;
            this.psiTypeParameter = psiTypeParameter;
            this.upperBoundsForKotlin = upperBoundsForKotlin;
            this.lowerBoundsForKotlin = lowerBoundsForKotlin;
        }
    }


    static abstract class ResolverScopeData {
        @Nullable
        final PsiClass psiClass;
        @Nullable
        final PsiPackage psiPackage;
        final FqName fqName;
        final boolean staticMembers;
        final boolean kotlin;
        final ClassOrNamespaceDescriptor classOrNamespaceDescriptor;

        protected ResolverScopeData(@Nullable PsiClass psiClass, @Nullable PsiPackage psiPackage, @NotNull FqName fqName, boolean staticMembers, @NotNull ClassOrNamespaceDescriptor descriptor) {
            checkPsiClassIsNotJet(psiClass);

            this.psiClass = psiClass;
            this.psiPackage = psiPackage;
            this.fqName = fqName;

            if (psiClass == null && psiPackage == null) {
                throw new IllegalStateException("both psiClass and psiPackage cannot be null");
            }

            if (fqName.lastSegmentIs(JvmAbi.PACKAGE_CLASS)) {
                throw new IllegalStateException("identified cannot have last segment " + JvmAbi.PACKAGE_CLASS + ": " + fqName);
            }

            this.staticMembers = staticMembers;
            this.kotlin = psiClass != null &&
                    (new PsiClassWrapper(psiClass).getJetClass().isDefined() || psiClass.getName().equals(JvmAbi.PACKAGE_CLASS));
            classOrNamespaceDescriptor = descriptor;
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

        private Map<String, NamedMembers> namedMembersMap;
        
        @NotNull
        public abstract List<TypeParameterDescriptor> getTypeParameters();
    }

    /** Class with instance members */
    static class ResolverBinaryClassData extends ResolverScopeData {
        private final MutableClassDescriptorLite classDescriptor;

        ResolverBinaryClassData(@NotNull PsiClass psiClass, @NotNull FqName fqName, @NotNull MutableClassDescriptorLite classDescriptor) {
            super(psiClass, null, fqName, false, classDescriptor);
            this.classDescriptor = classDescriptor;
        }

        private ResolverBinaryClassData(boolean negative) {
            super(negative);
            this.classDescriptor = null;
        }

        static final ResolverBinaryClassData NEGATIVE = new ResolverBinaryClassData(true);

        List<TypeParameterDescriptorInitialization> typeParameters;

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

    protected final Map<FqName, ResolverBinaryClassData> classDescriptorCache = Maps.newHashMap();
    protected final Map<FqName, ResolverNamespaceData> namespaceDescriptorCacheByFqn = Maps.newHashMap();

    protected Project project;
    protected JavaSemanticServices semanticServices;
    private NamespaceFactory namespaceFactory;
    private BindingTrace trace;
    private PsiClassFinder psiClassFinder;

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



    @Nullable
    private ClassDescriptor resolveJavaLangObject() {
        ClassDescriptor clazz = resolveClass(JdkNames.JL_OBJECT.getFqName(), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
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
        ResolverBinaryClassData classData = classDescriptorCache.get(qualifiedName);
        if (classData == null) {
            PsiClass psiClass = psiClassFinder.findPsiClass(qualifiedName, PsiClassFinder.RuntimeClassesHandleMode.THROW);
            if (psiClass == null) {
                ResolverBinaryClassData oldValue = classDescriptorCache.put(qualifiedName, ResolverBinaryClassData.NEGATIVE);
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
    private ResolverBinaryClassData createJavaClassDescriptor(@NotNull final PsiClass psiClass, List<Runnable> taskList) {
        FqName fqName = new FqName(psiClass.getQualifiedName());
        if (classDescriptorCache.containsKey(fqName)) {
            throw new IllegalStateException(psiClass.getQualifiedName());
        }

        checkPsiClassIsNotJet(psiClass);

        String name = psiClass.getName();
        ClassKind kind = psiClass.isInterface() ? (psiClass.isAnnotationType() ? ClassKind.ANNOTATION_CLASS : ClassKind.TRAIT) : ClassKind.CLASS;
        ClassOrNamespaceDescriptor containingDeclaration = resolveParentDescriptor(psiClass);

        // class may be resolved during resolution of parent
        ResolverBinaryClassData classData = classDescriptorCache.get(fqName);
        if (classData != null) {
            return classData;
        }

        classData = new ResolverBinaryClassData(psiClass, fqName, new MutableClassDescriptorLite(containingDeclaration, kind));
        classDescriptorCache.put(fqName, classData);
        classData.classDescriptor.setName(name);
        classData.classDescriptor.setAnnotations(resolveAnnotations(psiClass, taskList));

        List<JetType> supertypes = new ArrayList<JetType>();

        classData.typeParameters = createUninitializedClassTypeParameters(psiClass, classData);
        
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>();
        for (TypeParameterDescriptorInitialization typeParameter : classData.typeParameters) {
            typeParameters.add(typeParameter.descriptor);
        }
        
        classData.classDescriptor.setTypeParameterDescriptors(typeParameters);
        classData.classDescriptor.setSupertypes(supertypes);
        classData.classDescriptor.setVisibility(resolveVisibilityFromPsiModifiers(psiClass));
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

        initializeTypeParameters(classData.typeParameters, classData.classDescriptor, "class " + psiClass.getQualifiedName());

        TypeVariableResolver resolverForTypeParameters = TypeVariableResolvers.classTypeVariableResolver(
                classData.classDescriptor,
                "class " + psiClass.getQualifiedName());

        // TODO: ugly hack: tests crash if initializeTypeParameters called with class containing proper supertypes
        supertypes.addAll(getSupertypes(new PsiClassWrapper(psiClass), classData, classData.getTypeParameters()));

        PsiMethod[] psiConstructors = psiClass.getConstructors();

        boolean isStatic = psiClass.hasModifierProperty(PsiModifier.STATIC);
        if (psiConstructors.length == 0) {
            // We need to create default constructors for classes and abstract classes.
            // Example:
            // class Kotlin() : Java() {}
            // abstract public class Java {}
            if (!psiClass.isInterface()) {
                ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                        classData.classDescriptor,
                        Collections.<AnnotationDescriptor>emptyList(),
                        false);
                constructorDescriptor.initialize(typeParameters, Collections.<ValueParameterDescriptor>emptyList(), classData.classDescriptor.getVisibility(), isStatic);
                constructorDescriptor.setReturnType(classData.classDescriptor.getDefaultType());
                classData.classDescriptor.addConstructor(constructorDescriptor, null);
                trace.record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
            }
            if (psiClass.isAnnotationType()) {
                // A constructor for an annotation type takes all the "methods" in the @interface as parameters
                ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                        classData.classDescriptor,
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
                                method.getName(),
                                false,
                                semanticServices.getTypeTransformer().transformToType(returnType, resolverForTypeParameters),
                                annotationMethod.getDefaultValue() != null,
                                varargElementType));
                    }
                }

                constructorDescriptor.initialize(typeParameters, valueParameters, classData.classDescriptor.getVisibility(), isStatic);
                constructorDescriptor.setReturnType(classData.classDescriptor.getDefaultType());
                classData.classDescriptor.addConstructor(constructorDescriptor, null);
                trace.record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
            }
        }
        else {
            for (PsiMethod psiConstructor : psiConstructors) {
                PsiMethodWrapper constructor = new PsiMethodWrapper(psiConstructor);

                if (constructor.getJetConstructor().hidden()) {
                    continue;
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
                constructorDescriptor.initialize(typeParameters, valueParameterDescriptors.descriptors,
                        resolveVisibilityFromPsiModifiers(psiConstructor), isStatic);
                constructorDescriptor.setReturnType(classData.classDescriptor.getDefaultType());
                classData.classDescriptor.addConstructor(constructorDescriptor, null);
                trace.record(BindingContext.CONSTRUCTOR, psiConstructor, constructorDescriptor);
            }
        }

        MutableClassDescriptorLite classObject = createClassObjectDescriptor(classData.classDescriptor, psiClass);
        if (classObject != null) {
            classData.classDescriptor.setClassObjectDescriptor(classObject);
        }

        trace.record(BindingContext.CLASS, psiClass, classData.classDescriptor);

        return classData;
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
        PsiClass classObjectPsiClass = getInnerClassClassObject(psiClass);
        if (classObjectPsiClass == null) {
            return null;
        }

        checkPsiClassIsNotJet(psiClass);

        FqName fqName = new FqName(classObjectPsiClass.getQualifiedName());
        ResolverBinaryClassData classData = new ResolverBinaryClassData(classObjectPsiClass, fqName, new MutableClassDescriptorLite(containing, ClassKind.OBJECT));

        classDescriptorCache.put(fqName, classData);

        classData.classDescriptor.setSupertypes(getSupertypes(new PsiClassWrapper(classObjectPsiClass), classData, new ArrayList<TypeParameterDescriptor>(0)));
        classData.classDescriptor.setName(JetPsiUtil.NO_NAME_PROVIDED); // TODO
        classData.classDescriptor.setModality(Modality.FINAL);
        classData.classDescriptor.setVisibility(containing.getVisibility());
        classData.classDescriptor.setTypeParameterDescriptors(new ArrayList<TypeParameterDescriptor>(0));
        classData.classDescriptor.createTypeConstructor();
        classData.classDescriptor.setScopeForMemberLookup(new JavaClassMembersScope(semanticServices, classData));

        // TODO: wrong: class objects do not need visible constructors
        ConstructorDescriptorImpl constructor = new ConstructorDescriptorImpl(classData.classDescriptor, new ArrayList<AnnotationDescriptor>(0), true);
        constructor.setReturnType(classData.classDescriptor.getDefaultType());
        constructor.initialize(new ArrayList<TypeParameterDescriptor>(0), new ArrayList<ValueParameterDescriptor>(0), Visibilities.PUBLIC);

        classData.classDescriptor.addConstructor(constructor, null);
        return classData.classDescriptor;
    }

    private List<TypeParameterDescriptorInitialization> createUninitializedClassTypeParameters(PsiClass psiClass, ResolverBinaryClassData classData) {
        JetClassAnnotation jetClassAnnotation = JetClassAnnotation.get(psiClass);

        if (jetClassAnnotation.signature().length() > 0) {
            return resolveClassTypeParametersFromJetSignature(
                    jetClassAnnotation.signature(), psiClass, classData.classDescriptor);
        }

        return makeUninitializedTypeParameters(classData.classDescriptor, psiClass.getTypeParameters());
    }

    @NotNull
    private PsiTypeParameter getPsiTypeParameterByName(PsiTypeParameterListOwner clazz, String name) {
        for (PsiTypeParameter typeParameter : clazz.getTypeParameters()) {
            if (typeParameter.getName().equals(name)) {
                return typeParameter; 
            }
        }
        throw new IllegalStateException("PsiTypeParameter '" + name + "' is not found");
    }


    private static final FqName JL_OBJECT = new FqName("java.lang.Object");

    private boolean isJavaLangObject(JetType type) {
        ClassifierDescriptor classifierDescriptor = type.getConstructor().getDeclarationDescriptor();
        return classifierDescriptor instanceof ClassDescriptor &&
               DescriptorUtils.getFQName(classifierDescriptor).equals(JL_OBJECT.toUnsafe());
    }


    private abstract class JetSignatureTypeParameterVisitor extends JetSignatureExceptionsAdapter {

        @NotNull
        private final PsiTypeParameterListOwner psiOwner;
        @NotNull
        private final String name;
        @NotNull
        private final TypeVariableResolver typeVariableResolver;
        @NotNull
        private final TypeParameterDescriptor typeParameterDescriptor;

        protected JetSignatureTypeParameterVisitor(PsiTypeParameterListOwner psiOwner,
                String name, TypeVariableResolver typeVariableResolver, TypeParameterDescriptor typeParameterDescriptor)
        {
            if (name.isEmpty()) {
                throw new IllegalStateException();
            }

            this.psiOwner = psiOwner;
            this.name = name;
            this.typeVariableResolver = typeVariableResolver;
            this.typeParameterDescriptor = typeParameterDescriptor;
        }

        List<JetType> upperBounds = new ArrayList<JetType>();
        List<JetType> lowerBounds = new ArrayList<JetType>();
        
        @Override
        public JetSignatureVisitor visitClassBound() {
            return new JetTypeJetSignatureReader(semanticServices, JetStandardLibrary.getInstance(), typeVariableResolver) {
                @Override
                protected void done(@NotNull JetType jetType) {
                    if (isJavaLangObject(jetType)) {
                        return;
                    }
                    upperBounds.add(jetType);
                }
            };
        }

        @Override
        public JetSignatureVisitor visitInterfaceBound() {
            return new JetTypeJetSignatureReader(semanticServices, JetStandardLibrary.getInstance(), typeVariableResolver) {
                @Override
                protected void done(@NotNull JetType jetType) {
                    upperBounds.add(jetType);
                }
            };
        }

        @Override
        public void visitFormalTypeParameterEnd() {
            PsiTypeParameter psiTypeParameter = getPsiTypeParameterByName(psiOwner, name);
            TypeParameterDescriptorInitialization typeParameterDescriptorInitialization = new TypeParameterDescriptorInitialization(typeParameterDescriptor, psiTypeParameter, upperBounds, lowerBounds);
            done(typeParameterDescriptorInitialization);
        }
        
        protected abstract void done(@NotNull TypeParameterDescriptorInitialization typeParameterDescriptor);
    }

    private class JetSignatureTypeParametersVisitor extends JetSignatureExceptionsAdapter {
        @NotNull
        private final DeclarationDescriptor containingDeclaration;
        @NotNull
        private final PsiTypeParameterListOwner psiOwner;

        private final List<TypeParameterDescriptor> previousTypeParameters = new ArrayList<TypeParameterDescriptor>();
        // note changes state in this method
        private final TypeVariableResolver typeVariableResolver;


        private JetSignatureTypeParametersVisitor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameterListOwner psiOwner, @NotNull String context) {
            this.containingDeclaration = containingDeclaration;
            this.psiOwner = psiOwner;

            this.typeVariableResolver = TypeVariableResolvers.typeVariableResolverFromTypeParameters(
                    previousTypeParameters,
                    containingDeclaration,
                    context);
        }

        private int formalTypeParameterIndex = 0;


        List<TypeParameterDescriptorInitialization> r = new ArrayList<TypeParameterDescriptorInitialization>();

        @Override
        public JetSignatureVisitor visitFormalTypeParameter(final String name, final TypeInfoVariance variance, boolean reified) {
            TypeParameterDescriptor typeParameter = TypeParameterDescriptor.createForFurtherModification(
                    containingDeclaration,
                    Collections.<AnnotationDescriptor>emptyList(), // TODO: wrong
                    reified,
                    JetSignatureUtils.translateVariance(variance),
                    name,
                    formalTypeParameterIndex++);

            previousTypeParameters.add(typeParameter);

            return new JetSignatureTypeParameterVisitor(psiOwner, name, typeVariableResolver, typeParameter) {
                @Override
                protected void done(@NotNull TypeParameterDescriptorInitialization typeParameterDescriptor) {
                    r.add(typeParameterDescriptor);
                    previousTypeParameters.add(typeParameterDescriptor.descriptor);
                }
            };
        }
    }

    /**
     * @see #resolveMethodTypeParametersFromJetSignature(String, com.intellij.psi.PsiMethod, org.jetbrains.jet.lang.descriptors.DeclarationDescriptor)
     */
    private List<TypeParameterDescriptorInitialization> resolveClassTypeParametersFromJetSignature(String jetSignature,
            final PsiClass clazz, final ClassDescriptor classDescriptor) {
        String context = "class " + clazz.getQualifiedName();
        JetSignatureTypeParametersVisitor jetSignatureTypeParametersVisitor = new JetSignatureTypeParametersVisitor(classDescriptor, clazz, context) {
            @Override
            public JetSignatureVisitor visitSuperclass() {
                // TODO
                return new JetSignatureAdapter();
            }

            @Override
            public JetSignatureVisitor visitInterface() {
                // TODO
                return new JetSignatureAdapter();
            }
        };
        new JetSignatureReader(jetSignature).accept(jetSignatureTypeParametersVisitor);
        return jetSignatureTypeParametersVisitor.r;
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

    private List<TypeParameterDescriptorInitialization> makeUninitializedTypeParameters(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameter[] typeParameters) {
        List<TypeParameterDescriptorInitialization> result = Lists.newArrayList();
        for (PsiTypeParameter typeParameter : typeParameters) {
            TypeParameterDescriptorInitialization typeParameterDescriptor = makeUninitializedTypeParameter(containingDeclaration, typeParameter);
            result.add(typeParameterDescriptor);
        }
        return result;
    }

    @NotNull
    private TypeParameterDescriptorInitialization makeUninitializedTypeParameter(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameter psiTypeParameter) {
        TypeParameterDescriptor typeParameterDescriptor = TypeParameterDescriptor.createForFurtherModification(
                containingDeclaration,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                false,
                Variance.INVARIANT,
                psiTypeParameter.getName(),
                psiTypeParameter.getIndex()
        );
        return new TypeParameterDescriptorInitialization(typeParameterDescriptor, psiTypeParameter);
    }

    private void initializeTypeParameter(TypeParameterDescriptorInitialization typeParameter, TypeVariableResolver typeVariableByPsiResolver) {
        TypeParameterDescriptor typeParameterDescriptor = typeParameter.descriptor;
        if (typeParameter.origin == TypeParameterDescriptorOrigin.KOTLIN) {
            List<?> upperBounds = typeParameter.upperBoundsForKotlin;
            if (upperBounds.size() == 0){
                typeParameterDescriptor.addUpperBound(JetStandardClasses.getNullableAnyType());
            }
            else {
                for (JetType upperBound : typeParameter.upperBoundsForKotlin) {
                    typeParameterDescriptor.addUpperBound(upperBound);
                }
            }

            // TODO: lower bounds
        }
        else {
            PsiClassType[] referencedTypes = typeParameter.psiTypeParameter.getExtendsList().getReferencedTypes();
            if (referencedTypes.length == 0){
                typeParameterDescriptor.addUpperBound(JetStandardClasses.getNullableAnyType());
            }
            else if (referencedTypes.length == 1) {
                typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer().transformToType(referencedTypes[0], JavaTypeTransformer.TypeUsage.UPPER_BOUND, typeVariableByPsiResolver));
            }
            else {
                for (PsiClassType referencedType : referencedTypes) {
                    typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer().transformToType(referencedType, JavaTypeTransformer.TypeUsage.UPPER_BOUND, typeVariableByPsiResolver));
                }
            }
        }
        typeParameterDescriptor.setInitialized();
    }

    private void initializeTypeParameters(List<TypeParameterDescriptorInitialization> typeParametersInitialization, @NotNull DeclarationDescriptor typeParametersOwner, @NotNull String context) {
        List<TypeParameterDescriptor> prevTypeParameters = new ArrayList<TypeParameterDescriptor>();
        for (TypeParameterDescriptorInitialization psiTypeParameter : typeParametersInitialization) {
            prevTypeParameters.add(psiTypeParameter.descriptor);
            initializeTypeParameter(psiTypeParameter,
                    TypeVariableResolvers.typeVariableResolverFromTypeParameters(prevTypeParameters, typeParametersOwner, context));
        }
    }

    private Collection<JetType> getSupertypes(PsiClassWrapper psiClass, ResolverBinaryClassData classData, List<TypeParameterDescriptor> typeParameters) {
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
            transformSupertypeList(result, psiClass.getPsiClass().getExtendsListTypes(), typeVariableResolverForSupertypes, classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS);
            transformSupertypeList(result, psiClass.getPsiClass().getImplementsListTypes(), typeVariableResolverForSupertypes, classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS);
        }
        
        for (JetType supertype : result) {
            if (ErrorUtils.isErrorType(supertype)) {
                trace.record(BindingContext.INCOMPLETE_HIERARCHY, classDescriptor);
            }
        }
        
        if (result.isEmpty()) {
            if (classData.kotlin
                    || psiClass.getQualifiedName().equals(JdkNames.JL_OBJECT.getFqName().getFqName())
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

    private void transformSupertypeList(List<JetType> result, PsiClassType[] extendsListTypes, TypeVariableResolver typeVariableResolver, boolean annotation) {
        for (PsiClassType type : extendsListTypes) {
            PsiClass resolved = type.resolve();
            if (resolved != null && resolved.getQualifiedName().equals(JvmStdlibNames.JET_OBJECT.getFqName().getFqName())) {
                continue;
            }
            if (annotation && resolved.getQualifiedName().equals("java.lang.annotation.Annotation")) {
                continue;
            }
            
            JetType transform = semanticServices.getTypeTransformer().transformToType(type, typeVariableResolver);

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
                qualifiedName.isRoot() ? "<root>" : qualifiedName.shortName(),
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
            if (psiClass != null) {
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
        JavaPackageScope scope = resolverNamespaceData.memberScope;
        if (scope == null) {
            throw new IllegalStateException("fqn: " + fqName);
        }
        return scope;
    }

    @Nullable
    private PsiClass getPsiClassForJavaPackageScope(@NotNull FqName packageFQN) {
        return psiClassFinder.findPsiClass(packageFQN.child(JvmAbi.PACKAGE_CLASS), PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
    }

    private static class ValueParameterDescriptors {
        private final JetType receiverType;
        private final List<ValueParameterDescriptor> descriptors;

        private ValueParameterDescriptors(@Nullable JetType receiverType, List<ValueParameterDescriptor> descriptors) {
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
        String name = parameter.getPsiParameter().getName() != null ? parameter.getPsiParameter().getName() : "p" + i;

        if (parameter.getJetValueParameter().name().length() > 0) {
            name = parameter.getJetValueParameter().name();
        }
        
        String typeFromAnnotation = parameter.getJetValueParameter().type();
        boolean receiver = parameter.getJetValueParameter().receiver();
        boolean hasDefaultValue = parameter.getJetValueParameter().hasDefaultValue();

        JetType outType;
        if (typeFromAnnotation.length() > 0) {
            outType = semanticServices.getTypeTransformer().transformToType(typeFromAnnotation, typeVariableResolver);
        }
        else {
            outType = semanticServices.getTypeTransformer().transformToType(psiType, typeVariableResolver);
        }

        JetType varargElementType;
        if (psiType instanceof PsiEllipsisType) {
            varargElementType = JetStandardLibrary.getInstance().getArrayElementType(outType);
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
            else if (parameter.getPsiParameter().getModifierList().findAnnotation(JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) != null) {
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

    public Set<VariableDescriptor> resolveFieldGroupByName(@NotNull String fieldName, @NotNull ResolverScopeData scopeData) {

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
        Map<String, NamedMembers> membersForProperties = scopeData.namedMembersMap;
        for (Map.Entry<String, NamedMembers> entry : membersForProperties.entrySet()) {
            NamedMembers namedMembers = entry.getValue();
            String propertyName = entry.getKey();

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
            @NotNull NamedMembers namedMembers, @NotNull String propertyName,
            @NotNull String context) {
        getResolverScopeData(scopeData);

        if (namedMembers.propertyDescriptors != null) {
            return;
        }
        
        if (namedMembers.propertyAccessors == null) {
            namedMembers.propertyAccessors = Collections.emptyList();
        }

        TypeVariableResolver typeVariableResolver = TypeVariableResolvers.classTypeVariableResolver(owner, context);

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

            Modality modality;
            if (isFinal) {
                modality = Modality.FINAL;
            }
            else {
                modality = anyMember.getMember().isAbstract() ? Modality.ABSTRACT : Modality.OPEN;
            }


            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                    owner,
                    resolveAnnotations(anyMember.getMember().psiMember),
                    modality,
                    resolveVisibilityFromPsiModifiers(anyMember.getMember().psiMember),
                    isVar,
                    false,
                    propertyName,
                    CallableMemberDescriptor.Kind.DECLARATION);

            PropertyGetterDescriptor getterDescriptor = null;
            PropertySetterDescriptor setterDescriptor = null;
            if (members.getter != null) {
                getterDescriptor = new PropertyGetterDescriptor(propertyDescriptor, resolveAnnotations(members.getter.getMember().psiMember), Modality.OPEN, Visibilities.PUBLIC, true, false, CallableMemberDescriptor.Kind.DECLARATION);
            }
            if (members.setter != null) {
                setterDescriptor = new PropertySetterDescriptor(propertyDescriptor, resolveAnnotations(members.setter.getMember().psiMember), Modality.OPEN, Visibilities.PUBLIC, true, false, CallableMemberDescriptor.Kind.DECLARATION);
            }

            propertyDescriptor.initialize(getterDescriptor, setterDescriptor);

            List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(0);

            if (members.setter != null) {
                PsiMethodWrapper method = (PsiMethodWrapper) members.setter.getMember();

                if (anyMember == members.setter) {
                    typeParameters = resolveMethodTypeParameters(method, propertyDescriptor, typeVariableResolver);
                }
            }
            if (members.getter != null) {
                PsiMethodWrapper method = (PsiMethodWrapper) members.getter.getMember();

                if (anyMember == members.getter) {
                    typeParameters = resolveMethodTypeParameters(method, propertyDescriptor, typeVariableResolver);
                }
            }

            TypeVariableResolver typeVariableResolverForPropertyInternals = TypeVariableResolvers.typeVariableResolverFromTypeParameters(typeParameters, propertyDescriptor, "property " + propertyName + " in " + context);

            JetType propertyType;
            if (anyMember.getType().getTypeString().length() > 0) {
                propertyType = semanticServices.getTypeTransformer().transformToType(anyMember.getType().getTypeString(), typeVariableResolverForPropertyInternals);
            }
            else {
                propertyType = semanticServices.getTypeTransformer().transformToType(anyMember.getType().getPsiType(), typeVariableResolverForPropertyInternals);
                if (anyMember.getType().getPsiNotNullOwner().getModifierList().findAnnotation(JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) != null) {
                    propertyType = TypeUtils.makeNullableAsSpecified(propertyType, false);
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
                    DescriptorUtils.getExpectedThisObjectIfNeeded(owner),
                    receiverType
            );
            if (getterDescriptor != null) {
                getterDescriptor.initialize(propertyType);
            }
            if (setterDescriptor != null) {
                setterDescriptor.initialize(new ValueParameterDescriptorImpl(setterDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), "p0"/*TODO*/, false, propertyDescriptor.getType(), false, null));
            }

            trace.record(BindingContext.VARIABLE, anyMember.getMember().psiMember, propertyDescriptor);
            
            propertiesFromCurrent.add(propertyDescriptor);
        }


        Set<PropertyDescriptor> propertiesFromSupertypes = getPropertiesFromSupertypes(scopeData, propertyName);

        final Set<VariableDescriptor> properties = Sets.newHashSet();

        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            OverrideResolver.generateOverridesInFunctionGroup(propertyName, null, propertiesFromSupertypes, propertiesFromCurrent, classDescriptor, new OverrideResolver.DescriptorSink() {
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

    private void resolveNamedGroupFunctions(@NotNull ClassOrNamespaceDescriptor owner, PsiClass psiClass,
            TypeSubstitutor typeSubstitutorForGenericSuperclasses, NamedMembers namedMembers, String methodName, ResolverScopeData scopeData) {
        if (namedMembers.functionDescriptors != null) {
            return;
        }
        
        final Set<FunctionDescriptor> functions = new HashSet<FunctionDescriptor>();

        Set<SimpleFunctionDescriptor> functionsFromCurrent = Sets.newHashSet();
        for (PsiMethodWrapper method : namedMembers.methods) {
            FunctionDescriptorImpl function = resolveMethodToFunctionDescriptor(psiClass, method, scopeData);
            if (function != null) {
                functionsFromCurrent.add((SimpleFunctionDescriptor) function);
            }
        }

        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            Set<SimpleFunctionDescriptor> functionsFromSupertypes = getFunctionsFromSupertypes(scopeData, methodName);

            OverrideResolver.generateOverridesInFunctionGroup(methodName, null, functionsFromSupertypes, functionsFromCurrent, classDescriptor, new OverrideResolver.DescriptorSink() {
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

        namedMembers.functionDescriptors = functions;
    }
    
    private Set<SimpleFunctionDescriptor> getFunctionsFromSupertypes(ResolverScopeData scopeData, String methodName) {
        Set<SimpleFunctionDescriptor> r = new HashSet<SimpleFunctionDescriptor>();
        for (JetType supertype : getSupertypes(scopeData)) {
            for (FunctionDescriptor function : supertype.getMemberScope().getFunctions(methodName)) {
                r.add((SimpleFunctionDescriptor) function);
            }
        }
        return r;
    }

    private Set<PropertyDescriptor> getPropertiesFromSupertypes(ResolverScopeData scopeData, String propertyName) {
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
    public Set<FunctionDescriptor> resolveFunctionGroup(@NotNull String methodName, @NotNull ResolverScopeData scopeData) {

        getResolverScopeData(scopeData);

        Map<String, NamedMembers> namedMembersMap = scopeData.namedMembersMap;

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
            typeSubstitutor = TypeUtils.buildDeepSubstitutor(classDescriptor.getDefaultType());
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
                    throw new IllegalStateException("more then one receiver");
                }
                --indexDelta;
                receiverType = meaning.receiverType;
            }
        }
        return new ValueParameterDescriptors(receiverType, result);
    }

    @Nullable
    private FunctionDescriptorImpl resolveMethodToFunctionDescriptor(
            @NotNull final PsiClass psiClass, final PsiMethodWrapper method,
            @NotNull ResolverScopeData scopeData) {

        getResolverScopeData(scopeData);

        PsiType returnType = method.getReturnType();
        if (returnType == null) {
            return null;
        }

        // TODO: ugly
        if (method.getJetMethod().kind() == JvmStdlibNames.JET_METHOD_KIND_PROPERTY) {
            return null;
        }

        if (scopeData.kotlin) {
            // TODO: unless maybe class explicitly extends Object
            String ownerClassName = method.getPsiMethod().getContainingClass().getQualifiedName();
            if (ownerClassName.equals("java.lang.Object")) {
                return null;
            }
        }

        SimpleFunctionDescriptorImpl functionDescriptorImpl = new SimpleFunctionDescriptorImpl(
                scopeData.classOrNamespaceDescriptor,
                resolveAnnotations(method.getPsiMethod()),
                method.getName(),
                CallableMemberDescriptor.Kind.DECLARATION
        );

        String context = "method " + method.getName() + " in class " + psiClass.getQualifiedName();

        final TypeVariableResolver typeVariableResolverForParameters = TypeVariableResolvers.classTypeVariableResolver(scopeData.classOrNamespaceDescriptor, context);

        final List<TypeParameterDescriptor> methodTypeParameters = resolveMethodTypeParameters(method, functionDescriptorImpl, typeVariableResolverForParameters);

        TypeVariableResolver methodTypeVariableResolver = TypeVariableResolvers.typeVariableResolverFromTypeParameters(methodTypeParameters, functionDescriptorImpl, context);


        ValueParameterDescriptors valueParameterDescriptors = resolveParameterDescriptors(functionDescriptorImpl, method.getParameters(), methodTypeVariableResolver);
        functionDescriptorImpl.initialize(
                valueParameterDescriptors.receiverType,
                DescriptorUtils.getExpectedThisObjectIfNeeded(scopeData.classOrNamespaceDescriptor),
                methodTypeParameters,
                valueParameterDescriptors.descriptors,
                makeReturnType(returnType, method, methodTypeVariableResolver),
                Modality.convertFromFlags(method.getPsiMethod().hasModifierProperty(PsiModifier.ABSTRACT), !method.isFinal()),
                resolveVisibilityFromPsiModifiers(method.getPsiMethod()),
                /*isInline = */ false
        );
        trace.record(BindingContext.FUNCTION, method.getPsiMethod(), functionDescriptorImpl);
        BindingContextUtils.recordFunctionDeclarationToDescriptor(trace, method.getPsiMethod(), functionDescriptorImpl);
        FunctionDescriptor substitutedFunctionDescriptor = functionDescriptorImpl;
        if (method.getPsiMethod().getContainingClass() != psiClass && !method.isStatic()) {
            throw new IllegalStateException("non-static method in subclass");
        }
        return (FunctionDescriptorImpl) substitutedFunctionDescriptor;
    }

    private List<AnnotationDescriptor> resolveAnnotations(PsiModifierListOwner owner, @NotNull List<Runnable> tasks) {
        PsiAnnotation[] psiAnnotations = owner.getModifierList().getAnnotations();
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
        if (qname.startsWith("java.lang.annotation.") || qname.startsWith("jet.runtime.typeinfo.") || qname.equals(JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName())) {
            // TODO
            return null;
        }

        final ClassDescriptor clazz = resolveClass(new FqName(psiAnnotation.getQualifiedName()), DescriptorSearchRule.INCLUDE_KOTLIN, taskList);
        if (clazz == null) {
            return null;
        }
        taskList.add(new Runnable() {
            @Override
            public void run() {
                annotation.setAnnotationType(clazz.getDefaultType());
            }
        });
        ArrayList<CompileTimeConstant<?>> valueArguments = new ArrayList<CompileTimeConstant<?>>();

        PsiAnnotationParameterList parameterList = psiAnnotation.getParameterList();
        for (PsiNameValuePair psiNameValuePair : parameterList.getAttributes()) {
            PsiAnnotationMemberValue value = psiNameValuePair.getValue();
            if (!(value instanceof PsiLiteralExpression)) {
                // todo
                continue;
            }
            Object literalValue = ((PsiLiteralExpression) value).getValue();
            if(literalValue instanceof String)
                valueArguments.add(new StringValue((String) literalValue));
            else if(literalValue instanceof Byte)
                valueArguments.add(new ByteValue((Byte) literalValue));
            else if(literalValue instanceof Short)
                valueArguments.add(new ShortValue((Short) literalValue));
            else if(literalValue instanceof Character)
                valueArguments.add(new CharValue((Character) literalValue));
            else if(literalValue instanceof Integer)
                valueArguments.add(new IntValue((Integer) literalValue));
            else if(literalValue instanceof Long)
                valueArguments.add(new LongValue((Long) literalValue));
            else if(literalValue instanceof Float)
                valueArguments.add(new FloatValue((Float) literalValue));
            else if(literalValue instanceof Double)
                valueArguments.add(new DoubleValue((Double) literalValue));
            else if(literalValue == null)
                valueArguments.add(NullValue.NULL);
        }

        annotation.setValueArguments(valueArguments); // TODO
        return annotation;
    }

    public List<FunctionDescriptor> resolveMethods(@NotNull ResolverScopeData scopeData) {

        getResolverScopeData(scopeData);

        TypeSubstitutor substitutorForGenericSupertypes = typeSubstitutorForGenericSupertypes(scopeData);

        List<FunctionDescriptor> functions = new ArrayList<FunctionDescriptor>();

        for (Map.Entry<String, NamedMembers> entry : scopeData.namedMembersMap.entrySet()) {
            String methodName = entry.getKey();
            NamedMembers namedMembers = entry.getValue();
            resolveNamedGroupFunctions(scopeData.classOrNamespaceDescriptor, scopeData.psiClass, substitutorForGenericSupertypes, namedMembers, methodName, scopeData);
            functions.addAll(namedMembers.functionDescriptors);
        }

        return functions;
    }

    private Collection<JetType> getSupertypes(ResolverScopeData scope) {
        if (scope instanceof ResolverBinaryClassData) {
            return ((ResolverBinaryClassData) scope).classDescriptor.getSupertypes();
        }
        else if (scope instanceof ResolverNamespaceData) {
            return Collections.emptyList();
        }
        else {
            throw new IllegalStateException();
        }
    }

    private TypeSubstitutor typeSubstitutorForGenericSupertypes(ResolverScopeData scopeData) {
        if (scopeData instanceof ResolverBinaryClassData) {
            return createSubstitutorForGenericSupertypes(((ResolverBinaryClassData) scopeData).getClassDescriptor());
        }
        else {
            return TypeSubstitutor.EMPTY;
        }
    }

    private List<TypeParameterDescriptor> resolveMethodTypeParameters(
            @NotNull PsiMethodWrapper method,
            @NotNull DeclarationDescriptor functionDescriptor,
            @NotNull TypeVariableResolver classTypeVariableResolver) {

        List<TypeParameterDescriptorInitialization> typeParametersIntialization;
        if (method.getJetMethod().typeParameters().length() > 0) {
            typeParametersIntialization = resolveMethodTypeParametersFromJetSignature(
                    method.getJetMethod().typeParameters(), method.getPsiMethod(), functionDescriptor);
        }
        else {
            typeParametersIntialization = makeUninitializedTypeParameters(functionDescriptor, method.getPsiMethod().getTypeParameters());
        }

        String context = "method " + method.getName() + " in class " + method.getPsiMethod().getContainingClass().getQualifiedName();
        initializeTypeParameters(typeParametersIntialization, functionDescriptor, context);
        
        List<TypeParameterDescriptor> typeParameters = Lists.newArrayListWithCapacity(typeParametersIntialization.size());
        
        for (TypeParameterDescriptorInitialization tpdi : typeParametersIntialization) {
            typeParameters.add(tpdi.descriptor);
        }
        
        return typeParameters;
    }

    /**
     * @see #resolveClassTypeParametersFromJetSignature(String, com.intellij.psi.PsiClass, org.jetbrains.jet.lang.descriptors.ClassDescriptor)
     */
    private List<TypeParameterDescriptorInitialization> resolveMethodTypeParametersFromJetSignature(String jetSignature,
            final PsiMethod method, final DeclarationDescriptor functionDescriptor)
    {
        String context = "method " + method.getName() + " in class " + method.getContainingClass().getQualifiedName();
        JetSignatureTypeParametersVisitor jetSignatureTypeParametersVisitor = new JetSignatureTypeParametersVisitor(functionDescriptor, method, context);
        new JetSignatureReader(jetSignature).acceptFormalTypeParametersOnly(jetSignatureTypeParametersVisitor);
        return jetSignatureTypeParametersVisitor.r;
    }

    private JetType makeReturnType(PsiType returnType, PsiMethodWrapper method,
            @NotNull TypeVariableResolver typeVariableResolver) {

        String returnTypeFromAnnotation = method.getJetMethod().returnType();

        JetType transformedType;
        if (returnTypeFromAnnotation.length() > 0) {
            transformedType = semanticServices.getTypeTransformer().transformToType(returnTypeFromAnnotation, typeVariableResolver);
        }
        else {
            transformedType = semanticServices.getTypeTransformer().transformToType(returnType, typeVariableResolver);
        }
        if (method.getJetMethod().returnTypeNullable()) {
            return TypeUtils.makeNullableAsSpecified(transformedType, true);
        }
        else if (method.getPsiMethod().getModifierList().findAnnotation(JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) != null) {
            return TypeUtils.makeNullableAsSpecified(transformedType, false);
        }
        else {
            return transformedType;
        }
    }

    private static Visibility resolveVisibilityFromPsiModifiers(PsiModifierListOwner modifierListOwner) {
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
            r.add(resolveClass(new FqName(innerPsiClass.getQualifiedName()), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN));
        }
        return r;
    }
}
