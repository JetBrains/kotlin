package org.jetbrains.jet.plugin.caches;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.containers.HashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.stubindex.JetExtensionFunctionNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetShortFunctionNameIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Nikolay Krasko
 */
public class JetShortNamesCache extends PsiShortNamesCache {

    private final Project project;

    public JetShortNamesCache(Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public PsiClass[] getClassesByName(@NotNull @NonNls String name, @NotNull GlobalSearchScope scope) {
        return new PsiClass[0];
    }

    @NotNull
    @Override
    public String[] getAllClassNames() {
        return new String[0];
    }

    @Override
    public void getAllClassNames(@NotNull HashSet<String> dest) {
    }

    public List<String> getAllJetClassNames() {
        final BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCache(project, GlobalSearchScope.allScope(project));
        final Collection<String> fqNames = context.getKeys(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR);

        final ArrayList<String> classNames = new ArrayList<String>();

        for (String fqName : fqNames) {
            int lastDotIndex = fqName.lastIndexOf('.');
            if (lastDotIndex != -1) {
                classNames.add(fqName.substring(lastDotIndex + 1, fqName.length()));
            } 
            else {
                classNames.add(fqName);
            }
        }

        return classNames;
    }

    public Collection<ClassDescriptor> getClassDescriptors() {
        final BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCache(project, GlobalSearchScope.allScope(project));

        final Collection<String> fqNames = context.getKeys(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR);
        return Collections2.transform(fqNames, new Function<String, ClassDescriptor>() {
            @Override
            public ClassDescriptor apply(String fqName) {
                return context.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, fqName);
            }
        });
    }
    
    public List<JetClass> getJetClassesByName(@NotNull String name, @NotNull GlobalSearchScope scope) {
        final BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCache(project, scope);
        final ArrayList<JetClass> classesResult = new ArrayList<JetClass>();

        for (String fqn : getFQNamesByName(name, scope)) {
            final ClassDescriptor descriptor = context.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, fqn);
            final PsiElement declaration = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);

            if (declaration instanceof JetClass) {
                classesResult.add((JetClass) declaration);
            }
        }

        return classesResult;
    }

    @NotNull
    public Collection<String> getFQNamesByName(@NotNull final String name, @NotNull GlobalSearchScope scope) {
        final BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCache(project, scope);
        return Collections2.filter(context.getKeys(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR), new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String fqName) {
                return fqName != null && (fqName.endsWith('.' + name) || fqName.equals(name));
            }
        });
    }
    
    public Collection<JetNamedFunction> getAllExtensionFunctionsByName(@NotNull String name, @NotNull GlobalSearchScope scope) {
        return JetExtensionFunctionNameIndex.getInstance().get(name, project, scope);
    }

    @NotNull
    public Collection<String> getAllTopLevelFunctionNames() {
        return JetShortFunctionNameIndex.getInstance().getAllKeys(project);
    }

    public Collection<JetNamedFunction> getTopLevelFunctionsByName(final @NotNull String name, @NotNull GlobalSearchScope scope) {
        return JetShortFunctionNameIndex.getInstance().get(name, project, scope);
    }
    
//    private Collection<FunctionDescriptor> getTopLeveFunctions(@NotNull GlobalSearchScope scope) {
//        final BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCache(project, scope);
//        final Collection<String> keys = context.getKeys(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR);
//
//        final List<FunctionDescriptor> result = new ArrayList<FunctionDescriptor>();
//
//        for (String namespaceKey : keys) {
//            final NamespaceDescriptor namespace = context.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, namespaceKey);
//            if (namespace != null) {
//                final JetScope memberScope = namespace.getNamespaceType().getMemberScope();
//                final Collection<DeclarationDescriptor> allDescriptors = memberScope.getAllDescriptors();
//
//                for (DeclarationDescriptor descriptor : allDescriptors) {
//                    if (descriptor instanceof FunctionDescriptor) {
//                        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
//                        if (functionDescriptor.getReceiverParameter() == ReceiverDescriptor.NO_RECEIVER) {
//                            result.add(functionDescriptor);
//                        }
//                    }
//                }
//            }
//        }
//
//        return result;
//    }

    @NotNull
    @Override
    public PsiMethod[] getMethodsByName(@NonNls @NotNull String name, @NotNull GlobalSearchScope scope) {
        return new PsiMethod[0];
    }

    @NotNull
    @Override
    public PsiMethod[] getMethodsByNameIfNotMoreThan(@NonNls @NotNull String name, @NotNull GlobalSearchScope scope, int maxCount) {
        return new PsiMethod[0];
    }

    @NotNull
    @Override
    public String[] getAllMethodNames() {
        return new String[0];
    }

    @Override
    public void getAllMethodNames(@NotNull HashSet<String> set) {
    }

    @NotNull
    @Override
    public PsiField[] getFieldsByName(@NotNull @NonNls String name, @NotNull GlobalSearchScope scope) {
        return new PsiField[0];
    }

    @NotNull
    @Override
    public String[] getAllFieldNames() {
        return new String[0];
    }

    @Override
    public void getAllFieldNames(@NotNull HashSet<String> set) {
    }
}
