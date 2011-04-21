package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        String name = psiClass.getName();
        PsiModifierList modifierList = psiClass.getModifierList();
        ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                JAVA_ROOT,
                Collections.<Attribute>emptyList(), // TODO
                name
        );

        WritableFunctionGroup constructors = new WritableFunctionGroup("<init>");
        classDescriptor.initialize(
                // TODO
                modifierList == null ? false : modifierList.hasModifierProperty(PsiModifier.FINAL),
                resolveTypeParameters(psiClass, classDescriptor),
                getSupertypes(psiClass),
                new JavaClassMembersScope(classDescriptor, psiClass, semanticServices, false),
                constructors
        );

        classDescriptorCache.put(psiClass.getQualifiedName(), classDescriptor);

        // NOTE: this writes into constructors after it is remembered by the classDescriptor
        PsiMethod[] psiConstructors = psiClass.getConstructors();
        for (PsiMethod constructor : psiConstructors) {
            ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                    classDescriptor,
                    Collections.<Attribute>emptyList(), // TODO
                    false);
            constructorDescriptor.initialize(resolveParameterDescriptors(constructorDescriptor, constructor.getParameterList().getParameters()));
            constructors.addFunction(constructorDescriptor);
            semanticServices.getTrace().recordDeclarationResolution(constructor, constructorDescriptor);
        }

        semanticServices.getTrace().recordDeclarationResolution(psiClass, classDescriptor);
        return classDescriptor;
    }

    private List<TypeParameterDescriptor> resolveTypeParameters(@NotNull PsiClass psiClass, @NotNull ClassDescriptor classDescriptor) {
        if (1 < 2) return Collections.emptyList(); // TODO
        List<TypeParameterDescriptor> result = Lists.newArrayList();
        PsiTypeParameter[] typeParameters = psiClass.getTypeParameters();
        for (PsiTypeParameter typeParameter : typeParameters) {
            PsiClassType[] referencedTypes = typeParameter.getExtendsList().getReferencedTypes();
            Set<JetType> upperBounds;
            JetType boundsAsType;
            if (referencedTypes.length == 0){
                boundsAsType = JetStandardClasses.getNullableAnyType();
                upperBounds = Collections.singleton(boundsAsType);
            }
            else if (referencedTypes.length == 1) {
                boundsAsType = semanticServices.getTypeTransformer().transform(referencedTypes[0]);
                upperBounds = Collections.singleton(boundsAsType);
            }
            else {
                upperBounds = Sets.newLinkedHashSet();
                for (PsiClassType referencedType : referencedTypes) {
                    upperBounds.add(semanticServices.getTypeTransformer().transform(referencedType));
                }
                boundsAsType = TypeUtils.safeIntersect(semanticServices.getTypeChecker(), upperBounds);
            }
            result.add(new TypeParameterDescriptor(
                    classDescriptor,
                    Collections.<Attribute>emptyList(), // TODO
                    Variance.INVARIANT,
                    typeParameter.getName(),
                    upperBounds,
                    boundsAsType
            ));
        }
        return result;
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
            JetType transform = semanticServices.getTypeTransformer().transform(type);

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
                    semanticServices.getTypeTransformer().transform(parameter.getType()),
                    false,
                    parameter.isVarArgs()
            ));
        }
        return result;
    }
}
