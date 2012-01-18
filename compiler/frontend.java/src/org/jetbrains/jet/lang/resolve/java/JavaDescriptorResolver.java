package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterListOwner;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import jet.typeinfo.TypeInfoVariance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.MutableClassDescriptorLite;
import org.jetbrains.jet.lang.descriptors.NamedFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.rt.signature.JetSignatureAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureExceptionsAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureReader;
import org.jetbrains.jet.rt.signature.JetSignatureVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    private static class TypeParameterDescriptorInitialization {
        private final TypeParameterDescriptorOrigin origin;
        private final TypeParameterDescriptor descriptor;
        @Nullable
        private final List<JetType> upperBoundsForKotlin;
        @Nullable
        private final List<JetType> lowerBoundsForKotlin;

        private TypeParameterDescriptorInitialization(TypeParameterDescriptor descriptor) {
            this.origin = TypeParameterDescriptorOrigin.JAVA;
            this.descriptor = descriptor;
            this.upperBoundsForKotlin = null;
            this.lowerBoundsForKotlin = null;
        }

        private TypeParameterDescriptorInitialization(TypeParameterDescriptor descriptor,
                List<JetType> upperBoundsForKotlin, List<JetType> lowerBoundsForKotlin) {
            this.origin = TypeParameterDescriptorOrigin.KOTLIN;
            this.descriptor = descriptor;
            this.upperBoundsForKotlin = upperBoundsForKotlin;
            this.lowerBoundsForKotlin = lowerBoundsForKotlin;
        }
    }


    private static abstract class ResolverScopeData {
        protected boolean kotlin;
        
        private Map<String, NamedMembers> namedMembersMap;
    }

    private static class ResolverClassData extends ResolverScopeData {
        private MutableClassDescriptorLite classDescriptor;

        @NotNull
        public ClassDescriptor getClassDescriptor() {
            return classDescriptor;
        }
    }

    private static class ResolverNamespaceData extends ResolverScopeData {
        private JavaNamespaceDescriptor namespaceDescriptor;

        @NotNull
        public NamespaceDescriptor getNamespaceDescriptor() {
            return namespaceDescriptor;
        }
    }

    protected final Map<String, ResolverClassData> classDescriptorCache = Maps.newHashMap();
    protected final Map<String, ResolverNamespaceData> namespaceDescriptorCacheByFqn = Maps.newHashMap();
    protected final Map<PsiElement, ResolverNamespaceData> namespaceDescriptorCache = Maps.newHashMap();

    private final Map<PsiTypeParameter, TypeParameterDescriptorInitialization> typeParameterDescriptorCache = Maps.newHashMap();
    protected final Map<PsiMethod, FunctionDescriptor> methodDescriptorCache = Maps.newHashMap();
    protected final JavaPsiFacade javaFacade;
    protected final GlobalSearchScope javaSearchScope;
    protected final JavaSemanticServices semanticServices;

    public JavaDescriptorResolver(Project project, JavaSemanticServices semanticServices) {
        this.javaFacade = JavaPsiFacade.getInstance(project);
        this.javaSearchScope = new DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
            @Override
            public boolean contains(VirtualFile file) {
                return myBaseScope.contains(file) && file.getFileType() != JetFileType.INSTANCE;
            }
        };
        this.semanticServices = semanticServices;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();

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
        ResolverClassData classData = classDescriptorCache.get(qualifiedName);
        if (classData == null) {
            classData = createJavaClassDescriptor(psiClass);
            classDescriptorCache.put(qualifiedName, classData);
        }
        return classData.getClassDescriptor();
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
        ResolverClassData classData = classDescriptorCache.get(qualifiedName);
        if (classData == null) {
            PsiClass psiClass = findClass(qualifiedName);
            if (psiClass == null) {
                return null;
            }
            classData = createJavaClassDescriptor(psiClass);
        }
        return classData.getClassDescriptor();
    }

    private ResolverClassData createJavaClassDescriptor(@NotNull final PsiClass psiClass) {
        assert !classDescriptorCache.containsKey(psiClass.getQualifiedName()) : psiClass.getQualifiedName();
        classDescriptorCache.put(psiClass.getQualifiedName(), null); // TODO

        String name = psiClass.getName();
        ResolverClassData classData = new ResolverClassData();
        ClassKind kind = psiClass.isInterface() ? ClassKind.TRAIT : ClassKind.CLASS;
        classData.classDescriptor = new MutableClassDescriptorLite(
                resolveParentDescriptor(psiClass), kind
        );
        classData.classDescriptor.setName(name);
        
        class OuterClassTypeVariableResolver implements TypeVariableResolver {

            @NotNull
            @Override
            public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
                throw new IllegalStateException("not implemented"); // TODO
            }
        }

        List<JetType> supertypes = new ArrayList<JetType>();

        List<TypeParameterDescriptor> typeParameters = resolveClassTypeParameters(psiClass, classData, new OuterClassTypeVariableResolver());
        classData.classDescriptor.setTypeParameterDescriptors(typeParameters);
        classData.classDescriptor.setSupertypes(supertypes);
        classData.classDescriptor.setVisibility(resolveVisibilityFromPsiModifiers(psiClass));
        classData.classDescriptor.setModality(Modality.convertFromFlags(
                psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface(),
                !psiClass.hasModifierProperty(PsiModifier.FINAL))
        );
        classData.classDescriptor.createTypeConstructor();
        classDescriptorCache.put(psiClass.getQualifiedName(), classData);
        classData.classDescriptor.setScopeForMemberLookup(new JavaClassMembersScope(classData.classDescriptor, psiClass, semanticServices, false));

        initializeTypeParameters(psiClass);

        // TODO: ugly hack: tests crash if initializeTypeParameters called with class containing proper supertypes
        supertypes.addAll(getSupertypes(psiClass));

        if (psiClass.isInterface()) {
            //classData.classDescriptor.setSuperclassType(JetStandardClasses.getAnyType()); // TODO : Make it java.lang.Object
        }
        else {
            PsiClassType[] extendsListTypes = psiClass.getExtendsListTypes();
            assert extendsListTypes.length == 0 || extendsListTypes.length == 1;
            JetType superclassType = extendsListTypes.length == 0
                                            ? JetStandardClasses.getAnyType()
                                            : semanticServices.getTypeTransformer().transformToType(extendsListTypes[0]);
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
                        new TypeParameterListTypeVariableResolver(typeParameters) // TODO: outer too
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

        semanticServices.getTrace().record(BindingContext.CLASS, psiClass, classData.classDescriptor);

        return classData;
    }

    private List<TypeParameterDescriptor> resolveClassTypeParameters(PsiClass psiClass, ResolverClassData classData, TypeVariableResolver typeVariableResolver) {
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
        
        private final DeclarationDescriptor containingDeclaration;
        private final PsiTypeParameterListOwner psiOwner;
        private final String name;
        private final int index;
        private final TypeInfoVariance variance;
        private final TypeVariableResolver typeVariableResolver;

        protected JetSignatureTypeParameterVisitor(DeclarationDescriptor containingDeclaration, PsiTypeParameterListOwner psiOwner,
                String name, int index, TypeInfoVariance variance, TypeVariableResolver typeVariableResolver)
        {
            if (name.isEmpty()) {
                throw new IllegalStateException();
            }
            
            this.containingDeclaration = containingDeclaration;
            this.psiOwner = psiOwner;
            this.name = name;
            this.index = index;
            this.variance = variance;
            this.typeVariableResolver = typeVariableResolver;
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
            TypeParameterDescriptor typeParameter = TypeParameterDescriptor.createForFurtherModification(
                    containingDeclaration,
                    Collections.<AnnotationDescriptor>emptyList(), // TODO: wrong
                    true, // TODO: wrong
                    JetSignatureUtils.translateVariance(variance),
                    name,
                    index);
            PsiTypeParameter psiTypeParameter = getPsiTypeParameterByName(psiOwner, name);
            typeParameterDescriptorCache.put(psiTypeParameter, new TypeParameterDescriptorInitialization(typeParameter, upperBounds, lowerBounds));
            done(typeParameter);
        }
        
        protected abstract void done(TypeParameterDescriptor typeParameterDescriptor);
    }

    /**
     * @see #resolveMethodTypeParametersFromJetSignature(String, FunctionDescriptor)
     */
    private List<TypeParameterDescriptor> resolveClassTypeParametersFromJetSignature(String jetSignature, final PsiClass clazz,
            final ClassDescriptor classDescriptor, final TypeVariableResolver outerClassTypeVariableResolver) {
        final List<TypeParameterDescriptor> r = new ArrayList<TypeParameterDescriptor>();
        
        class MyTypeVariableResolver implements TypeVariableResolver {

            @NotNull
            @Override
            public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
                for (TypeParameterDescriptor typeParameter : r) {
                    if (typeParameter.getName().equals(name)) {
                        return typeParameter;
                    }
                }
                return outerClassTypeVariableResolver.getTypeVariable(name);
            }
        }
        
        new JetSignatureReader(jetSignature).accept(new JetSignatureExceptionsAdapter() {
            private int formalTypeParameterIndex = 0;
            
            @Override
            public JetSignatureVisitor visitFormalTypeParameter(final String name, final TypeInfoVariance variance) {
                return new JetSignatureTypeParameterVisitor(classDescriptor, clazz, name, formalTypeParameterIndex++, variance, new MyTypeVariableResolver()) {
                    @Override
                    protected void done(TypeParameterDescriptor typeParameterDescriptor) {
                        r.add(typeParameterDescriptor);
                    }
                };
            }

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
        });
        return r;
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

    private List<TypeParameterDescriptor> makeUninitializedTypeParameters(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameter[] typeParameters) {
        List<TypeParameterDescriptor> result = Lists.newArrayList();
        for (PsiTypeParameter typeParameter : typeParameters) {
            TypeParameterDescriptor typeParameterDescriptor = makeUninitializedTypeParameter(containingDeclaration, typeParameter);
            result.add(typeParameterDescriptor);
        }
        return result;
    }

    @NotNull
    private TypeParameterDescriptor makeUninitializedTypeParameter(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameter psiTypeParameter) {
        assert typeParameterDescriptorCache.get(psiTypeParameter) == null : psiTypeParameter.getText();
        TypeParameterDescriptor typeParameterDescriptor = TypeParameterDescriptor.createForFurtherModification(
                containingDeclaration,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                false,
                Variance.INVARIANT,
                psiTypeParameter.getName(),
                psiTypeParameter.getIndex()
        );
        typeParameterDescriptorCache.put(psiTypeParameter, new TypeParameterDescriptorInitialization(typeParameterDescriptor));
        return typeParameterDescriptor;
    }

    private void initializeTypeParameter(PsiTypeParameter typeParameter, TypeParameterDescriptorInitialization typeParameterDescriptorInitialization) {
        TypeParameterDescriptor typeParameterDescriptor = typeParameterDescriptorInitialization.descriptor;
        if (typeParameterDescriptorInitialization.origin == TypeParameterDescriptorOrigin.KOTLIN) {
            List<?> upperBounds = typeParameterDescriptorInitialization.upperBoundsForKotlin;
            if (upperBounds.size() == 0){
                typeParameterDescriptor.addUpperBound(JetStandardClasses.getNullableAnyType());
            } else {
                for (JetType upperBound : typeParameterDescriptorInitialization.upperBoundsForKotlin) {
                    typeParameterDescriptor.addUpperBound(upperBound);
                }
            }

            // TODO: lower bounds
        } else {
            PsiClassType[] referencedTypes = typeParameter.getExtendsList().getReferencedTypes();
            if (referencedTypes.length == 0){
                typeParameterDescriptor.addUpperBound(JetStandardClasses.getNullableAnyType());
            }
            else if (referencedTypes.length == 1) {
                typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer().transformToType(referencedTypes[0]));
            }
            else {
                for (PsiClassType referencedType : referencedTypes) {
                    typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer().transformToType(referencedType));
                }
            }
        }
        typeParameterDescriptor.setInitialized();
    }

    private void initializeTypeParameters(PsiTypeParameterListOwner typeParameterListOwner) {
        for (PsiTypeParameter psiTypeParameter : typeParameterListOwner.getTypeParameters()) {
            initializeTypeParameter(psiTypeParameter, resolveTypeParameterInitialization(psiTypeParameter));
        }
    }

    @NotNull
    private TypeParameterDescriptorInitialization resolveTypeParameter(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameter psiTypeParameter) {
        TypeParameterDescriptorInitialization typeParameterDescriptor = typeParameterDescriptorCache.get(psiTypeParameter);
        if (typeParameterDescriptor == null) {
            // TODO: report properly without crashing compiler
            throw new IllegalStateException("failed to resolve type parameter: " + psiTypeParameter.getName());
        }
        return typeParameterDescriptor;
    }

    private Collection<? extends JetType> getSupertypes(PsiClass psiClass) {
        List<JetType> result = new ArrayList<JetType>();
        result.add(JetStandardClasses.getAnyType());
        transformSupertypeList(result, psiClass.getExtendsListTypes());
        transformSupertypeList(result, psiClass.getImplementsListTypes());
        return result;
    }

    private void transformSupertypeList(List<JetType> result, PsiClassType[] extendsListTypes) {
        for (PsiClassType type : extendsListTypes) {
            JetType transform = semanticServices.getTypeTransformer().transformToType(type);

            result.add(TypeUtils.makeNotNullable(transform));
        }
    }

    public NamespaceDescriptor resolveNamespace(String qualifiedName) {
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

    private PsiClass findClass(String qualifiedName) {
        return javaFacade.findClass(qualifiedName, javaSearchScope);
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
                name == null ? JAVA_ROOT : psiPackage.getQualifiedName()
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

    private ResolverNamespaceData createJavaNamespaceDescriptor(@NotNull final PsiClass psiClass) {
        ResolverNamespaceData namespaceData = new ResolverNamespaceData();
        namespaceData.namespaceDescriptor = new JavaNamespaceDescriptor(
                resolveParentDescriptor(psiClass),
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                psiClass.getName(),
                psiClass.getQualifiedName()
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
            outType = semanticServices.getTypeTransformer().transformToType(psiType);
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

    public Set<VariableDescriptor> resolveFieldGroupByName(@NotNull DeclarationDescriptor owner, PsiClass psiClass, String fieldName, boolean staticMembers) {
        ResolverScopeData scopeData = getResolverScopeData(owner, new PsiClassWrapper(psiClass));

        NamedMembers namedMembers = scopeData.namedMembersMap.get(fieldName);
        if (namedMembers == null) {
            return Collections.emptySet();
        }

        resolveNamedGroupProperties(owner, staticMembers, namedMembers, fieldName);

        return namedMembers.propertyDescriptors;
    }
    
    @NotNull
    public Set<VariableDescriptor> resolveFieldGroup(@NotNull DeclarationDescriptor owner, PsiClass psiClass, boolean staticMembers) {

        ResolverScopeData scopeData = getResolverScopeData(owner, new PsiClassWrapper(psiClass));
        
        Set<VariableDescriptor> descriptors = Sets.newHashSet();
        Map<String, NamedMembers> membersForProperties = scopeData.namedMembersMap;
        for (Map.Entry<String, NamedMembers> entry : membersForProperties.entrySet()) {
            NamedMembers namedMembers = entry.getValue();
            if (namedMembers.properties == null) {
                continue;
            }
            
            String propertyName = entry.getKey();

            resolveNamedGroupProperties(owner, staticMembers, namedMembers, propertyName);
            descriptors.addAll(namedMembers.propertyDescriptors);
        }
        return descriptors;
    }

    private void resolveNamedGroupProperties(DeclarationDescriptor owner, boolean staticMembers, NamedMembers namedMembers, String propertyName) {
        if (namedMembers.propertyDescriptors != null) {
            return;
        }
        
        if (namedMembers.properties == null) {
            namedMembers.propertyDescriptors = Collections.emptySet();
            return;
        }
        
        TypeSource propertyType = namedMembers.properties.type;
        TypeSource receiver = namedMembers.properties.receiverType;
        MembersForProperty members = namedMembers.properties;

        boolean isFinal;
        if (members.setter == null && members.getter == null) {
            isFinal = false;
        } else if (members.getter != null) {
            isFinal = members.getter.isFinal();
        } else if (members.setter != null) {
            isFinal = members.setter.isFinal();
        } else {
            isFinal = false;
        }

        PsiMemberWrapper anyMember;
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
            isVar = !members.field.isFinal();
        } else {
            isVar = members.setter != null;
        }

        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                owner,
                Collections.<AnnotationDescriptor>emptyList(),
                isFinal && !staticMembers ? Modality.FINAL : Modality.OPEN, // TODO: abstract
                resolveVisibilityFromPsiModifiers(anyMember.psiMember),
                isVar,
                false,
                propertyName);

        PropertyGetterDescriptor getterDescriptor = null;
        PropertySetterDescriptor setterDescriptor = null;
        if (members.getter != null) {
            getterDescriptor = new PropertyGetterDescriptor(propertyDescriptor, Collections.<AnnotationDescriptor>emptyList(), Modality.OPEN, Visibility.PUBLIC, true, false);
        }
        if (members.setter != null) {
            setterDescriptor = new PropertySetterDescriptor(propertyDescriptor, Collections.<AnnotationDescriptor>emptyList(), Modality.OPEN, Visibility.PUBLIC, true, false);
        }

        propertyDescriptor.initialize(getterDescriptor, setterDescriptor);

        final List<TypeParameterDescriptor> classTypeParameters;
        if (anyMember instanceof PsiMethodWrapper && !anyMember.isStatic()) {
            classTypeParameters = ((ClassDescriptor) owner).getTypeConstructor().getParameters();
        } else {
            classTypeParameters = new ArrayList<TypeParameterDescriptor>(0);
        }
        TypeParameterListTypeVariableResolver typeVariableResolver = new TypeParameterListTypeVariableResolver(classTypeParameters);

        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(0);

        if (members.setter != null) {
            // call ugly code with side effects
            typeParameters = resolveMethodTypeParameters(members.setter, propertyDescriptor.getSetter(), typeVariableResolver);
        }
        if (members.getter != null) {
            // call ugly code with side effects
            typeParameters = resolveMethodTypeParameters(members.getter, propertyDescriptor.getGetter(), typeVariableResolver);
        }
        
        List<TypeParameterDescriptor> typeParametersForReceiver = new ArrayList<TypeParameterDescriptor>();
        typeParametersForReceiver.addAll(classTypeParameters);
        typeParametersForReceiver.addAll(typeParameters);

        TypeParameterListTypeVariableResolver typeVariableResolverForPropertyInternals = new TypeParameterListTypeVariableResolver(typeParametersForReceiver);
        
        JetType receiverJetType;
        if (receiver == null) {
            receiverJetType = null;
        } else if (receiver.getTypeString().length() > 0) {
            receiverJetType = semanticServices.getTypeTransformer().transformToType(receiver.getTypeString(), typeVariableResolverForPropertyInternals);
        } else {
            receiverJetType = semanticServices.getTypeTransformer().transformToType(receiver.getPsiType());
        }

        JetType type;
        if (propertyType.getTypeString().length() > 0) {
            type = semanticServices.getTypeTransformer().transformToType(propertyType.getTypeString(), typeVariableResolverForPropertyInternals);
        } else {
            type = semanticServices.getTypeTransformer().transformToType(propertyType.getPsiType());
        }

        propertyDescriptor.setType(
                type,
                typeParameters,
                DescriptorUtils.getExpectedThisObjectIfNeeded(owner),
                receiverJetType
            );
        if (getterDescriptor != null) {
            getterDescriptor.initialize(type);
        }
        if (setterDescriptor != null) {
            // TODO: initialize
        }

        semanticServices.getTrace().record(BindingContext.VARIABLE, anyMember.psiMember, propertyDescriptor);
        namedMembers.propertyDescriptors = Collections.<VariableDescriptor>singleton(propertyDescriptor);
    }

    private void resolveNamedGroupFunctions(DeclarationDescriptor owner, PsiClass psiClass, TypeSubstitutor typeSubstitutorForGenericSuperclasses, boolean staticMembers, NamedMembers namedMembers, String functionName) {
        if (namedMembers.functionDescriptors != null) {
            return;
        }

        if (namedMembers.methods == null) {
            namedMembers.functionDescriptors = Collections.emptySet();
            return;
        }

        Set<FunctionDescriptor> functionDescriptors = new HashSet<FunctionDescriptor>(namedMembers.methods.size());
        for (PsiMethodWrapper method : namedMembers.methods) {
            functionDescriptors.add(resolveMethodToFunctionDescriptor(owner, psiClass, typeSubstitutorForGenericSuperclasses, method));
        }
        namedMembers.functionDescriptors = functionDescriptors;
    }

    private ResolverScopeData getResolverScopeData(DeclarationDescriptor owner, PsiClassWrapper psiClass) {
        ResolverScopeData scopeData;
        boolean staticMembers;
        if (owner instanceof JavaNamespaceDescriptor) {
            scopeData = namespaceDescriptorCacheByFqn.get(((JavaNamespaceDescriptor) owner).getQualifiedName());
            staticMembers = true;
        } else if (owner instanceof ClassDescriptor) {
            scopeData = classDescriptorCache.get(psiClass.getQualifiedName());
            staticMembers = false;
        } else {
            throw new IllegalStateException();
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
    public Set<FunctionDescriptor> resolveFunctionGroup(@NotNull DeclarationDescriptor owner, @NotNull PsiClass psiClass, @Nullable ClassDescriptor classDescriptor, @NotNull String methodName, boolean staticMembers) {

        ResolverScopeData resolverScopeData = getResolverScopeData(owner, new PsiClassWrapper(psiClass));

        Map<String, NamedMembers> namedMembersMap = resolverScopeData.namedMembersMap;

        NamedMembers namedMembers = namedMembersMap.get(methodName);
        if (namedMembers == null || namedMembers.methods == null) {
            return Collections.emptySet();
        }

        TypeSubstitutor typeSubstitutor = createSubstitutorForGenericSupertypes(classDescriptor);
        resolveNamedGroupFunctions(owner, psiClass, typeSubstitutor, staticMembers, namedMembers, methodName);
        
        return namedMembers.functionDescriptors;
    }

    public TypeSubstitutor createSubstitutorForGenericSupertypes(ClassDescriptor classDescriptor) {
        TypeSubstitutor typeSubstitutor;
        if (classDescriptor != null) {
            typeSubstitutor = TypeUtils.buildDeepSubstitutor(classDescriptor.getDefaultType());
        }
        else {
            typeSubstitutor = TypeSubstitutor.EMPTY;
        }
        return typeSubstitutor;
    }

    private static class TypeParameterListTypeVariableResolver implements TypeVariableResolver {

        private final List<TypeParameterDescriptor> typeParameters;

        private TypeParameterListTypeVariableResolver(List<TypeParameterDescriptor> typeParameters) {
            this.typeParameters = typeParameters;
        }

        @NotNull
        @Override
        public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
            for (TypeParameterDescriptor typeParameter : typeParameters) {
                if (typeParameter.getName().equals(name)) {
                    return typeParameter;
                }
            }
            throw new IllegalStateException("unresolver variable: " + name); // TODO: report properly
        }
    }

    @Nullable
    public FunctionDescriptor resolveMethodToFunctionDescriptor(DeclarationDescriptor owner, PsiClass psiClass, TypeSubstitutor typeSubstitutorForGenericSuperclasses, PsiMethodWrapper method) {
        
        PsiType returnType = method.getReturnType();
        if (returnType == null) {
            return null;
        }
        FunctionDescriptor functionDescriptor = methodDescriptorCache.get(method.getPsiMethod());
        if (functionDescriptor != null) {
            if (method.getPsiMethod().getContainingClass() != psiClass) {
                functionDescriptor = functionDescriptor.substitute(typeSubstitutorForGenericSuperclasses);
            }
            return functionDescriptor;
        }

        boolean kotlin;
        if (owner instanceof JavaNamespaceDescriptor) {
            JavaNamespaceDescriptor javaNamespaceDescriptor = (JavaNamespaceDescriptor) owner;
            ResolverNamespaceData namespaceData = namespaceDescriptorCacheByFqn.get(javaNamespaceDescriptor.getQualifiedName());
            if (namespaceData == null) {
                throw new IllegalStateException("namespaceData not found by name " + javaNamespaceDescriptor.getQualifiedName());
            }
            kotlin = namespaceData.kotlin;
        } else {
            ResolverClassData classData = classDescriptorCache.get(psiClass.getQualifiedName());
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

        DeclarationDescriptor classDescriptor;
        final List<TypeParameterDescriptor> classTypeParameters;
        if (method.isStatic()) {
            classDescriptor = resolveNamespace(method.getPsiMethod().getContainingClass());
            classTypeParameters = Collections.emptyList();
        }
        else {
            ClassDescriptor classClassDescriptor = resolveClass(method.getPsiMethod().getContainingClass());
            classDescriptor = classClassDescriptor;
            classTypeParameters = classClassDescriptor.getTypeConstructor().getParameters();
        }
        if (classDescriptor == null) {
            return null;
        }
        NamedFunctionDescriptorImpl functionDescriptorImpl = new NamedFunctionDescriptorImpl(
                owner,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                method.getName()
        );
        methodDescriptorCache.put(method.getPsiMethod(), functionDescriptorImpl);

        // TODO: add outer classes
        TypeParameterListTypeVariableResolver typeVariableResolverForParameters = new TypeParameterListTypeVariableResolver(classTypeParameters);

        final List<TypeParameterDescriptor> methodTypeParameters = resolveMethodTypeParameters(method, functionDescriptorImpl, typeVariableResolverForParameters);

        class MethodTypeVariableResolver implements TypeVariableResolver {

            @NotNull
            @Override
            public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
                for (TypeParameterDescriptor typeParameter : methodTypeParameters) {
                    if (typeParameter.getName().equals(name)) {
                        return typeParameter;
                    }
                }
                for (TypeParameterDescriptor typeParameter : classTypeParameters) {
                    if (typeParameter.getName().equals(name)) {
                        return typeParameter;
                    }
                }
                throw new IllegalStateException("unresolver variable: " + name); // TODO: report properly
            }
        }


        ValueParameterDescriptors valueParameterDescriptors = resolveParameterDescriptors(functionDescriptorImpl, method.getParameters(), new MethodTypeVariableResolver());
        functionDescriptorImpl.initialize(
                valueParameterDescriptors.receiverType,
                DescriptorUtils.getExpectedThisObjectIfNeeded(classDescriptor),
                methodTypeParameters,
                valueParameterDescriptors.descriptors,
                makeReturnType(returnType, method, new MethodTypeVariableResolver()),
                Modality.convertFromFlags(method.getPsiMethod().hasModifierProperty(PsiModifier.ABSTRACT), !method.isFinal()),
                resolveVisibilityFromPsiModifiers(method.getPsiMethod())
        );
        semanticServices.getTrace().record(BindingContext.FUNCTION, method.getPsiMethod(), functionDescriptorImpl);
        FunctionDescriptor substitutedFunctionDescriptor = functionDescriptorImpl;
        if (method.getPsiMethod().getContainingClass() != psiClass) {
            substitutedFunctionDescriptor = functionDescriptorImpl.substitute(typeSubstitutorForGenericSuperclasses);
        }
        return substitutedFunctionDescriptor;
    }

    private List<TypeParameterDescriptor> resolveMethodTypeParameters(
            @NotNull PsiMethodWrapper method,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull TypeVariableResolver classTypeVariableResolver) {
        if (method.getJetMethod().typeParameters().length() > 0) {
            List<TypeParameterDescriptor> r = resolveMethodTypeParametersFromJetSignature(
                    method.getJetMethod().typeParameters(), method.getPsiMethod(), functionDescriptor, classTypeVariableResolver);
            initializeTypeParameters(method.getPsiMethod());
            return r;
        }

        List<TypeParameterDescriptor> typeParameters = makeUninitializedTypeParameters(functionDescriptor, method.getPsiMethod().getTypeParameters());
        initializeTypeParameters(method.getPsiMethod());
        return typeParameters;
    }

    /**
     * @see #resolveClassTypeParametersFromJetSignature(String, com.intellij.psi.PsiClass, MutableClassDescriptorLite)
     */
    private List<TypeParameterDescriptor> resolveMethodTypeParametersFromJetSignature(String jetSignature, final PsiMethod method,
            final FunctionDescriptor functionDescriptor, final TypeVariableResolver classTypeVariableResolver)
    {
        final List<TypeParameterDescriptor> r = new ArrayList<TypeParameterDescriptor>();
        
        class MyTypeVariableResolver implements TypeVariableResolver {

            @NotNull
            @Override
            public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
                for (TypeParameterDescriptor typeParameter : r) {
                    if (typeParameter.getName().equals(name)) {
                        return typeParameter;
                    }
                }
                return classTypeVariableResolver.getTypeVariable(name);
            }
        }
        
        new JetSignatureReader(jetSignature).acceptFormalTypeParametersOnly(new JetSignatureExceptionsAdapter() {
            private int formalTypeParameterIndex = 0;
            
            @Override
            public JetSignatureVisitor visitFormalTypeParameter(final String name, final TypeInfoVariance variance) {
                
                return new JetSignatureTypeParameterVisitor(functionDescriptor, method, name, formalTypeParameterIndex++, variance, new MyTypeVariableResolver()) {
                    @Override
                    protected void done(TypeParameterDescriptor typeParameterDescriptor) {
                        r.add(typeParameterDescriptor);
                    }
                };

            }
        });
        return r;
    }

    private JetType makeReturnType(PsiType returnType, PsiMethodWrapper method, TypeVariableResolver typeVariableResolver) {

        String returnTypeFromAnnotation = method.getJetMethod().returnType();

        JetType transformedType;
        if (returnTypeFromAnnotation.length() > 0) {
            transformedType = semanticServices.getTypeTransformer().transformToType(returnTypeFromAnnotation, typeVariableResolver);
        } else {
            transformedType = semanticServices.getTypeTransformer().transformToType(returnType);
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

    @NotNull
    private TypeParameterDescriptorInitialization resolveTypeParameterInitialization(PsiTypeParameter typeParameter) {
        PsiTypeParameterListOwner owner = typeParameter.getOwner();
        if (owner instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) owner;
            return resolveTypeParameter(resolveClass(psiClass), typeParameter);
        }
        if (owner instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) owner;
            PsiClass containingClass = psiMethod.getContainingClass();
            DeclarationDescriptor ownerOwner;
            TypeSubstitutor substitutorForGenericSupertypes;
            if (psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                substitutorForGenericSupertypes = TypeSubstitutor.EMPTY;
                return resolveTypeParameter(JAVA_METHOD_TYPE_PARAMETER_PARENT, typeParameter);
            }
            else {
                ClassDescriptor classDescriptor = resolveClass(containingClass);
                ownerOwner = classDescriptor;
                substitutorForGenericSupertypes = semanticServices.getDescriptorResolver().createSubstitutorForGenericSupertypes(classDescriptor);
            }
            FunctionDescriptor functionDescriptor = resolveMethodToFunctionDescriptor(ownerOwner, containingClass, substitutorForGenericSupertypes, new PsiMethodWrapper(psiMethod));
            return resolveTypeParameter(functionDescriptor, typeParameter);
        }
        throw new IllegalStateException("Unknown parent type: " + owner);
    }

    public TypeParameterDescriptor resolveTypeParameter(PsiTypeParameter typeParameter) {
        return resolveTypeParameterInitialization(typeParameter).descriptor;
    }

    public List<ClassDescriptor> resolveInnerClasses(DeclarationDescriptor owner, PsiClass psiClass, boolean staticMembers) {
        PsiClass[] innerPsiClasses = psiClass.getInnerClasses();
        List<ClassDescriptor> r = new ArrayList<ClassDescriptor>(innerPsiClasses.length);
        for (PsiClass innerPsiClass : innerPsiClasses) {
            r.add(resolveClass(innerPsiClass));
        }
        return r;
    }
}
