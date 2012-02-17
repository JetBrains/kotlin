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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import jet.typeinfo.TypeInfoVariance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.java.alt.AltClassFinder;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.rt.signature.JetSignatureAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureExceptionsAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureReader;
import org.jetbrains.jet.rt.signature.JetSignatureVisitor;

import java.util.*;

/**
 * @author abreslav
 */
public class JavaDescriptorResolver {
    
    public static String JAVA_ROOT = "<java_root>";

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


    private static abstract class ResolverScopeData {
        protected boolean kotlin;
        
        private Map<String, NamedMembers> namedMembersMap;
        
        @NotNull
        public abstract List<TypeParameterDescriptor> getTypeParameters();
    }

    static abstract class ResolverClassData extends ResolverScopeData {

        @NotNull
        public abstract ClassDescriptor getClassDescriptor();

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getTypeParameters() {
            return getClassDescriptor().getTypeConstructor().getParameters();
        }
    }

    /** Class with instance members */
    static class ResolverBinaryClassData extends ResolverClassData {

        ResolverBinaryClassData() {
        }

        private MutableClassDescriptorLite classDescriptor;

        List<TypeParameterDescriptorInitialization> typeParameters;

        @Override
        @NotNull
        public ClassDescriptor getClassDescriptor() {
            return classDescriptor;
        }
    }

    static class ResolverSrcClassData extends ResolverClassData {
        @NotNull
        private final ClassDescriptor classDescriptor;

        ResolverSrcClassData(@NotNull ClassDescriptor classDescriptor) {
            this.classDescriptor = classDescriptor;
        }

        @Override
        @NotNull
        public ClassDescriptor getClassDescriptor() {
            return classDescriptor;
        }
    }

    /** Either package or class with static members */
    private static class ResolverNamespaceData extends ResolverScopeData {
        private JavaNamespaceDescriptor namespaceDescriptor;

        @NotNull
        public NamespaceDescriptor getNamespaceDescriptor() {
            return namespaceDescriptor;
        }

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getTypeParameters() {
            return new ArrayList<TypeParameterDescriptor>(0);
        }
    }

    protected final Map<String, ResolverBinaryClassData> classDescriptorCache = Maps.newHashMap();
    protected final Map<String, ResolverNamespaceData> namespaceDescriptorCacheByFqn = Maps.newHashMap();
    protected final Map<PsiElement, ResolverNamespaceData> namespaceDescriptorCache = Maps.newHashMap();

    protected final Map<PsiMethod, FunctionDescriptorImpl> methodDescriptorCache = Maps.newHashMap();
    protected final JavaPsiFacade javaFacade;
    protected final GlobalSearchScope javaSearchScope;
    protected final JavaSemanticServices semanticServices;
    private final AltClassFinder altClassFinder;

    public JavaDescriptorResolver(Project project, JavaSemanticServices semanticServices) {
        this.javaFacade = JavaPsiFacade.getInstance(project);
        this.javaSearchScope = new DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
            @Override
            public boolean contains(VirtualFile file) {
                return myBaseScope.contains(file) && file.getFileType() != JetFileType.INSTANCE;
            }
        };
        this.semanticServices = semanticServices;
        altClassFinder = new AltClassFinder(project);
    }
    
    @Nullable
    ResolverClassData resolveClassData(@NotNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();

        if (qualifiedName.endsWith(JvmAbi.TRAIT_IMPL_SUFFIX)) {
            // TODO: only if -$$TImpl class is created by Kotlin
            return null;
        }

        // First, let's check that this is a real Java class, not a Java's view on a Kotlin class:
        ClassDescriptor kotlinClassDescriptor = semanticServices.getKotlinClassDescriptor(qualifiedName);
        if (kotlinClassDescriptor != null) {
            return new ResolverSrcClassData(kotlinClassDescriptor);
        }

        PsiClass containingClass = psiClass.getContainingClass();
        if (containingClass != null) {
            // must resolve containing class first, because inner class must have a reference to it
            resolveClass(containingClass);
        }

        // Not let's take a descriptor of a Java class
        ResolverBinaryClassData classData = classDescriptorCache.get(qualifiedName);
        if (classData == null) {
            classData = createJavaClassDescriptor(psiClass);
            classDescriptorCache.put(qualifiedName, classData);
        }
        return classData;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull PsiClass psiClass) {
        ResolverClassData classData = resolveClassData(psiClass);
        if (classData != null) {
            return classData.getClassDescriptor();
        } else {
            return null;
        }
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull String qualifiedName) {

        if (qualifiedName.endsWith(JvmAbi.TRAIT_IMPL_SUFFIX)) {
            // TODO: only if -$$TImpl class is created by Kotlin
            return null;
        }
        
        // First, let's check that this is a real Java class, not a Java's view on a Kotlin class:
        ClassDescriptor kotlinClassDescriptor = semanticServices.getKotlinClassDescriptor(qualifiedName);
        if (kotlinClassDescriptor != null) {
            return kotlinClassDescriptor;
        }

        // Not let's take a descriptor of a Java class
        ResolverBinaryClassData classData = classDescriptorCache.get(qualifiedName);
        if (classData == null) {
            PsiClass psiClass = findClass(qualifiedName);
            if (psiClass == null) {
                return null;
            }
            classData = createJavaClassDescriptor(psiClass);
        }
        return classData.getClassDescriptor();
    }

    private ResolverBinaryClassData createJavaClassDescriptor(@NotNull final PsiClass psiClass) {
        if (classDescriptorCache.containsKey(psiClass.getQualifiedName())) {
            throw new IllegalStateException(psiClass.getQualifiedName());
        }

        String name = psiClass.getName();
        ResolverBinaryClassData classData = new ResolverBinaryClassData();
        classDescriptorCache.put(psiClass.getQualifiedName(), classData);
        ClassKind kind = psiClass.isInterface() ? (psiClass.isAnnotationType() ? ClassKind.ANNOTATION_CLASS : ClassKind.TRAIT) : ClassKind.CLASS;
        DeclarationDescriptor containingDeclaration = resolveParentDescriptor(psiClass);
        classData.classDescriptor = new MutableClassDescriptorLite(containingDeclaration, kind);
        classData.classDescriptor.setName(name);
        
        List<JetType> supertypes = new ArrayList<JetType>();

        TypeVariableResolverFromOuters outerTypeVariableByNameResolver = new TypeVariableResolverFromOuters(containingDeclaration);

        classData.typeParameters = createUninitializedClassTypeParameters(psiClass, classData, outerTypeVariableByNameResolver);
        
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>();
        for (TypeParameterDescriptorInitialization typeParameter : classData.typeParameters) {
            typeParameters.add(typeParameter.descriptor);
        }
        
        classData.classDescriptor.setTypeParameterDescriptors(typeParameters);
        classData.classDescriptor.setSupertypes(supertypes);
        classData.classDescriptor.setVisibility(resolveVisibilityFromPsiModifiers(psiClass));
        classData.classDescriptor.setModality(Modality.convertFromFlags(
                psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface(),
                !psiClass.hasModifierProperty(PsiModifier.FINAL))
        );
        classData.classDescriptor.createTypeConstructor();
        classData.classDescriptor.setScopeForMemberLookup(new JavaClassMembersScope(classData.classDescriptor, psiClass, semanticServices, false));

        initializeTypeParameters(classData.typeParameters, new TypeVariableResolverFromTypeDescriptors(new ArrayList<TypeParameterDescriptor>(), outerTypeVariableByNameResolver));

        TypeVariableResolverFromTypeDescriptors resolverForTypeParameters = new TypeVariableResolverFromTypeDescriptors(classData.getTypeParameters(), null);

        // TODO: ugly hack: tests crash if initializeTypeParameters called with class containing proper supertypes
        supertypes.addAll(getSupertypes(new PsiClassWrapper(psiClass), classData.getTypeParameters()));

        if (psiClass.isInterface()) {
            //classData.classDescriptor.setSuperclassType(JetStandardClasses.getAnyType()); // TODO : Make it java.lang.Object
        }
        else {
            PsiClassType[] extendsListTypes = psiClass.getExtendsListTypes();
            assert extendsListTypes.length == 0 || extendsListTypes.length == 1;
            JetType superclassType = extendsListTypes.length == 0
                                            ? JetStandardClasses.getAnyType()
                                            : semanticServices.getTypeTransformer().transformToType(extendsListTypes[0], resolverForTypeParameters);
            //classData.classDescriptor.setSuperclassType(superclassType);
        }

        PsiMethod[] psiConstructors = psiClass.getConstructors();

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
                constructorDescriptor.initialize(typeParameters, Collections.<ValueParameterDescriptor>emptyList(), Modality.FINAL, classData.classDescriptor.getVisibility());
                constructorDescriptor.setReturnType(classData.classDescriptor.getDefaultType());
                classData.classDescriptor.addConstructor(constructorDescriptor, null);
                semanticServices.getTrace().record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
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

                constructorDescriptor.initialize(typeParameters, valueParameters, Modality.FINAL, classData.classDescriptor.getVisibility());
                constructorDescriptor.setReturnType(classData.classDescriptor.getDefaultType());
                classData.classDescriptor.addConstructor(constructorDescriptor, null);
                semanticServices.getTrace().record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
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
                ValueParameterDescriptors valueParameterDescriptors = resolveParameterDescriptors(constructorDescriptor,
                        constructor.getParameters(),
                        new TypeVariableResolverFromTypeDescriptors(classData.getTypeParameters(), null) // TODO: outer too
                    );
                if (valueParameterDescriptors.receiverType != null) {
                    throw new IllegalStateException();
                }
                constructorDescriptor.initialize(typeParameters, valueParameterDescriptors.descriptors, Modality.FINAL,
                                                 resolveVisibilityFromPsiModifiers(psiConstructor));
                constructorDescriptor.setReturnType(classData.classDescriptor.getDefaultType());
                classData.classDescriptor.addConstructor(constructorDescriptor, null);
                semanticServices.getTrace().record(BindingContext.CONSTRUCTOR, psiConstructor, constructorDescriptor);
            }
        }

        // TODO
        //classData.classDescriptor.setClassObjectDescriptor(createClassObjectDescriptor(classData.classDescriptor));

        semanticServices.getTrace().record(BindingContext.CLASS, psiClass, classData.classDescriptor);

        return classData;
    }

    /**
     * TODO
     * @see #createJavaNamespaceDescriptor(com.intellij.psi.PsiClass)
     */
    @Nullable
    private MutableClassDescriptorLite createClassObjectDescriptor(@NotNull ClassDescriptor containing, @NotNull PsiClass psiClass) {
        MutableClassDescriptorLite classObjectDescriptor = new MutableClassDescriptorLite(containing, ClassKind.OBJECT);
        classObjectDescriptor.setName("<TODO>"); // TODO
        classObjectDescriptor.setModality(Modality.FINAL);
        classObjectDescriptor.setTypeParameterDescriptors(new ArrayList<TypeParameterDescriptor>(0));
        classObjectDescriptor.createTypeConstructor();
        classObjectDescriptor.setScopeForMemberLookup(new JavaClassMembersScope(classObjectDescriptor, psiClass, semanticServices, true));
        return classObjectDescriptor;
    }

    private List<TypeParameterDescriptorInitialization> createUninitializedClassTypeParameters(PsiClass psiClass, ResolverBinaryClassData classData, TypeVariableResolver typeVariableResolver) {
        JetClassAnnotation jetClassAnnotation = JetClassAnnotation.get(psiClass);
        classData.kotlin = jetClassAnnotation.isDefined();
        
        if (jetClassAnnotation.signature().length() > 0) {
            return resolveClassTypeParametersFromJetSignature(
                    jetClassAnnotation.signature(), psiClass, classData.classDescriptor, typeVariableResolver);
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


    // cache
    protected ClassDescriptor javaLangObject;

    @NotNull
    private ClassDescriptor getJavaLangObject() {
        if (javaLangObject == null) {
            javaLangObject = resolveClass("java.lang.Object");
        }
        return javaLangObject;
    }

    private boolean isJavaLangObject(JetType type) {
        return type.getConstructor().getDeclarationDescriptor() == getJavaLangObject();
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
            return new JetTypeJetSignatureReader(semanticServices, semanticServices.getJetSemanticServices().getStandardLibrary(), typeVariableResolver) {
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
            return new JetTypeJetSignatureReader(semanticServices, semanticServices.getJetSemanticServices().getStandardLibrary(), typeVariableResolver) {
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


        private JetSignatureTypeParametersVisitor(@NotNull TypeVariableResolver containerTypeVariableResolver, @NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameterListOwner psiOwner) {
            this.containingDeclaration = containingDeclaration;
            this.psiOwner = psiOwner;

            this.typeVariableResolver = new TypeVariableResolverFromTypeDescriptors(previousTypeParameters, containerTypeVariableResolver);
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
     * @see #resolveMethodTypeParametersFromJetSignature(String, com.intellij.psi.PsiMethod, org.jetbrains.jet.lang.descriptors.DeclarationDescriptor, TypeVariableResolver) 
     */
    private List<TypeParameterDescriptorInitialization> resolveClassTypeParametersFromJetSignature(String jetSignature, final PsiClass clazz,
            final ClassDescriptor classDescriptor, final TypeVariableResolver outerClassTypeVariableResolver) {
        JetSignatureTypeParametersVisitor jetSignatureTypeParametersVisitor = new JetSignatureTypeParametersVisitor(outerClassTypeVariableResolver, classDescriptor, clazz) {
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

    private DeclarationDescriptor resolveParentDescriptor(PsiClass psiClass) {
        PsiClass containingClass = psiClass.getContainingClass();
        if (containingClass != null) {
            return resolveClass(containingClass);
        }
        
        PsiJavaFile containingFile = (PsiJavaFile) psiClass.getContainingFile();
        String packageName = containingFile.getPackageName();
        return resolveNamespace(packageName);
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
            } else {
                for (JetType upperBound : typeParameter.upperBoundsForKotlin) {
                    typeParameterDescriptor.addUpperBound(upperBound);
                }
            }

            // TODO: lower bounds
        } else {
            PsiClassType[] referencedTypes = typeParameter.psiTypeParameter.getExtendsList().getReferencedTypes();
            if (referencedTypes.length == 0){
                typeParameterDescriptor.addUpperBound(JetStandardClasses.getNullableAnyType());
            }
            else if (referencedTypes.length == 1) {
                typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer().transformToType(referencedTypes[0], typeVariableByPsiResolver));
            }
            else {
                for (PsiClassType referencedType : referencedTypes) {
                    typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer().transformToType(referencedType, typeVariableByPsiResolver));
                }
            }
        }
        typeParameterDescriptor.setInitialized();
    }

    private void initializeTypeParameters(List<TypeParameterDescriptorInitialization> typeParametersInitialization, TypeVariableResolver typeVariableByPsiResolver) {
        List<TypeParameterDescriptor> prevTypeParameters = new ArrayList<TypeParameterDescriptor>();
        for (TypeParameterDescriptorInitialization psiTypeParameter : typeParametersInitialization) {
            prevTypeParameters.add(psiTypeParameter.descriptor);
            initializeTypeParameter(psiTypeParameter, new TypeVariableResolverFromTypeDescriptors(prevTypeParameters, typeVariableByPsiResolver));
        }
    }

    private Collection<? extends JetType> getSupertypes(PsiClassWrapper psiClass, List<TypeParameterDescriptor> typeParameters) {
        final List<JetType> result = new ArrayList<JetType>();

        if (psiClass.getJetClass().signature().length() > 0) {
            final TypeVariableResolver typeVariableResolver = new TypeVariableResolverFromTypeDescriptors(typeParameters, null);
            
            new JetSignatureReader(psiClass.getJetClass().signature()).accept(new JetSignatureExceptionsAdapter() {
                @Override
                public JetSignatureVisitor visitFormalTypeParameter(String name, TypeInfoVariance variance, boolean reified) {
                    // TODO: collect
                    return new JetSignatureAdapter();
                }

                @Override
                public JetSignatureVisitor visitSuperclass() {
                    return new JetTypeJetSignatureReader(semanticServices, semanticServices.getJetSemanticServices().getStandardLibrary(), typeVariableResolver) {
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
                    return new JetTypeJetSignatureReader(semanticServices, semanticServices.getJetSemanticServices().getStandardLibrary(), typeVariableResolver) {
                        @Override
                        protected void done(@NotNull JetType jetType) {
                            if (!jetType.equals(JetStandardClasses.getAnyType())) {
                                result.add(jetType);
                            }
                        }
                    };
                }
            });
        } else {
            transformSupertypeList(result, psiClass.getPsiClass().getExtendsListTypes(), new TypeVariableResolverFromTypeDescriptors(typeParameters, null));
            transformSupertypeList(result, psiClass.getPsiClass().getImplementsListTypes(), new TypeVariableResolverFromTypeDescriptors(typeParameters, null));
        }
        if (result.isEmpty()) {
            result.add(JetStandardClasses.getAnyType());
        }
        return result;
    }

    private void transformSupertypeList(List<JetType> result, PsiClassType[] extendsListTypes, TypeVariableResolver typeVariableResolver) {
        for (PsiClassType type : extendsListTypes) {
            PsiClass resolved = type.resolve();
            if (resolved != null && resolved.getQualifiedName().equals(JvmStdlibNames.JET_OBJECT.getFqName())) {
                continue;
            }
            
            JetType transform = semanticServices.getTypeTransformer().transformToType(type, typeVariableResolver);

            result.add(TypeUtils.makeNotNullable(transform));
        }
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull String qualifiedName) {
        // First, let's check that there is no Kotlin package:
        NamespaceDescriptor kotlinNamespaceDescriptor = semanticServices.getKotlinNamespaceDescriptor(qualifiedName);
        if (kotlinNamespaceDescriptor != null) {
            return kotlinNamespaceDescriptor;
        }

        PsiPackage psiPackage = findPackage(qualifiedName);
        if (psiPackage == null) {
            PsiClass psiClass = findClass(qualifiedName);
            if (psiClass == null) return null;
            return resolveNamespace(psiClass);
        }
        return resolveNamespace(psiPackage);
    }

    public PsiClass findClass(String qualifiedName) {
        PsiClass original = javaFacade.findClass(qualifiedName, javaSearchScope);
        PsiClass altClass = altClassFinder.findClass(qualifiedName);
        if (altClass != null) {
            if (altClass instanceof ClsClassImpl) {
                altClass.putUserData(ClsClassImpl.DELEGATE_KEY, original);
            }

            return altClass;
        }
        return original;
    }

    /*package*/ PsiPackage findPackage(String qualifiedName) {
        return javaFacade.findPackage(qualifiedName);
    }

    private NamespaceDescriptor resolveNamespace(@NotNull PsiPackage psiPackage) {
        ResolverNamespaceData namespaceData = namespaceDescriptorCache.get(psiPackage);
        if (namespaceData == null) {
            namespaceData = createJavaNamespaceDescriptor(psiPackage);
            namespaceDescriptorCache.put(psiPackage, namespaceData);
            namespaceDescriptorCacheByFqn.put(psiPackage.getQualifiedName(), namespaceData);
        }
        return namespaceData.namespaceDescriptor;
    }

    private NamespaceDescriptor resolveNamespace(@NotNull PsiClass psiClass) {
        ResolverNamespaceData namespaceData = namespaceDescriptorCache.get(psiClass);
        if (namespaceData == null) {
            namespaceData = createJavaNamespaceDescriptor(psiClass);
            namespaceDescriptorCache.put(psiClass, namespaceData);
            namespaceDescriptorCacheByFqn.put(psiClass.getQualifiedName(), namespaceData);
        }
        return namespaceData.namespaceDescriptor;
    }

    private ResolverNamespaceData createJavaNamespaceDescriptor(@NotNull PsiPackage psiPackage) {
        ResolverNamespaceData namespaceData = new ResolverNamespaceData();
        String name = psiPackage.getName();
        namespaceData.namespaceDescriptor = new JavaNamespaceDescriptor(
                resolveParentDescriptor(psiPackage),
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                name == null ? JAVA_ROOT : name,
                name == null ? JAVA_ROOT : psiPackage.getQualifiedName(),
                true
        );

        namespaceData.namespaceDescriptor.setMemberScope(new JavaPackageScope(psiPackage.getQualifiedName(), namespaceData.namespaceDescriptor, semanticServices));
        semanticServices.getTrace().record(BindingContext.NAMESPACE, psiPackage, namespaceData.namespaceDescriptor);
        // TODO: hack
        namespaceData.kotlin = true;
        return namespaceData;
    }

    private DeclarationDescriptor resolveParentDescriptor(@NotNull PsiPackage psiPackage) {
        PsiPackage parentPackage = psiPackage.getParentPackage();
        if (parentPackage == null) {
            return null;
        }
        return resolveNamespace(parentPackage);
    }

    /**
     * TODO
     * @see #createClassObjectDescriptor(org.jetbrains.jet.lang.descriptors.ClassDescriptor, com.intellij.psi.PsiClass)
     */
    private ResolverNamespaceData createJavaNamespaceDescriptor(@NotNull final PsiClass psiClass) {
        ResolverNamespaceData namespaceData = new ResolverNamespaceData();
        namespaceData.namespaceDescriptor = new JavaNamespaceDescriptor(
                resolveParentDescriptor(psiClass),
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                psiClass.getName(),
                psiClass.getQualifiedName(),
                false
        );
        namespaceData.namespaceDescriptor.setMemberScope(new JavaClassMembersScope(namespaceData.namespaceDescriptor, psiClass, semanticServices, true));
        semanticServices.getTrace().record(BindingContext.NAMESPACE, psiClass, namespaceData.namespaceDescriptor);
        return namespaceData;
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

        boolean nullable = parameter.getJetValueParameter().nullable();

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
        } else {
            outType = semanticServices.getTypeTransformer().transformToType(psiType, typeVariableResolver);
        }

        JetType varargElementType;
        if (psiType instanceof PsiEllipsisType) {
            varargElementType = semanticServices.getJetSemanticServices().getStandardLibrary().getArrayElementType(outType);
        } else {
            varargElementType = null;
        }

        if (receiver) {
            return JvmMethodParameterMeaning.receiver(outType);
        } else {
            return JvmMethodParameterMeaning.regular(new ValueParameterDescriptorImpl(
                    containingDeclaration,
                    i,
                    Collections.<AnnotationDescriptor>emptyList(), // TODO
                    name,
                    false,
                    nullable ? TypeUtils.makeNullableAsSpecified(outType, nullable) : outType,
                    hasDefaultValue,
                    varargElementType
            ));
        }
    }

    public Set<VariableDescriptor> resolveFieldGroupByName(@NotNull ClassOrNamespaceDescriptor owner, PsiClass psiClass, String fieldName, boolean staticMembers) {
        ResolverScopeData scopeData = getResolverScopeData(owner, new PsiClassWrapper(psiClass));

        NamedMembers namedMembers = scopeData.namedMembersMap.get(fieldName);
        if (namedMembers == null) {
            return Collections.emptySet();
        }
        Set<VariableDescriptor> r = new HashSet<VariableDescriptor>();

        resolveNamedGroupProperties(owner, scopeData, staticMembers, namedMembers, fieldName);

        r.addAll(namedMembers.propertyDescriptors);

        for (JetType supertype : getSupertypes(scopeData)) {
            r.addAll(supertype.getMemberScope().getProperties(fieldName));
        }

        return r;
    }
    
    @NotNull
    public Set<VariableDescriptor> resolveFieldGroup(@NotNull ClassOrNamespaceDescriptor owner, PsiClass psiClass, boolean staticMembers) {

        ResolverScopeData scopeData = getResolverScopeData(owner, new PsiClassWrapper(psiClass));
        
        Set<VariableDescriptor> descriptors = Sets.newHashSet();
        Map<String, NamedMembers> membersForProperties = scopeData.namedMembersMap;
        for (Map.Entry<String, NamedMembers> entry : membersForProperties.entrySet()) {
            NamedMembers namedMembers = entry.getValue();
            if (namedMembers.propertyAccessors == null) {
                continue;
            }
            
            String propertyName = entry.getKey();

            resolveNamedGroupProperties(owner, scopeData, staticMembers, namedMembers, propertyName);
            descriptors.addAll(namedMembers.propertyDescriptors);
        }

        for (JetType supertype : getSupertypes(scopeData)) {
            for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
                // TODO: ugly
                if (descriptor instanceof VariableDescriptor) {
                    descriptors.add((VariableDescriptor) descriptor);
                }
            }
        }
        
        return descriptors;
    }
    
    private Object key(TypeSource typeSource) {
        if (typeSource == null) {
            return "";
        } else if (typeSource.getTypeString().length() > 0) {
            return typeSource.getTypeString();
        } else {
            return psiTypeToKey(typeSource.getPsiType());
        }
    }

    private Object psiTypeToKey(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            return ((PsiClassType) psiType).getClassName();
        } else if (psiType instanceof PsiPrimitiveType) {
            return psiType.getPresentableText();
        } else if (psiType instanceof PsiArrayType) {
            return Pair.create("[", psiTypeToKey(((PsiArrayType) psiType).getComponentType()));
        } else {
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
            boolean staticMembers, NamedMembers namedMembers, String propertyName) {
        if (namedMembers.propertyDescriptors != null) {
            return;
        }
        
        if (namedMembers.propertyAccessors == null) {
            namedMembers.propertyDescriptors = Collections.emptySet();
            return;
        }

        final List<TypeParameterDescriptor> classTypeParameterDescriptorInitialization = scopeData.getTypeParameters();

        TypeVariableResolver typeVariableResolver = new TypeVariableResolverFromTypeDescriptors(scopeData.getTypeParameters(), null);

        class GroupingValue {
            PropertyAccessorData getter;
            PropertyAccessorData setter;
            PropertyAccessorData field;
        }
        
        Map<Object, GroupingValue> map = new HashMap<Object, GroupingValue>();

        for (PropertyAccessorData propertyAccessor : namedMembers.propertyAccessors) {

            Object key = propertyKeyForGrouping(propertyAccessor);
            
            GroupingValue value = map.get(key);
            if (value == null) {
                value = new GroupingValue();
                map.put(key, value);
            }

            if (propertyAccessor.isGetter()) {
                if (value.getter != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.getter = propertyAccessor;
            } else if (propertyAccessor.isSetter()) {
                if (value.setter != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.setter = propertyAccessor;
            } else if (propertyAccessor.isField()) {
                if (value.field != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.field = propertyAccessor;
            } else {
                throw new IllegalStateException();
            }
        }

        
        Set<VariableDescriptor> r = new HashSet<VariableDescriptor>();
        
        for (GroupingValue members : map.values()) {
            boolean isFinal;
            if (members.setter == null && members.getter == null) {
                isFinal = false;
            } else if (members.getter != null) {
                isFinal = members.getter.getMember().isFinal();
            } else if (members.setter != null) {
                isFinal = members.setter.getMember().isFinal();
            } else {
                isFinal = false;
            }

            PropertyAccessorData anyMember;
            if (members.getter != null) {
                anyMember = members.getter;
            } else if (members.field != null) {
                anyMember = members.field;
            } else if (members.setter != null) {
                anyMember = members.setter;
            } else {
                throw new IllegalStateException();
            }

            boolean isVar;
            if (members.getter == null && members.setter == null) {
                isVar = !members.field.getMember().isFinal();
            } else {
                isVar = members.setter != null;
            }
            
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                    owner,
                    Collections.<AnnotationDescriptor>emptyList(),
                    isFinal && !staticMembers ? Modality.FINAL : Modality.OPEN, // TODO: abstract
                    resolveVisibilityFromPsiModifiers(anyMember.getMember().psiMember),
                    isVar,
                    false,
                    propertyName,
                    CallableMemberDescriptor.Kind.DECLARATION);

            PropertyGetterDescriptor getterDescriptor = null;
            PropertySetterDescriptor setterDescriptor = null;
            if (members.getter != null) {
                getterDescriptor = new PropertyGetterDescriptor(propertyDescriptor, Collections.<AnnotationDescriptor>emptyList(), Modality.OPEN, Visibility.PUBLIC, true, false, CallableMemberDescriptor.Kind.DECLARATION);
            }
            if (members.setter != null) {
                setterDescriptor = new PropertySetterDescriptor(propertyDescriptor, Collections.<AnnotationDescriptor>emptyList(), Modality.OPEN, Visibility.PUBLIC, true, false, CallableMemberDescriptor.Kind.DECLARATION);
            }

            propertyDescriptor.initialize(getterDescriptor, setterDescriptor);

            List<TypeParameterDescriptor> typeParametersInitialization = new ArrayList<TypeParameterDescriptor>(0);

            if (members.setter != null) {
                PsiMethodWrapper method = (PsiMethodWrapper) members.setter.getMember();

                if (anyMember == members.setter) {
                    typeParametersInitialization = resolveMethodTypeParameters(method, setterDescriptor, typeVariableResolver);
                }
            }
            if (members.getter != null) {
                PsiMethodWrapper method = (PsiMethodWrapper) members.getter.getMember();

                if (anyMember == members.getter) {
                    typeParametersInitialization = resolveMethodTypeParameters(method, getterDescriptor, typeVariableResolver);
                }
            }

            List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>();
            for (TypeParameterDescriptor typeParameter : typeParametersInitialization) {
                typeParameters.add(typeParameter);
            }

            List<TypeParameterDescriptor> typeParametersForReceiver = new ArrayList<TypeParameterDescriptor>();
            typeParametersForReceiver.addAll(classTypeParameterDescriptorInitialization);
            typeParametersForReceiver.addAll(typeParametersInitialization);
            TypeVariableResolver typeVariableResolverForPropertyInternals = new TypeVariableResolverFromTypeDescriptors(typeParametersForReceiver, null);

            JetType propertyType;
            if (anyMember.getType().getTypeString().length() > 0) {
                propertyType = semanticServices.getTypeTransformer().transformToType(anyMember.getType().getTypeString(), typeVariableResolverForPropertyInternals);
            } else {
                propertyType = semanticServices.getTypeTransformer().transformToType(anyMember.getType().getPsiType(), typeVariableResolverForPropertyInternals);
            }
            
            JetType receiverType;
            if (anyMember.getReceiverType() == null) {
                receiverType = null;
            } else if (anyMember.getReceiverType().getTypeString().length() > 0) {
                receiverType = semanticServices.getTypeTransformer().transformToType(anyMember.getReceiverType().getTypeString(), typeVariableResolverForPropertyInternals);
            } else {
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
                setterDescriptor.initialize(new ValueParameterDescriptorImpl(setterDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), "p0"/*TODO*/, false, propertyDescriptor.getOutType(), false, null));
            }

            semanticServices.getTrace().record(BindingContext.VARIABLE, anyMember.getMember().psiMember, propertyDescriptor);
            
            r.add(propertyDescriptor);
        }

        namedMembers.propertyDescriptors = r;
    }

    private void resolveNamedGroupFunctions(ClassOrNamespaceDescriptor owner, PsiClass psiClass, TypeSubstitutor typeSubstitutorForGenericSuperclasses, NamedMembers namedMembers, String methodName, ResolverScopeData scopeData) {
        if (namedMembers.functionDescriptors != null) {
            return;
        }
        
        final Set<FunctionDescriptor> functions = new HashSet<FunctionDescriptor>();

        Set<NamedFunctionDescriptor> functionsFromCurrent = Sets.newHashSet();
        for (PsiMethodWrapper method : namedMembers.methods) {
            FunctionDescriptorImpl function = resolveMethodToFunctionDescriptor(owner, psiClass,
                    typeSubstitutorForGenericSuperclasses,
                    method);
            if (function != null) {
                functionsFromCurrent.add((NamedFunctionDescriptor) function);
            }
        }

        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            Set<NamedFunctionDescriptor> functionsFromSupertypes = getFunctionsFromSupertypes(scopeData, methodName);

            OverrideResolver.generateOverridesInFunctionGroup(methodName, functionsFromSupertypes, functionsFromCurrent, classDescriptor, new OverrideResolver.DescriptorSink<NamedFunctionDescriptor>() {
                @Override
                public void addToScope(NamedFunctionDescriptor fakeOverride) {
                    functions.add(fakeOverride);
                }
            });

        }

        functions.addAll(functionsFromCurrent);

        namedMembers.functionDescriptors = functions;
    }
    
    private Set<NamedFunctionDescriptor> getFunctionsFromSupertypes(ResolverScopeData scopeData, String methodName) {
        Set<NamedFunctionDescriptor> r = new HashSet<NamedFunctionDescriptor>();
        for (JetType supertype : getSupertypes(scopeData)) {
            for (FunctionDescriptor function : supertype.getMemberScope().getFunctions(methodName)) {
                r.add((NamedFunctionDescriptor) function);
            }
        }
        return r;
    }

    private ResolverScopeData getResolverScopeData(@NotNull ClassOrNamespaceDescriptor owner, PsiClassWrapper psiClass) {
        ResolverScopeData scopeData;
        boolean staticMembers;
        if (owner instanceof JavaNamespaceDescriptor) {
            scopeData = namespaceDescriptorCacheByFqn.get(((JavaNamespaceDescriptor) owner).getQualifiedName());
            staticMembers = true;
        } else if (owner instanceof ClassDescriptor) {
            scopeData = classDescriptorCache.get(psiClass.getQualifiedName());
            staticMembers = false;
        } else {
            throw new IllegalStateException("unknown owner: " + owner.getClass().getName());
        }
        if (scopeData == null) {
            throw new IllegalStateException();
        }
        
        if (scopeData.namedMembersMap == null) {
            scopeData.namedMembersMap = JavaDescriptorResolverHelper.getNamedMembers(psiClass, staticMembers, scopeData.kotlin);
        }
        
        return scopeData;
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroup(@NotNull ClassOrNamespaceDescriptor descriptor, @NotNull PsiClass psiClass, @NotNull String methodName, boolean staticMembers) {

        ResolverScopeData resolverScopeData = getResolverScopeData(descriptor, new PsiClassWrapper(psiClass));

        Map<String, NamedMembers> namedMembersMap = resolverScopeData.namedMembersMap;

        NamedMembers namedMembers = namedMembersMap.get(methodName);
        if (namedMembers != null && namedMembers.methods != null) {
            TypeSubstitutor typeSubstitutor = typeSubstitutorForGenericSupertypes(resolverScopeData);

            resolveNamedGroupFunctions(descriptor, psiClass, typeSubstitutor, namedMembers, methodName, resolverScopeData);

            return namedMembers.functionDescriptors;
        } else {
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
    
    private static boolean equal(@NotNull PsiClass c1, @NotNull PsiClass c2) {
        return c1.getQualifiedName().equals(c2.getQualifiedName());
    }
    
    private static boolean equal(@NotNull PsiMethod m1, @NotNull PsiMethod m2) {
        // TODO dummy
        return m1.getName().equals(m2.getName()) && equal(m1.getContainingClass(), m2.getContainingClass());
    }

    private static boolean equal(@NotNull PsiTypeParameterListOwner o1, @NotNull PsiTypeParameterListOwner o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 instanceof PsiClass && o2 instanceof PsiClass) {
            return equal((PsiClass) o1, (PsiClass) o2);
        }
        if (o1 instanceof PsiMethod && o2 instanceof PsiMethod) {
            return equal((PsiMethod) o1, (PsiMethod) o2);
        }
        if ((o1 instanceof PsiClass || o1 instanceof PsiMethod) && (o2 instanceof PsiClass || o2 instanceof PsiMethod)) {
            return false;
        }
        throw new IllegalStateException(o1.getClass() + ", " + o2.getClass());
    }
    
    public static boolean equal(@NotNull PsiTypeParameter p1, @NotNull PsiTypeParameter p2) {
        if (p1 == p2) {
            return true;
        }
        if (p1.getIndex() != p2.getIndex()) {
            return false;
        }
        return equal(p1.getOwner(), p2.getOwner());
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
            } else if (meaning.kind == JvmMethodParameterKind.REGULAR) {
                result.add(meaning.valueParameterDescriptor);
            } else if (meaning.kind == JvmMethodParameterKind.RECEIVER) {
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
    private FunctionDescriptorImpl resolveMethodToFunctionDescriptor(ClassOrNamespaceDescriptor owner, final PsiClass psiClass, TypeSubstitutor typeSubstitutorForGenericSuperclasses, final PsiMethodWrapper method) {

        PsiType returnType = method.getReturnType();
        if (returnType == null) {
            return null;
        }
        FunctionDescriptorImpl functionDescriptor = methodDescriptorCache.get(method.getPsiMethod());
        if (functionDescriptor != null) {
            if (method.getPsiMethod().getContainingClass() != psiClass) {
                //functionDescriptor = functionDescriptor.substitute(typeSubstitutorForGenericSuperclasses);
                throw new IllegalStateException();
            }
            return functionDescriptor;
        }

        ResolverScopeData scopeData = getResolverScopeData(owner, new PsiClassWrapper(psiClass));

        boolean kotlin;
        if (owner instanceof JavaNamespaceDescriptor) {
            JavaNamespaceDescriptor javaNamespaceDescriptor = (JavaNamespaceDescriptor) owner;
            ResolverNamespaceData namespaceData = namespaceDescriptorCacheByFqn.get(javaNamespaceDescriptor.getQualifiedName());
            if (namespaceData == null) {
                throw new IllegalStateException("namespaceData not found by name " + javaNamespaceDescriptor.getQualifiedName());
            }
            kotlin = namespaceData.kotlin;
        } else {
            ResolverBinaryClassData classData = classDescriptorCache.get(psiClass.getQualifiedName());
            if (classData == null) {
                throw new IllegalStateException("classData not found by name " + psiClass.getQualifiedName());
            }
            kotlin = classData.kotlin;
        }

        // TODO: ugly
        if (method.getJetMethod().kind() == JvmStdlibNames.JET_METHOD_KIND_PROPERTY) {
            return null;
        }

        if (kotlin) {
            // TODO: unless maybe class explicitly extends Object
            String ownerClassName = method.getPsiMethod().getContainingClass().getQualifiedName();
            if (ownerClassName.equals("java.lang.Object")) {
                return null;
            }
            
            if (method.getName().equals(JvmStdlibNames.JET_OBJECT_GET_TYPEINFO_METHOD) && method.getParameters().size() == 0) {
                return null;
            }
            
            if (method.getName().equals(JvmStdlibNames.JET_OBJECT_GET_OUTER_OBJECT_METHOD) && method.getParameters().size() == 0) {
                return null;
            }

            // TODO: check signature
            if (method.getName().equals(JvmAbi.SET_TYPE_INFO_METHOD)) {
                return null;
            }
        }

        ClassOrNamespaceDescriptor classDescriptor;
        if (scopeData instanceof ResolverBinaryClassData) {
            ClassDescriptor classClassDescriptor = resolveClass(method.getPsiMethod().getContainingClass());
            classDescriptor = classClassDescriptor;
        }
        else {
            classDescriptor = resolveNamespace(method.getPsiMethod().getContainingClass());
        }
        if (classDescriptor == null) {
            return null;
        }

        NamedFunctionDescriptorImpl functionDescriptorImpl = new NamedFunctionDescriptorImpl(
                owner,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                method.getName(),
                CallableMemberDescriptor.Kind.DECLARATION
        );
        methodDescriptorCache.put(method.getPsiMethod(), functionDescriptorImpl);

        final TypeVariableResolver typeVariableResolverForParameters = TypeVariableResolvers.classTypeVariableResolver(classDescriptor);

        final List<TypeParameterDescriptor> methodTypeParameters = resolveMethodTypeParameters(method, functionDescriptorImpl, typeVariableResolverForParameters);

        TypeVariableResolver methodTypeVariableResolver = new TypeVariableResolverFromTypeDescriptors(methodTypeParameters, typeVariableResolverForParameters);


        ValueParameterDescriptors valueParameterDescriptors = resolveParameterDescriptors(functionDescriptorImpl, method.getParameters(), methodTypeVariableResolver);
        functionDescriptorImpl.initialize(
                valueParameterDescriptors.receiverType,
                DescriptorUtils.getExpectedThisObjectIfNeeded(classDescriptor),
                methodTypeParameters,
                valueParameterDescriptors.descriptors,
                makeReturnType(returnType, method, methodTypeVariableResolver),
                Modality.convertFromFlags(method.getPsiMethod().hasModifierProperty(PsiModifier.ABSTRACT), !method.isFinal()),
                resolveVisibilityFromPsiModifiers(method.getPsiMethod())
        );
        semanticServices.getTrace().record(BindingContext.FUNCTION, method.getPsiMethod(), functionDescriptorImpl);
        FunctionDescriptor substitutedFunctionDescriptor = functionDescriptorImpl;
        if (method.getPsiMethod().getContainingClass() != psiClass) {
            //substitutedFunctionDescriptor = functionDescriptorImpl.substitute(typeSubstitutorForGenericSuperclasses);
            throw new IllegalStateException();
        }
        return (FunctionDescriptorImpl) substitutedFunctionDescriptor;
    }

    public List<FunctionDescriptor> resolveMethods(PsiClass psiClass, ClassOrNamespaceDescriptor containingDeclaration) {
        ResolverScopeData scopeData = getResolverScopeData(containingDeclaration, new PsiClassWrapper(psiClass));

        TypeSubstitutor substitutorForGenericSupertypes = typeSubstitutorForGenericSupertypes(scopeData);

        List<FunctionDescriptor> functions = new ArrayList<FunctionDescriptor>();

        for (Map.Entry<String, NamedMembers> entry : scopeData.namedMembersMap.entrySet()) {
            String methodName = entry.getKey();
            NamedMembers namedMembers = entry.getValue();
            resolveNamedGroupFunctions(containingDeclaration, psiClass, substitutorForGenericSupertypes, namedMembers, methodName, scopeData);
            functions.addAll(namedMembers.functionDescriptors);
        }

        return functions;
    }

    private Collection<JetType> getSupertypes(ResolverScopeData scope) {
        if (scope instanceof ResolverBinaryClassData) {
            return ((ResolverBinaryClassData) scope).classDescriptor.getSupertypes();
        } else if (scope instanceof ResolverNamespaceData) {
            return Collections.emptyList();
        } else {
            throw new IllegalStateException();
        }
    }

    private TypeSubstitutor typeSubstitutorForGenericSupertypes(ResolverScopeData scopeData) {
        if (scopeData instanceof ResolverClassData) {
            return createSubstitutorForGenericSupertypes(((ResolverClassData) scopeData).getClassDescriptor());
        } else {
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
                    method.getJetMethod().typeParameters(), method.getPsiMethod(), functionDescriptor, classTypeVariableResolver);
        } else {
            typeParametersIntialization = makeUninitializedTypeParameters(functionDescriptor, method.getPsiMethod().getTypeParameters());
        }

        initializeTypeParameters(typeParametersIntialization, classTypeVariableResolver);
        
        List<TypeParameterDescriptor> typeParameters = Lists.newArrayListWithCapacity(typeParametersIntialization.size());
        
        for (TypeParameterDescriptorInitialization tpdi : typeParametersIntialization) {
            typeParameters.add(tpdi.descriptor);
        }
        
        return typeParameters;
    }

    /**
     * @see #resolveClassTypeParametersFromJetSignature(String, com.intellij.psi.PsiClass, org.jetbrains.jet.lang.descriptors.ClassDescriptor, TypeVariableResolver)
     */
    private List<TypeParameterDescriptorInitialization> resolveMethodTypeParametersFromJetSignature(String jetSignature, final PsiMethod method,
            final DeclarationDescriptor functionDescriptor, final TypeVariableResolver classTypeVariableResolver)
    {
        JetSignatureTypeParametersVisitor jetSignatureTypeParametersVisitor = new JetSignatureTypeParametersVisitor(classTypeVariableResolver, functionDescriptor, method);
        new JetSignatureReader(jetSignature).acceptFormalTypeParametersOnly(jetSignatureTypeParametersVisitor);
        return jetSignatureTypeParametersVisitor.r;
    }

    private JetType makeReturnType(PsiType returnType, PsiMethodWrapper method,
            @NotNull TypeVariableResolver typeVariableResolver) {

        String returnTypeFromAnnotation = method.getJetMethod().returnType();

        JetType transformedType;
        if (returnTypeFromAnnotation.length() > 0) {
            transformedType = semanticServices.getTypeTransformer().transformToType(returnTypeFromAnnotation, typeVariableResolver);
        } else {
            transformedType = semanticServices.getTypeTransformer().transformToType(returnType, typeVariableResolver);
        }
        if (method.getJetMethod().returnTypeNullable()) {
            return TypeUtils.makeNullableAsSpecified(transformedType, true);
        } else {
            return transformedType;
        }
    }

    private static Visibility resolveVisibilityFromPsiModifiers(PsiModifierListOwner modifierListOwner) {
        //TODO report error
        return modifierListOwner.hasModifierProperty(PsiModifier.PUBLIC) ? Visibility.PUBLIC :
                                        (modifierListOwner.hasModifierProperty(PsiModifier.PRIVATE) ? Visibility.PRIVATE :
                                        (modifierListOwner.hasModifierProperty(PsiModifier.PROTECTED) ? Visibility.PROTECTED : Visibility.INTERNAL));
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
            r.add(resolveClass(innerPsiClass));
        }
        return r;
    }
}
