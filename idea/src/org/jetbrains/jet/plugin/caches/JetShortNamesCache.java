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

package org.jetbrains.jet.plugin.caches;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.containers.HashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.JavaElementFinder;
import org.jetbrains.jet.lang.descriptors.NamedFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.stubindex.JetExtensionFunctionNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetShortClassNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetShortFunctionNameIndex;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Will provide both java elements from jet context and some special declarations special to jet.
 * All those declaration are planned to be used in completion.
 *
 * @author Nikolay Krasko
 */
public class JetShortNamesCache extends PsiShortNamesCache {

    private final Project project;
    private final JavaElementFinder javaElementFinder;

    public JetShortNamesCache(Project project) {
        this.project = project;
        this.javaElementFinder = new JavaElementFinder(project);
    }

    /**
     * Return jet class names form jet project sources which should be visible from java.
     */
    @NotNull
    @Override
    public String[] getAllClassNames() {
        final Collection<String> classNames = JetShortClassNameIndex.getInstance().getAllKeys(project);
        return classNames.toArray(new String[classNames.size()]);
    }

    /**
     * Return class names form jet sources in given scope which should be visible as java classes.
     */
    @NotNull
    @Override
    public PsiClass[] getClassesByName(@NotNull @NonNls final String name, @NotNull GlobalSearchScope scope) {

        // Quick check for classes from getAllClassNames()
        final Collection<String> classNames = JetShortClassNameIndex.getInstance().getAllKeys(project);
        if (!classNames.contains(name)) {
            return new PsiClass[0];
        }

        List<PsiClass> result = new ArrayList<PsiClass>();

        for (String fqName : JetFullClassNameIndex.getInstance().getAllKeys(project)) {
            if (QualifiedNamesUtil.fqnToShortName(fqName).equals(name)) {
                final PsiClass psiClass = javaElementFinder.findClass(fqName, scope);
                if (psiClass != null) {
                    result.add(psiClass);
                }
            }
        }

        return result.toArray(new PsiClass[result.size()]);
    }

    @Override
    public void getAllClassNames(@NotNull HashSet<String> dest) {
        // TODO: Implement it. Is it called somewhere?
    }

    public Collection<String> getALlJetClassFQNames() {
        final BindingContext context = getResolutionContext(GlobalSearchScope.allScope(project));
        return context.getKeys(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR);
    }

    @NotNull
    public Collection<String> getFQNamesByName(@NotNull final String name, @NotNull GlobalSearchScope scope) {
        final BindingContext context = getResolutionContext(scope);
        return Collections2.filter(context.getKeys(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR), new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String fqName) {
                return fqName != null && QualifiedNamesUtil.isShortNameForFQN(name, fqName);
            }
        });
    }

    /**
     * Get jet non-extension top-level function names. Method is allowed to give invalid names - all result should be
     * checked with getTopLevelFunctionDescriptorsByName().
     *
     * @return
     */
    @NotNull
    public Collection<String> getAllTopLevelFunctionNames() {
        final HashSet<String> functionNames = new HashSet<String>();
        functionNames.addAll(JetShortFunctionNameIndex.getInstance().getAllKeys(project));
        functionNames.addAll(JetFromJavaDescriptorHelper.getPossiblePackageDeclarationsNames(project, GlobalSearchScope.allScope(project)));
        return functionNames;
    }

    @NotNull
    public Collection<NamedFunctionDescriptor> getTopLevelFunctionDescriptorsByName(final @NotNull String name,
                                                                                    final @NotNull GlobalSearchScope scope) {

        // TODO: Add jet function in jar-dependencies (those functions are missing in BindingContext and stubs)

        final Collection<JetNamedFunction> jetNamedFunctions = JetShortFunctionNameIndex.getInstance().get(name, project, scope);
        
        final BindingContext context = getResolutionContext(scope);

        final HashSet<NamedFunctionDescriptor> result = new HashSet<NamedFunctionDescriptor>();

        for (JetNamedFunction jetNamedFunction : jetNamedFunctions) {
            final NamedFunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, jetNamedFunction);
            if (functionDescriptor != null) {
                result.add(functionDescriptor);
            }
        }

        return result;
    }

    @NotNull
    public BindingContext getResolutionContext(final @NotNull GlobalSearchScope scope) {
        return WholeProjectAnalyzerFacade.analyzeProjectWithCache(project, scope);
    }

    public Collection<JetNamedFunction> getTopLevelFunctionsByName(final @NotNull String name, @NotNull GlobalSearchScope scope) {
        return JetShortFunctionNameIndex.getInstance().get(name, project, scope);
    }

    /**
     * Get jet extensions top-level function names. Method is allowed to give invalid names - all result should be
     * checked with getAllJetExtensionFunctionsByName().
     *
     * @return
     */
    @NotNull
    public Collection<String> getAllJetExtensionFunctionsNames(@NotNull GlobalSearchScope scope) {
        final Set<String> extensionFunctionNames = new HashSet<String>();

        extensionFunctionNames.addAll(JetExtensionFunctionNameIndex.getInstance().getAllKeys(project));
        extensionFunctionNames.addAll(JetFromJavaDescriptorHelper.getTopExtensionFunctionNames(null, project, scope));

        return extensionFunctionNames;
    }

    public Collection<NamedFunctionDescriptor> getAllJetExtensionFunctionsByName(@NotNull String name, @NotNull GlobalSearchScope scope) {
        // TODO: Add jet extension functions in jar-dependencies (those functions are missing in BindingContext and stubs)

        final Collection<JetNamedFunction> jetNamedFunctions = JetShortFunctionNameIndex.getInstance().get(name, project, scope);

        final BindingContext context = getResolutionContext(scope);

        final HashSet<NamedFunctionDescriptor> result = new HashSet<NamedFunctionDescriptor>();

        for (JetNamedFunction jetNamedFunction : jetNamedFunctions) {
            final NamedFunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, jetNamedFunction);
            if (functionDescriptor != null) {
                if (functionDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor) {
                    if (functionDescriptor.getExpectedThisObject() != ReceiverDescriptor.NO_RECEIVER) {
                        result.add(functionDescriptor);
                    }
                }
            }
        }

        return result;
    }

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
