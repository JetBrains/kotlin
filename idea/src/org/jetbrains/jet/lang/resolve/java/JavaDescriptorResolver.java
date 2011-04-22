package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.MutableClassDescriptor;
import org.jetbrains.jet.lang.resolve.WritableFunctionGroup;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class JavaDescriptorResolver {

    /*package*/ static final DeclarationDescriptor JAVA_ROOT = new DeclarationDescriptorImpl(null, Collections.<Attribute>emptyList(), "<java_root>") {
        @NotNull
        @Override
        public DeclarationDescriptor substitute(TypeSubstitutor substitutor) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
            throw new UnsupportedOperationException(); // TODO
        }
    };

    protected final Map<String, ClassDescriptor> classDescriptorCache = new HashMap<String, ClassDescriptor>();
    protected final Map<PsiTypeParameter, TypeParameterDescriptor> typeParameterDescriptorCache = Maps.newHashMap();
    protected final Map<String, NamespaceDescriptor> namespaceDescriptorCache = new HashMap<String, NamespaceDescriptor>();
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
        PsiModifierList modifierList = psiClass.getModifierList();
        MutableClassDescriptor classDescriptor = new MutableClassDescriptor(
                JAVA_ROOT
        );
        classDescriptor.setName(name);

        WritableFunctionGroup constructors = new WritableFunctionGroup("<init>");
        List<JetType> supertypes = new ArrayList<JetType>();
        classDescriptor.setTypeConstructor(new TypeConstructorImpl(
                classDescriptor,
                Collections.<Attribute>emptyList(), // TODO
                // TODO
                modifierList == null ? false : modifierList.hasModifierProperty(PsiModifier.FINAL),
                name,
                resolveTypeParameters(psiClass.getTypeParameters()),
                supertypes

        ));
        classDescriptorCache.put(psiClass.getQualifiedName(), classDescriptor);
        // UGLY HACK
        supertypes.addAll(getSupertypes(psiClass));
        classDescriptor.setUnsubstitutedMemberScope(new JavaClassMembersScope(classDescriptor, psiClass, semanticServices, false));

        // NOTE: this writes into constructors after it is remembered by the classDescriptor
        PsiMethod[] psiConstructors = psiClass.getConstructors();
        for (PsiMethod constructor : psiConstructors) {
            ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                    classDescriptor,
                    Collections.<Attribute>emptyList(), // TODO
                    false);
            constructorDescriptor.initialize(resolveParameterDescriptors(constructorDescriptor, constructor.getParameterList().getParameters()));
            classDescriptor.addConstructor(constructorDescriptor);
            semanticServices.getTrace().recordDeclarationResolution(constructor, constructorDescriptor);
        }

        semanticServices.getTrace().recordDeclarationResolution(psiClass, classDescriptor);
        return classDescriptor;
    }

    private List<TypeParameterDescriptor> resolveTypeParameters(@NotNull PsiTypeParameter[] typeParameters) {
        List<TypeParameterDescriptor> result = Lists.newArrayList();
        for (PsiTypeParameter typeParameter : typeParameters) {
            TypeParameterDescriptor typeParameterDescriptor = resolveTypeParameter(typeParameter);
            result.add(typeParameterDescriptor);
        }
        return result;
    }

    private TypeParameterDescriptor createJavaTypeParameterDescriptor(@NotNull DeclarationDescriptor owner, @NotNull PsiTypeParameter typeParameter) {
        PsiClassType[] referencedTypes = typeParameter.getExtendsList().getReferencedTypes();
        Set<JetType> upperBounds;
        JetType boundsAsType;
        if (referencedTypes.length == 0){
            boundsAsType = JetStandardClasses.getNullableAnyType();
            upperBounds = Collections.singleton(boundsAsType);
        }
        else if (referencedTypes.length == 1) {
            boundsAsType = semanticServices.getTypeTransformer().transformToType(referencedTypes[0]);
            upperBounds = Collections.singleton(boundsAsType);
        }
        else {
            upperBounds = Sets.newLinkedHashSet();
            for (PsiClassType referencedType : referencedTypes) {
                upperBounds.add(semanticServices.getTypeTransformer().transformToType(referencedType));
            }
            boundsAsType = TypeUtils.safeIntersect(semanticServices.getJetSemanticServices().getTypeChecker(), upperBounds);
        }
        return new TypeParameterDescriptor(
                owner,
                Collections.<Attribute>emptyList(), // TODO
                Variance.INVARIANT,
                typeParameter.getName(),
                upperBounds,
                boundsAsType
        );
    }

    @NotNull
    public TypeParameterDescriptor resolveTypeParameter(@NotNull PsiTypeParameter psiTypeParameter) {
        TypeParameterDescriptor typeParameterDescriptor = typeParameterDescriptorCache.get(psiTypeParameter);
        if (typeParameterDescriptor == null) {
            typeParameterDescriptor = createJavaTypeParameterDescriptor(JAVA_ROOT, psiTypeParameter);
            typeParameterDescriptorCache.put(psiTypeParameter, typeParameterDescriptor);
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
        NamespaceDescriptor namespaceDescriptor = namespaceDescriptorCache.get(qualifiedName);
        if (namespaceDescriptor == null) {
            // TODO : packages

            PsiClass psiClass = javaFacade.findClass(qualifiedName, javaSearchScope);
            if (psiClass == null) {
                PsiPackage psiPackage = javaFacade.findPackage(qualifiedName);
                if (psiPackage == null) {
                    return null;
                }
                namespaceDescriptor = createJavaNamespaceDescriptor(psiPackage);
            } else {
                namespaceDescriptor = createJavaNamespaceDescriptor(psiClass);
            }
            namespaceDescriptorCache.put(qualifiedName, namespaceDescriptor);
        }
        return namespaceDescriptor;
    }

    private NamespaceDescriptor createJavaNamespaceDescriptor(PsiPackage psiPackage) {
        NamespaceDescriptor namespaceDescriptor = new NamespaceDescriptor(
                JAVA_ROOT,
                Collections.<Attribute>emptyList(), // TODO
                psiPackage.getName()
        );
        namespaceDescriptor.initialize(new JavaPackageScope(psiPackage.getQualifiedName(), namespaceDescriptor, semanticServices));
        semanticServices.getTrace().recordDeclarationResolution(psiPackage, namespaceDescriptor);
        return namespaceDescriptor;
    }

    private NamespaceDescriptor createJavaNamespaceDescriptor(@NotNull final PsiClass psiClass) {
        NamespaceDescriptor namespaceDescriptor = new NamespaceDescriptor(
                JAVA_ROOT,
                Collections.<Attribute>emptyList(), // TODO
                psiClass.getName()
        );
        namespaceDescriptor.initialize(new JavaClassMembersScope(namespaceDescriptor, psiClass, semanticServices, true));
        semanticServices.getTrace().recordDeclarationResolution(psiClass, namespaceDescriptor);
        return namespaceDescriptor;
    }

    public List<ValueParameterDescriptor> resolveParameterDescriptors(DeclarationDescriptor containingDeclaration, PsiParameter[] parameters) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
            PsiParameter parameter = parameters[i];
            String name = parameter.getName();
            result.add(new ValueParameterDescriptorImpl(
                    containingDeclaration,
                    i,
                    Collections.<Attribute>emptyList(), // TODO
                    name == null ? "p" + i : name,
                    null, // TODO : review
                    semanticServices.getTypeTransformer().transformToType(parameter.getType()),
                    false,
                    parameter.isVarArgs()
            ));
        }
        return result;
    }

    @NotNull
    public FunctionGroup resolveFunctionGroup(@NotNull PsiClass psiClass, @NotNull String methodName, boolean staticMembers) {
        WritableFunctionGroup writableFunctionGroup = new WritableFunctionGroup(methodName);
        PsiMethod[] allMethods = psiClass.getMethods(); // TODO : look into superclasses
        for (PsiMethod method : allMethods) {
            if (method.hasModifierProperty(PsiModifier.STATIC) != staticMembers) {
                continue;
            }
            if (!methodName.equals(method.getName())) {
                 continue;
            }
            final PsiParameter[] parameters = method.getParameterList().getParameters();

            FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(
                    JavaDescriptorResolver.JAVA_ROOT,
                    Collections.<Attribute>emptyList(), // TODO
                    methodName
            );
            functionDescriptor.initialize(
                    resolveTypeParameters(method.getTypeParameters()),
                    semanticServices.getDescriptorResolver().resolveParameterDescriptors(functionDescriptor, parameters),
                    semanticServices.getTypeTransformer().transformToType(method.getReturnType())
            );
            semanticServices.getTrace().recordDeclarationResolution(method, functionDescriptor);
            writableFunctionGroup.addFunction(functionDescriptor);
        }
        return writableFunctionGroup;
    }
}
