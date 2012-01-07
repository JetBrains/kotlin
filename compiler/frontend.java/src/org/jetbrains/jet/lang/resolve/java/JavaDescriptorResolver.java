package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import jet.typeinfo.TypeInfoVariance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
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
        @Nullable
        private Set<VariableDescriptor> properties;
        private boolean kotlin;
    }

    private static class ResolverClassData extends ResolverScopeData {
        private JavaClassDescriptor classDescriptor;
        private boolean kotlin;

        @NotNull
        public ClassDescriptor getClassDescriptor() {
            return classDescriptor;
        }
    }

    private static class ResolverNamespaceData extends ResolverScopeData {
        private JavaNamespaceDescriptor namespaceDescriptor;
        private boolean kotlin;

        @NotNull
        public NamespaceDescriptor getNamespaceDescriptor() {
            return namespaceDescriptor;
        }
    }

    protected final Map<String, ResolverClassData> classDescriptorCache = Maps.newHashMap();
    protected final Map<String, ResolverNamespaceData> namespaceDescriptorCacheByFqn = Maps.newHashMap();
    protected final Map<PsiElement, ResolverNamespaceData> namespaceDescriptorCache = Maps.newHashMap();

    protected final Map<PsiTypeParameter, TypeParameterDescriptorInitialization> typeParameterDescriptorCache = Maps.newHashMap();
    protected final Map<PsiMethod, FunctionDescriptor> methodDescriptorCache = Maps.newHashMap();
    protected final Map<PsiField, VariableDescriptor> fieldDescriptorCache = Maps.newHashMap();
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
        classData.classDescriptor = new JavaClassDescriptor(
                resolveParentDescriptor(psiClass), psiClass.isInterface() ? ClassKind.TRAIT : ClassKind.CLASS
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
        classData.classDescriptor.setTypeConstructor(new TypeConstructorImpl(
                classData.classDescriptor,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                // TODO
                psiClass.hasModifierProperty(PsiModifier.FINAL),
                name,
                typeParameters,
                supertypes
        ));
        classData.classDescriptor.setModality(Modality.convertFromFlags(
                psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface(),
                !psiClass.hasModifierProperty(PsiModifier.FINAL))
        );
        classData.classDescriptor.setVisibility(resolveVisibilityFromPsiModifiers(psiClass));
        classDescriptorCache.put(psiClass.getQualifiedName(), classData);
        classData.classDescriptor.setUnsubstitutedMemberScope(new JavaClassMembersScope(classData.classDescriptor, psiClass, semanticServices, false));

        // UGLY HACK (Andrey Breslav is not sure what did he mean)
        initializeTypeParameters(psiClass);

        supertypes.addAll(getSupertypes(psiClass));
        if (psiClass.isInterface()) {
            classData.classDescriptor.setSuperclassType(JetStandardClasses.getAnyType()); // TODO : Make it java.lang.Object
        }
        else {
            PsiClassType[] extendsListTypes = psiClass.getExtendsListTypes();
            assert extendsListTypes.length == 0 || extendsListTypes.length == 1;
            JetType superclassType = extendsListTypes.length == 0
                                            ? JetStandardClasses.getAnyType()
                                            : semanticServices.getTypeTransformer().transformToType(extendsListTypes[0]);
            classData.classDescriptor.setSuperclassType(superclassType);
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
                classData.classDescriptor.addConstructor(constructorDescriptor);
                semanticServices.getTrace().record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
            }
        }
        else {
            for (PsiMethod constructor : psiConstructors) {
                ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                        classData.classDescriptor,
                        Collections.<AnnotationDescriptor>emptyList(), // TODO
                        false);
                ValueParameterDescriptors valueParameterDescriptors = resolveParameterDescriptors(constructorDescriptor,
                        constructor.getParameterList().getParameters(),
                        new TypeParameterListTypeVariableResolver(typeParameters) // TODO: outer too
                    );
                if (valueParameterDescriptors.receiverType != null) {
                    throw new IllegalStateException();
                }
                constructorDescriptor.initialize(typeParameters, valueParameterDescriptors.descriptors, Modality.FINAL,
                                                 resolveVisibilityFromPsiModifiers(constructor));
                constructorDescriptor.setReturnType(classData.classDescriptor.getDefaultType());
                classData.classDescriptor.addConstructor(constructorDescriptor);
                semanticServices.getTrace().record(BindingContext.CONSTRUCTOR, constructor, constructorDescriptor);
            }
        }

        semanticServices.getTrace().record(BindingContext.CLASS, psiClass, classData.classDescriptor);

        return classData;
    }

    private List<TypeParameterDescriptor> resolveClassTypeParameters(PsiClass psiClass, ResolverClassData classData, TypeVariableResolver typeVariableResolver) {
        for (PsiAnnotation annotation : psiClass.getModifierList().getAnnotations()) {
            if (annotation.getQualifiedName().equals(JvmStdlibNames.JET_CLASS.getFqName())) {
                classData.kotlin = true;
                PsiLiteralExpression attributeValue = (PsiLiteralExpression) annotation.findAttributeValue(JvmStdlibNames.JET_CLASS_SIGNATURE);
                if (attributeValue != null) {
                    String typeParametersString = (String) attributeValue.getValue();
                    if (typeParametersString != null) {
                        return resolveClassTypeParametersFromJetSignature(typeParametersString, psiClass, classData.classDescriptor, typeVariableResolver);
                    }
                }
            }
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
        private final TypeInfoVariance variance;
        private final TypeVariableResolver typeVariableResolver;

        protected JetSignatureTypeParameterVisitor(DeclarationDescriptor containingDeclaration, PsiTypeParameterListOwner psiOwner,
                String name, TypeInfoVariance variance, TypeVariableResolver typeVariableResolver)
        {
            if (name.isEmpty()) {
                throw new IllegalStateException();
            }
            
            this.containingDeclaration = containingDeclaration;
            this.psiOwner = psiOwner;
            this.name = name;
            this.variance = variance;
            this.typeVariableResolver = typeVariableResolver;
        }

        int index = 0;

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
                    ++index);
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
            final JavaClassDescriptor classDescriptor, final TypeVariableResolver outerClassTypeVariableResolver) {
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
            @Override
            public JetSignatureVisitor visitFormalTypeParameter(final String name, final TypeInfoVariance variance) {
                return new JetSignatureTypeParameterVisitor(classDescriptor, clazz, name, variance, new MyTypeVariableResolver()) {
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
        assert typeParameterDescriptor != null : psiTypeParameter.getText();
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

    public ValueParameterDescriptors resolveParameterDescriptors(DeclarationDescriptor containingDeclaration,
            PsiParameter[] parameters, TypeVariableResolver typeVariableResolver) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        JetType receiverType = null;
        for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
            PsiParameter parameter = parameters[i];
            JvmMethodParameterMeaning meaning = resolveParameterDescriptor(containingDeclaration, i, parameter, typeVariableResolver);
            if (meaning.kind == JvmMethodParameterKind.TYPE_INFO) {
                // TODO
            } else if (meaning.kind == JvmMethodParameterKind.REGULAR) {
                result.add(meaning.valueParameterDescriptor);
            } else if (meaning.kind == JvmMethodParameterKind.RECEIVER) {
                if (receiverType != null) {
                    throw new IllegalStateException("more then one receiver");
                }
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
            PsiParameter parameter, TypeVariableResolver typeVariableResolver) {
        PsiType psiType = parameter.getType();

        JetType varargElementType;
        if (psiType instanceof PsiEllipsisType) {
            PsiEllipsisType psiEllipsisType = (PsiEllipsisType) psiType;
            varargElementType = semanticServices.getTypeTransformer().transformToType(psiEllipsisType.getComponentType());
        }
        else {
            varargElementType = null;
        }

        boolean changeNullable = false;
        boolean nullable = true;
        String typeFromAnnotation = null;
        
        boolean receiver = false;
        boolean hasDefaultValue = false;
        
        // TODO: must be very slow, make it lazy?
        String name = parameter.getName() != null ? parameter.getName() : "p" + i;
        for (PsiAnnotation annotation : parameter.getModifierList().getAnnotations()) {

            if (annotation.getQualifiedName().equals(JvmStdlibNames.JET_VALUE_PARAMETER.getFqName())) {
                PsiLiteralExpression nameExpression = (PsiLiteralExpression) annotation.findAttributeValue(JvmStdlibNames.JET_VALUE_PARAMETER_NAME_FIELD);
                if (nameExpression != null) {
                    name = (String) nameExpression.getValue();
                }
                
                PsiLiteralExpression nullableExpression = (PsiLiteralExpression) annotation.findAttributeValue(JvmStdlibNames.JET_VALUE_PARAMETER_NULLABLE_FIELD);
                if (nullableExpression != null) {
                    nullable = (Boolean) nullableExpression.getValue();
                } else {
                    // default value of parameter
                    nullable = false;
                    changeNullable = true;
                }
                
                PsiLiteralExpression signatureExpression = (PsiLiteralExpression) annotation.findAttributeValue(JvmStdlibNames.JET_VALUE_PARAMETER_TYPE_FIELD);
                if (signatureExpression != null) {
                    typeFromAnnotation = (String) signatureExpression.getValue();
                }


                PsiLiteralExpression receiverExpression = (PsiLiteralExpression) annotation.findAttributeValue(JvmStdlibNames.JET_VALUE_PARAMETER_RECEIVER_FIELD);
                if (receiverExpression != null) {
                    receiver = (Boolean) receiverExpression.getValue();
                }
                
                PsiLiteralExpression hasDefaultValueExpression = (PsiLiteralExpression) annotation.findAttributeValue(JvmStdlibNames.JET_VALUE_PARAMETER_HAS_DEFAULT_VALUE_FIELD);
                if (hasDefaultValueExpression != null) {
                    hasDefaultValue = (Boolean) hasDefaultValueExpression.getValue();
                }


            } else if (annotation.getQualifiedName().equals(JvmStdlibNames.JET_TYPE_PARAMETER.getFqName())) {
                return JvmMethodParameterMeaning.typeInfo(new Object());
            }
        }
        
        JetType outType;
        if (typeFromAnnotation != null && typeFromAnnotation.length() > 0) {
            outType = semanticServices.getTypeTransformer().transformToType(typeFromAnnotation, typeVariableResolver);
        } else {
            outType = semanticServices.getTypeTransformer().transformToType(psiType);
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
                    changeNullable ? TypeUtils.makeNullableAsSpecified(outType, nullable) : outType,
                    hasDefaultValue,
                    varargElementType
            ));
        }
    }

    /*
    public VariableDescriptor resolveFieldToVariableDescriptor(DeclarationDescriptor containingDeclaration, PsiField field) {
        VariableDescriptor variableDescriptor = fieldDescriptorCache.get(field);
        if (variableDescriptor != null) {
            return variableDescriptor;
        }
        JetType type = semanticServices.getTypeTransformer().transformToType(field.getType());
        boolean isFinal = field.hasModifierProperty(PsiModifier.FINAL);
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                containingDeclaration,
                Collections.<AnnotationDescriptor>emptyList(),
                Modality.FINAL,
                resolveVisibilityFromPsiModifiers(field),
                !isFinal,
                null,
                DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration),
                field.getName(),
                type);
        semanticServices.getTrace().record(BindingContext.VARIABLE, field, propertyDescriptor);
        fieldDescriptorCache.put(field, propertyDescriptor);
        return propertyDescriptor;
    }
    */
    
    private static class PropertyKey {
        @NotNull
        private final String name;
        @NotNull
        private final PsiType type;

        private PropertyKey(String name, PsiType type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            // generated by Idea
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PropertyKey that = (PropertyKey) o;

            if (!name.equals(that.name)) return false;
            if (!type.equals(that.type)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }
    }

    private static class MembersForProperty {
        private PsiField field;
        private PsiMethod setter;
        private PsiMethod getter;
    }
    
    private Map<PropertyKey, MembersForProperty> getMembersForProperties(@NotNull PsiClass clazz, boolean staticMembers, boolean kotlin) {
        Map<PropertyKey, MembersForProperty> membersMap = Maps.newHashMap();
        if (!kotlin) {
            for (PsiField field : clazz.getFields()) {
                if (field.getModifierList().hasExplicitModifier(PsiModifier.STATIC) != staticMembers) {
                    continue;
                }

                if (field.hasModifierProperty(PsiModifier.PRIVATE)) {
                    continue;
                }

                MembersForProperty members = new MembersForProperty();
                members.field = field;
                membersMap.put(new PropertyKey(field.getName(), field.getType()), members);
            }
        }
        
        for (PsiMethod method : clazz.getMethods()) {
            if (method.getModifierList().hasExplicitModifier(PsiModifier.STATIC) != staticMembers) {
                continue;
            }

            if (method.hasModifierProperty(PsiModifier.PRIVATE)) {
                continue;
            }

            // TODO: "is" prefix
            if (method.getName().startsWith(JvmAbi.GETTER_PREFIX)) {
                // TODO: some java properties too
                if (method.getModifierList().findAnnotation(JvmStdlibNames.JET_PROPERTY.getFqName()) != null) {
                    if (method.getParameterList().getParametersCount() == 0) {
                        if (method.getName().equals(JvmStdlibNames.JET_OBJECT_GET_TYPEINFO_METHOD)) {
                            continue;
                        }

                        String propertyName = StringUtil.decapitalize(method.getName().substring(JvmAbi.GETTER_PREFIX.length()));
                        PropertyKey key = new PropertyKey(propertyName, method.getReturnType());
                        MembersForProperty members = membersMap.get(key);
                        if (members == null) {
                            members = new MembersForProperty();
                            membersMap.put(key, members);
                        }
                        members.getter = method;
                    }
                }
            } else if (method.getName().startsWith(JvmAbi.SETTER_PREFIX)) {
                if (method.getModifierList().findAnnotation(JvmStdlibNames.JET_PROPERTY.getFqName()) != null) {
                    if (method.getParameterList().getParametersCount() == 1) {
                        String propertyName = StringUtil.decapitalize(method.getName().substring(JvmAbi.SETTER_PREFIX.length()));
                        PropertyKey key = new PropertyKey(propertyName, method.getParameterList().getParameters()[0].getType());
                        MembersForProperty members = membersMap.get(key);
                        if (members == null) {
                            members = new MembersForProperty();
                            membersMap.put(key, members);
                        }
                        members.setter = method;
                    }
                }
            }
        }
        
        return membersMap;
    }
    
    public Set<VariableDescriptor> resolveFieldGroupByName(@NotNull DeclarationDescriptor owner, PsiClass psiClass, String fieldName, boolean staticMembers) {
        Set<VariableDescriptor> r = Sets.newHashSet();
        // TODO: slow
        Set<VariableDescriptor> variables = resolveFieldGroup(owner, psiClass, staticMembers);
        for (VariableDescriptor variable : variables) {
            if (variable.getName().equals(fieldName)) {
                r.add(variable);
            }
        }
        return r;
    }

    @NotNull
    public Set<VariableDescriptor> resolveFieldGroup(@NotNull DeclarationDescriptor owner, PsiClass psiClass, boolean staticMembers) {
        
        ResolverScopeData scopeData;
        if (owner instanceof JavaNamespaceDescriptor) {
            scopeData = namespaceDescriptorCacheByFqn.get(((JavaNamespaceDescriptor) owner).getQualifiedName());
        } else if (owner instanceof JavaClassDescriptor) {
            scopeData = classDescriptorCache.get(psiClass.getQualifiedName());
        } else {
            throw new IllegalStateException();
        }
        if (scopeData == null) {
            throw new IllegalStateException();
        }
        
        if (scopeData.properties != null) {
            return scopeData.properties;
        }
        
        Set<VariableDescriptor> descriptors = Sets.newHashSet();
        for (Map.Entry<PropertyKey, MembersForProperty> entry : getMembersForProperties(psiClass, staticMembers, scopeData.kotlin).entrySet()) {
            //VariableDescriptor variableDescriptor = fieldDescriptorCache.get(field);
            //if (variableDescriptor != null) {
            //    return variableDescriptor;
            //}
            String propertyName = entry.getKey().name;
            PsiType propertyType = entry.getKey().type;
            MembersForProperty members = entry.getValue();

            JetType type = semanticServices.getTypeTransformer().transformToType(propertyType);
            boolean isFinal;
            if (members.setter == null && members.getter == null) {
                isFinal = members.field.hasModifierProperty(PsiModifier.FINAL);
            } else if (members.getter != null) {
                isFinal = members.getter.hasModifierProperty(PsiModifier.FINAL);
            } else if (members.setter != null) {
                isFinal = members.setter.hasModifierProperty(PsiModifier.FINAL);
            } else {
                isFinal = false;
            }
            
            PsiMember anyMember;
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
                isVar = true;
            } else {
                isVar = members.setter != null;
            }
            
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                    owner,
                    Collections.<AnnotationDescriptor>emptyList(),
                    isFinal && !staticMembers ? Modality.FINAL : Modality.OPEN, // TODO: abstract
                    resolveVisibilityFromPsiModifiers(anyMember),
                    isVar, 
                    null,
                    DescriptorUtils.getExpectedThisObjectIfNeeded(owner),
                    propertyName,
                    type);
            semanticServices.getTrace().record(BindingContext.VARIABLE, anyMember, propertyDescriptor);
            //fieldDescriptorCache.put(field, propertyDescriptor);
            descriptors.add(propertyDescriptor);
        }
        scopeData.properties = descriptors;
        return descriptors;
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroup(@NotNull DeclarationDescriptor owner, @NotNull PsiClass psiClass, @Nullable ClassDescriptor classDescriptor, @NotNull String methodName, boolean staticMembers) {
        Set<FunctionDescriptor> writableFunctionGroup = Sets.newLinkedHashSet();
        final Collection<HierarchicalMethodSignature> signatures = psiClass.getVisibleSignatures();
        TypeSubstitutor typeSubstitutor = createSubstitutorForGenericSupertypes(classDescriptor);
        for (HierarchicalMethodSignature signature: signatures) {
            if (!methodName.equals(signature.getName())) {
                 continue;
            }

            FunctionDescriptor substitutedFunctionDescriptor = resolveHierarchicalSignatureToFunction(owner, psiClass, staticMembers, typeSubstitutor, signature);
            if (substitutedFunctionDescriptor != null) {
                writableFunctionGroup.add(substitutedFunctionDescriptor);
            }
        }
        return writableFunctionGroup;
    }

    @Nullable
    private FunctionDescriptor resolveHierarchicalSignatureToFunction(DeclarationDescriptor owner, PsiClass psiClass, boolean staticMembers, TypeSubstitutor typeSubstitutor, HierarchicalMethodSignature signature) {
        PsiMethod method = signature.getMethod();
        if (method.hasModifierProperty(PsiModifier.STATIC) != staticMembers) {
                return null;
        }
        FunctionDescriptor functionDescriptor = resolveMethodToFunctionDescriptor(owner, psiClass, typeSubstitutor, method);
//        if (functionDescriptor != null && !staticMembers) {
//            for (HierarchicalMethodSignature superSignature : signature.getSuperSignatures()) {
//                ((FunctionDescriptorImpl) functionDescriptor).addOverriddenFunction(resolveHierarchicalSignatureToFunction(owner, superSignature.getMethod().getContainingClass(), false, typeSubstitutor, superSignature));
//            }
//        }
        return functionDescriptor;
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
    public FunctionDescriptor resolveMethodToFunctionDescriptor(DeclarationDescriptor owner, PsiClass psiClass, TypeSubstitutor typeSubstitutorForGenericSuperclasses, PsiMethod method) {
        PsiType returnType = method.getReturnType();
        if (returnType == null) {
            return null;
        }
        FunctionDescriptor functionDescriptor = methodDescriptorCache.get(method);
        if (functionDescriptor != null) {
            if (method.getContainingClass() != psiClass) {
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

        // TODO: hide getters and setters properly
        if (kotlin) {
            if (method.getName().startsWith(JvmAbi.GETTER_PREFIX) && method.getParameterList().getParametersCount() == 0) {
                if (method.getModifierList().findAnnotation(JvmStdlibNames.JET_PROPERTY.getFqName()) != null)
                    return null;
            }
            if (method.getName().startsWith(JvmAbi.SETTER_PREFIX) && method.getParameterList().getParametersCount() == 1) {
                if (method.getModifierList().findAnnotation(JvmStdlibNames.JET_PROPERTY.getFqName()) != null)
                    return null;
            }
        }
        
        DeclarationDescriptor classDescriptor;
        final List<TypeParameterDescriptor> classTypeParameters;
        if (method.hasModifierProperty(PsiModifier.STATIC)) {
            classDescriptor = resolveNamespace(method.getContainingClass());
            classTypeParameters = Collections.emptyList();
        }
        else {
            ClassDescriptor classClassDescriptor = resolveClass(method.getContainingClass());
            classDescriptor = classClassDescriptor;
            classTypeParameters = classClassDescriptor.getTypeConstructor().getParameters();
        }
        if (classDescriptor == null) {
            return null;
        }
        PsiParameter[] parameters = method.getParameterList().getParameters();
        FunctionDescriptorImpl functionDescriptorImpl = new FunctionDescriptorImpl(
                owner,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                method.getName()
        );
        methodDescriptorCache.put(method, functionDescriptorImpl);

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


        ValueParameterDescriptors valueParameterDescriptors = resolveParameterDescriptors(functionDescriptorImpl, parameters, new MethodTypeVariableResolver());
        functionDescriptorImpl.initialize(
                valueParameterDescriptors.receiverType,
                DescriptorUtils.getExpectedThisObjectIfNeeded(classDescriptor),
                methodTypeParameters,
                valueParameterDescriptors.descriptors,
                makeReturnType(returnType, method, new MethodTypeVariableResolver()),
                Modality.convertFromFlags(method.hasModifierProperty(PsiModifier.ABSTRACT), !method.hasModifierProperty(PsiModifier.FINAL)),
                resolveVisibilityFromPsiModifiers(method)
        );
        semanticServices.getTrace().record(BindingContext.FUNCTION, method, functionDescriptorImpl);
        FunctionDescriptor substitutedFunctionDescriptor = functionDescriptorImpl;
        if (method.getContainingClass() != psiClass) {
            substitutedFunctionDescriptor = functionDescriptorImpl.substitute(typeSubstitutorForGenericSuperclasses);
        }
        return substitutedFunctionDescriptor;
    }

    private List<TypeParameterDescriptor> resolveMethodTypeParameters(PsiMethod method, FunctionDescriptorImpl functionDescriptorImpl, TypeVariableResolver classTypeVariableResolver) {
        for (PsiAnnotation annotation : method.getModifierList().getAnnotations()) {
            if (annotation.getQualifiedName().equals(JvmStdlibNames.JET_METHOD.getFqName())) {
                PsiLiteralExpression attributeValue = (PsiLiteralExpression) annotation.findAttributeValue(JvmStdlibNames.JET_METHOD_TYPE_PARAMETERS_FIELD);
                if (attributeValue != null) {
                    String typeParametersString = (String) attributeValue.getValue();
                    if (typeParametersString != null) {
                        List<TypeParameterDescriptor> r = resolveMethodTypeParametersFromJetSignature(typeParametersString, method, functionDescriptorImpl, classTypeVariableResolver);
                        initializeTypeParameters(method);
                        return r;
                    }
                }
            }
        }
        
        List<TypeParameterDescriptor> typeParameters = makeUninitializedTypeParameters(functionDescriptorImpl, method.getTypeParameters());
        initializeTypeParameters(method);
        return typeParameters;
    }

    /**
     * @see #resolveClassTypeParametersFromJetSignature(String, com.intellij.psi.PsiClass, JavaClassDescriptor) 
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
            @Override
            public JetSignatureVisitor visitFormalTypeParameter(final String name, final TypeInfoVariance variance) {
                
                return new JetSignatureTypeParameterVisitor(functionDescriptor, method, name, variance, new MyTypeVariableResolver()) {
                    @Override
                    protected void done(TypeParameterDescriptor typeParameterDescriptor) {
                        r.add(typeParameterDescriptor);
                    }
                };

            }
        });
        return r;
    }

    private JetType makeReturnType(PsiType returnType, PsiMethod method, TypeVariableResolver typeVariableResolver) {
        boolean changeNullable = false;
        boolean nullable = true;
        
        String returnTypeFromAnnotation = null;

        for (PsiAnnotation annotation : method.getModifierList().getAnnotations()) {
            if (annotation.getQualifiedName().equals(JvmStdlibNames.JET_METHOD.getFqName())) {
                PsiLiteralExpression nullableExpression = (PsiLiteralExpression) annotation.findAttributeValue(JvmStdlibNames.JET_METHOD_NULLABLE_RETURN_TYPE_FIELD);
                if (nullableExpression != null) {
                    nullable = (Boolean) nullableExpression.getValue();
                } else {
                    // default value of parameter
                    nullable = false;
                    changeNullable = true;
                }
                
                PsiLiteralExpression returnTypeExpression = (PsiLiteralExpression) annotation.findAttributeValue(JvmStdlibNames.JET_METHOD_RETURN_TYPE_FIELD);
                if (returnTypeExpression != null) {
                    returnTypeFromAnnotation = (String) returnTypeExpression.getValue();
                }
            }
        }
        JetType transformedType;
        if (returnTypeFromAnnotation != null && returnTypeFromAnnotation.length() > 0) {
            transformedType = semanticServices.getTypeTransformer().transformToType(returnTypeFromAnnotation, typeVariableResolver);
        } else {
            transformedType = semanticServices.getTypeTransformer().transformToType(returnType);
        }
        if (changeNullable) {
            return TypeUtils.makeNullableAsSpecified(transformedType, nullable);
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
            FunctionDescriptor functionDescriptor = resolveMethodToFunctionDescriptor(ownerOwner, containingClass, substitutorForGenericSupertypes, psiMethod);
            return resolveTypeParameter(functionDescriptor, typeParameter);
        }
        throw new IllegalStateException("Unknown parent type: " + owner);
    }

    public TypeParameterDescriptor resolveTypeParameter(PsiTypeParameter typeParameter) {
        return resolveTypeParameterInitialization(typeParameter).descriptor;
    }
}
