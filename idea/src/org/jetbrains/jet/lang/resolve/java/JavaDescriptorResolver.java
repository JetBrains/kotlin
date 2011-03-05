package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class JavaDescriptorResolver {

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
            classDescriptorCache.put(qualifiedName, classDescriptor);
        }
        return classDescriptor;
    }

    private ClassDescriptor createJavaClassDescriptor(@NotNull PsiClass psiClass) {
        String name = psiClass.getName();
        PsiModifierList modifierList = psiClass.getModifierList();
        ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                Collections.<Attribute>emptyList(), // TODO
                modifierList == null ? false : modifierList.hasModifierProperty(PsiModifier.FINAL),
                name,
                Collections.<TypeParameterDescriptor>emptyList(),
                Collections.<Type>emptyList(),
                new JavaClassMembersScope(psiClass, semanticServices, false)
        );
        semanticServices.getTrace().recordDeclarationResolution(psiClass, classDescriptor);
        return classDescriptor;
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
                Collections.<Attribute>emptyList(), // TODO
                psiPackage.getName(),
                new JavaPackageScope(psiPackage.getQualifiedName(), semanticServices)
        );
        semanticServices.getTrace().recordDeclarationResolution(psiPackage, namespaceDescriptor);
        return namespaceDescriptor;
    }

    private NamespaceDescriptor createJavaNamespaceDescriptor(@NotNull PsiClass psiClass) {
        NamespaceDescriptor namespaceDescriptor = new NamespaceDescriptor(
                Collections.<Attribute>emptyList(), // TODO
                psiClass.getName(),
                new JavaClassMembersScope(psiClass, semanticServices, true)
        );
        semanticServices.getTrace().recordDeclarationResolution(psiClass, namespaceDescriptor);
        return namespaceDescriptor;
    }

    public List<ValueParameterDescriptor> resolveParameterDescriptors(PsiParameter[] parameters) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
            PsiParameter parameter = parameters[i];
            result.add(new ValueParameterDescriptorImpl(
                    i,
                    Collections.<Attribute>emptyList(), // TODO
                    parameter.getName(),
                    semanticServices.getTypeTransformer().transform(parameter.getType()),
                    false,
                    parameter.isVarArgs()
            ));
        }
        return result;
    }
}
