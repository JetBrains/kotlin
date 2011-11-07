package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.*;

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

    protected final Map<String, ClassDescriptor> classDescriptorCache = new HashMap<String, ClassDescriptor>();
    protected final Map<PsiTypeParameter, TypeParameterDescriptor> typeParameterDescriptorCache = Maps.newHashMap();
    protected final Map<PsiMethod, FunctionDescriptor> methodDescriptorCache = Maps.newHashMap();
    protected final Map<PsiField, VariableDescriptor> fieldDescriptorCache = Maps.newHashMap();
    protected final Map<PsiElement, NamespaceDescriptor> namespaceDescriptorCache = Maps.newHashMap();
    protected final JavaPsiFacade javaFacade;
    protected final GlobalSearchScope javaSearchScope;
    protected final JavaSemanticServices semanticServices;

    public JavaDescriptorResolver(Project project, JavaSemanticServices semanticServices) {
        this.javaFacade = JavaPsiFacade.getInstance(project);
        this.javaSearchScope = GlobalSearchScope.allScope(project);
        this.semanticServices = semanticServices;
    }

    @NotNull
    public ClassDescriptor resolveClass(@NotNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        ClassDescriptor classDescriptor = classDescriptorCache.get(qualifiedName);
        if (classDescriptor == null) {
            classDescriptor = createJavaClassDescriptor(psiClass);
            classDescriptorCache.put(qualifiedName, classDescriptor);
        }
        return classDescriptor;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull String qualifiedName) {
        ClassDescriptor classDescriptor = classDescriptorCache.get(qualifiedName);
        if (classDescriptor == null) {
            PsiClass psiClass = javaFacade.findClass(qualifiedName, javaSearchScope);
            if (psiClass == null) {
                return null;
            }
            classDescriptor = createJavaClassDescriptor(psiClass);
        }
        return classDescriptor;
    }

    private ClassDescriptor createJavaClassDescriptor(@NotNull final PsiClass psiClass) {
        assert !classDescriptorCache.containsKey(psiClass.getQualifiedName()) : psiClass.getQualifiedName();
        classDescriptorCache.put(psiClass.getQualifiedName(), null); // TODO

        String name = psiClass.getName();
        JavaClassDescriptor classDescriptor = new JavaClassDescriptor(
                resolveParentDescriptor(psiClass), psiClass.isInterface() ? ClassKind.TRAIT : ClassKind.CLASS
        );
        classDescriptor.setName(name);

        List<JetType> supertypes = new ArrayList<JetType>();
        List<TypeParameterDescriptor> typeParameters = makeUninitializedTypeParameters(classDescriptor, psiClass.getTypeParameters());
        classDescriptor.setTypeConstructor(new TypeConstructorImpl(
                classDescriptor,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                // TODO
                psiClass.hasModifierProperty(PsiModifier.FINAL),
                name,
                typeParameters,
                supertypes
        ));
        classDescriptor.setModality(Modality.convertFromFlags(
                psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface(),
                !psiClass.hasModifierProperty(PsiModifier.FINAL))
        );
        classDescriptor.setVisibility(resolveVisibilityFromPsiModifiers(semanticServices.getTrace(), psiClass));
        classDescriptorCache.put(psiClass.getQualifiedName(), classDescriptor);
        classDescriptor.setUnsubstitutedMemberScope(new JavaClassMembersScope(classDescriptor, psiClass, semanticServices, false));

        // UGLY HACK
        initializeTypeParameters(psiClass);

        supertypes.addAll(getSupertypes(psiClass));
        if (psiClass.isInterface()) {
            classDescriptor.setSuperclassType(JetStandardClasses.getAnyType()); // TODO : Make it java.lang.Object
        }
        else {
            PsiClassType[] extendsListTypes = psiClass.getExtendsListTypes();
            assert extendsListTypes.length == 0 || extendsListTypes.length == 1;
            JetType superclassType = extendsListTypes.length == 0
                                            ? JetStandardClasses.getAnyType()
                                            : semanticServices.getTypeTransformer().transformToType(extendsListTypes[0]);
            classDescriptor.setSuperclassType(superclassType);
        }

        PsiMethod[] psiConstructors = psiClass.getConstructors();

        if (psiConstructors.length == 0) {
            if (!psiClass.hasModifierProperty(PsiModifier.ABSTRACT) && !psiClass.isInterface()) {
                ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                        classDescriptor,
                        Collections.<AnnotationDescriptor>emptyList(),
                        false);
                constructorDescriptor.initialize(typeParameters, Collections.<ValueParameterDescriptor>emptyList(), Modality.FINAL, classDescriptor.getVisibility());
                constructorDescriptor.setReturnType(classDescriptor.getDefaultType());
                classDescriptor.addConstructor(constructorDescriptor);
                semanticServices.getTrace().record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
            }
        }
        else {
            for (PsiMethod constructor : psiConstructors) {
                ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                        classDescriptor,
                        Collections.<AnnotationDescriptor>emptyList(), // TODO
                        false);
                constructorDescriptor.initialize(typeParameters, resolveParameterDescriptors(constructorDescriptor, constructor.getParameterList().getParameters()), Modality.FINAL,
                                                 resolveVisibilityFromPsiModifiers(semanticServices.getTrace(), constructor));
                constructorDescriptor.setReturnType(classDescriptor.getDefaultType());
                classDescriptor.addConstructor(constructorDescriptor);
                semanticServices.getTrace().record(BindingContext.CONSTRUCTOR, constructor, constructorDescriptor);
            }
        }

        semanticServices.getTrace().record(BindingContext.CLASS, psiClass, classDescriptor);

        return classDescriptor;
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
        typeParameterDescriptorCache.put(psiTypeParameter, typeParameterDescriptor);
        return typeParameterDescriptor;
    }

    private void initializeTypeParameter(PsiTypeParameter typeParameter, TypeParameterDescriptor typeParameterDescriptor) {
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

    private void initializeTypeParameters(PsiTypeParameterListOwner typeParameterListOwner) {
        for (PsiTypeParameter psiTypeParameter : typeParameterListOwner.getTypeParameters()) {
            initializeTypeParameter(psiTypeParameter, resolveTypeParameter(psiTypeParameter));
        }
    }

    @NotNull
    private TypeParameterDescriptor resolveTypeParameter(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameter psiTypeParameter) {
        TypeParameterDescriptor typeParameterDescriptor = typeParameterDescriptorCache.get(psiTypeParameter);
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
        PsiPackage psiPackage = javaFacade.findPackage(qualifiedName);
        if (psiPackage == null) {
            PsiClass psiClass = javaFacade.findClass(qualifiedName, javaSearchScope);
            if (psiClass == null) return null;
            return resolveNamespace(psiClass);
        }
        return resolveNamespace(psiPackage);
    }

    private NamespaceDescriptor resolveNamespace(@NotNull PsiPackage psiPackage) {
        NamespaceDescriptor namespaceDescriptor = namespaceDescriptorCache.get(psiPackage);
        if (namespaceDescriptor == null) {
            namespaceDescriptor = createJavaNamespaceDescriptor(psiPackage);
            namespaceDescriptorCache.put(psiPackage, namespaceDescriptor);
        }
        return namespaceDescriptor;
    }

    private NamespaceDescriptor resolveNamespace(@NotNull PsiClass psiClass) {
        NamespaceDescriptor namespaceDescriptor = namespaceDescriptorCache.get(psiClass);
        if (namespaceDescriptor == null) {
            namespaceDescriptor = createJavaNamespaceDescriptor(psiClass);
            namespaceDescriptorCache.put(psiClass, namespaceDescriptor);
        }
        return namespaceDescriptor;
    }

    private NamespaceDescriptor createJavaNamespaceDescriptor(@NotNull PsiPackage psiPackage) {
        String name = psiPackage.getName();
        JavaNamespaceDescriptor namespaceDescriptor = new JavaNamespaceDescriptor(
                resolveParentDescriptor(psiPackage),
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                name == null ? JAVA_ROOT : name
        );

        namespaceDescriptor.setMemberScope(new JavaPackageScope(psiPackage.getQualifiedName(), namespaceDescriptor, semanticServices));
        semanticServices.getTrace().record(BindingContext.NAMESPACE, psiPackage, namespaceDescriptor);
        return namespaceDescriptor;
    }

    private DeclarationDescriptor resolveParentDescriptor(@NotNull PsiPackage psiPackage) {
        PsiPackage parentPackage = psiPackage.getParentPackage();
        if (parentPackage == null) {
            return null;
        }
        return resolveNamespace(parentPackage);
    }

    private NamespaceDescriptor createJavaNamespaceDescriptor(@NotNull final PsiClass psiClass) {
        JavaNamespaceDescriptor namespaceDescriptor = new JavaNamespaceDescriptor(
                resolveParentDescriptor(psiClass),
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                psiClass.getName()
        );
        namespaceDescriptor.setMemberScope(new JavaClassMembersScope(namespaceDescriptor, psiClass, semanticServices, true));
        semanticServices.getTrace().record(BindingContext.NAMESPACE, psiClass, namespaceDescriptor);
        return namespaceDescriptor;
    }

    public List<ValueParameterDescriptor> resolveParameterDescriptors(DeclarationDescriptor containingDeclaration, PsiParameter[] parameters) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
            PsiParameter parameter = parameters[i];
            String name = parameter.getName();
            PsiType psiType = parameter.getType();

            JetType varargElementType;
            if (psiType instanceof PsiEllipsisType) {
                PsiEllipsisType psiEllipsisType = (PsiEllipsisType) psiType;
                varargElementType = semanticServices.getTypeTransformer().transformToType(psiEllipsisType.getComponentType());
            }
            else {
                varargElementType = null;
            }
            JetType outType = semanticServices.getTypeTransformer().transformToType(psiType);
            result.add(new ValueParameterDescriptorImpl(
                    containingDeclaration,
                    i,
                    Collections.<AnnotationDescriptor>emptyList(), // TODO
                    name == null ? "p" + i : name,
                    null, // TODO : review
                    outType,
                    false,
                    varargElementType
            ));
        }
        return result;
    }

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
                resolveVisibilityFromPsiModifiers(semanticServices.getTrace(), field),
                !isFinal,
                null,
                DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration),
                field.getName(),
                isFinal ? null : type,
                type);
        semanticServices.getTrace().record(BindingContext.VARIABLE, field, propertyDescriptor);
        fieldDescriptorCache.put(field, propertyDescriptor);
        return propertyDescriptor;
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
        PsiParameter[] parameters = method.getParameterList().getParameters();
        FunctionDescriptorImpl functionDescriptorImpl = new FunctionDescriptorImpl(
                owner,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                method.getName()
        );
        methodDescriptorCache.put(method, functionDescriptorImpl);
        List<TypeParameterDescriptor> typeParameters = makeUninitializedTypeParameters(functionDescriptorImpl, method.getTypeParameters());
        initializeTypeParameters(method);
        functionDescriptorImpl.initialize(
                null,
                DescriptorUtils.getExpectedThisObjectIfNeeded(owner),
                typeParameters,
                semanticServices.getDescriptorResolver().resolveParameterDescriptors(functionDescriptorImpl, parameters),
                semanticServices.getTypeTransformer().transformToType(returnType),
                Modality.convertFromFlags(method.hasModifierProperty(PsiModifier.ABSTRACT), !method.hasModifierProperty(PsiModifier.FINAL)),
                resolveVisibilityFromPsiModifiers(semanticServices.getTrace(), method)
        );
        semanticServices.getTrace().record(BindingContext.FUNCTION, method, functionDescriptorImpl);
        FunctionDescriptor substitutedFunctionDescriptor = functionDescriptorImpl;
        if (method.getContainingClass() != psiClass) {
            substitutedFunctionDescriptor = functionDescriptorImpl.substitute(typeSubstitutorForGenericSuperclasses);
        }
        return substitutedFunctionDescriptor;
    }

    private static Visibility resolveVisibilityFromPsiModifiers(BindingTrace trace, PsiModifierListOwner modifierListOwner) {
        //TODO report error
        return modifierListOwner.hasModifierProperty(PsiModifier.PUBLIC) ? Visibility.PUBLIC :
                                        (modifierListOwner.hasModifierProperty(PsiModifier.PRIVATE) ? Visibility.PRIVATE :
                                        (modifierListOwner.hasModifierProperty(PsiModifier.PROTECTED) ? Visibility.PROTECTED : Visibility.INTERNAL));
    }

    public TypeParameterDescriptor resolveTypeParameter(PsiTypeParameter typeParameter) {
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
}
