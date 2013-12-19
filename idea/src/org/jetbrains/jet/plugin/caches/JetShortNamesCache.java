/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.JavaElementFinder;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.plugin.caches.resolve.IDELightClassGenerationSupport;
import org.jetbrains.jet.plugin.project.CancelableResolveSession;
import org.jetbrains.jet.plugin.stubindex.*;

import java.util.*;

import static org.jetbrains.jet.plugin.caches.JetFromJavaDescriptorHelper.getTopLevelFunctionFqNames;

/**
 * Will provide both java elements from jet context and some special declarations special to jet.
 * All those declaration are planned to be used in completion.
 */
public class JetShortNamesCache extends PsiShortNamesCache {

    public static JetShortNamesCache getKotlinInstance(@NotNull Project project) {
        PsiShortNamesCache[] extensions = Extensions.getArea(project).getExtensionPoint(PsiShortNamesCache.EP_NAME).getExtensions();
        for (PsiShortNamesCache extension : extensions) {
            if (extension instanceof JetShortNamesCache) {
                return (JetShortNamesCache) extension;
            }
        }
        throw new IllegalStateException(JetShortNamesCache.class.getSimpleName() + " is not found for project " + project);
    }

    private static final PsiMethod[] NO_METHODS = new PsiMethod[0];
    private static final PsiField[] NO_FIELDS = new PsiField[0];
    private final Project project;

    public JetShortNamesCache(Project project) {
        this.project = project;
    }

    /**
     * Return jet class names form jet project sources which should be visible from java.
     */
    @NotNull
    @Override
    public String[] getAllClassNames() {
        Collection<String> classNames = JetShortClassNameIndex.getInstance().getAllKeys(project);

        // .namespace classes can not be indexed, since they have no explicit declarations
        IDELightClassGenerationSupport lightClassGenerationSupport = IDELightClassGenerationSupport.getInstanceForIDE(project);
        Set<String> packageClassShortNames =
                lightClassGenerationSupport.getAllPossiblePackageClasses(GlobalSearchScope.allScope(project)).keySet();
        classNames.addAll(packageClassShortNames);

        return ArrayUtil.toStringArray(classNames);
    }

    /**
     * Return class names form jet sources in given scope which should be visible as Java classes.
     */
    @NotNull
    @Override
    public PsiClass[] getClassesByName(@NotNull @NonNls String name, @NotNull GlobalSearchScope scope) {
        List<PsiClass> result = new ArrayList<PsiClass>();

        IDELightClassGenerationSupport lightClassGenerationSupport = IDELightClassGenerationSupport.getInstanceForIDE(project);
        MultiMap<String, FqName> packageClasses = lightClassGenerationSupport.getAllPossiblePackageClasses(scope);

        // .namespace classes can not be indexed, since they have no explicit declarations
        Collection<FqName> fqNames = packageClasses.get(name);
        if (!fqNames.isEmpty()) {
            for (FqName fqName : fqNames) {
                PsiClass psiClass = JavaElementFinder.getInstance(project).findClass(fqName.asString(), scope);
                if (psiClass != null) {
                    result.add(psiClass);
                }
            }
        }

        // Quick check for classes from getAllClassNames()
        Collection<JetClassOrObject> classOrObjects = JetShortClassNameIndex.getInstance().get(name, project, scope);
        if (classOrObjects.isEmpty()) {
            return result.toArray(new PsiClass[result.size()]);
        }

        for (JetClassOrObject classOrObject : classOrObjects) {
            FqName fqName = JetPsiUtil.getFQName(classOrObject);
            if (fqName != null) {
                assert fqName.shortName().asString().equals(name) : "A declaration obtained from index has non-matching name:\n" +
                                                                    "in index: " + name + "\n" +
                                                                    "declared: " + fqName.shortName() + "(" + fqName + ")";
                PsiClass psiClass = JavaElementFinder.getInstance(project).findClass(fqName.asString(), scope);
                if (psiClass != null) {
                    result.add(psiClass);
                }
            }
        }

        return result.toArray(new PsiClass[result.size()]);
    }

    @Override
    public void getAllClassNames(@NotNull HashSet<String> destination) {
        destination.addAll(Arrays.asList(getAllClassNames()));
    }

    /**
     * Get jet non-extension top-level function names. Method is allowed to give invalid names - all result should be
     * checked with getTopLevelFunctionDescriptorsByName().
     *
     * @return
     */
    @NotNull
    public Collection<String> getAllTopLevelFunctionNames() {
        Set<String> functionNames = new HashSet<String>();
        functionNames.addAll(JetShortFunctionNameIndex.getInstance().getAllKeys(project));
        functionNames.addAll(JetFromJavaDescriptorHelper.getPossiblePackageDeclarationsNames(project, GlobalSearchScope.allScope(project)));
        return functionNames;
    }

    @NotNull
    public Collection<String> getAllTopLevelObjectNames() {
        Set<String> topObjectNames = new HashSet<String>();
        topObjectNames.addAll(JetTopLevelShortObjectNameIndex.getInstance().getAllKeys(project));

        Collection<PsiClass> classObjects =
                JetFromJavaDescriptorHelper.getCompiledClassesForTopLevelObjects(project, GlobalSearchScope.allScope(project));
        topObjectNames.addAll(Collections2.transform(classObjects, new Function<PsiClass, String>() {
            @Override
            public String apply(@Nullable PsiClass aClass) {
                assert aClass != null;
                return aClass.getName();
            }
        }));

        return topObjectNames;
    }

    @NotNull
    public Collection<ClassDescriptor> getTopLevelObjectsByName(
            @NotNull String name,
            @NotNull JetSimpleNameExpression expression,
            @NotNull CancelableResolveSession resolveSession,
            @NotNull GlobalSearchScope scope
    ) {
        BindingContext context = resolveSession.resolveToElement(expression);
        JetScope jetScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);

        if (jetScope == null) {
            return Collections.emptyList();
        }

        Set<ClassDescriptor> result = Sets.newHashSet();

        Collection<JetObjectDeclaration> topObjects = JetTopLevelShortObjectNameIndex.getInstance().get(name, project, scope);
        for (JetObjectDeclaration objectDeclaration : topObjects) {
            FqName fqName = JetPsiUtil.getFQName(objectDeclaration);
            assert fqName != null : "Local object declaration in JetTopLevelShortObjectNameIndex:" + objectDeclaration.getText();
            result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession, fqName, true));
        }

        for (PsiClass psiClass : JetFromJavaDescriptorHelper
                .getCompiledClassesForTopLevelObjects(project, GlobalSearchScope.allScope(project))) {
            String qualifiedName = psiClass.getQualifiedName();
            if (qualifiedName != null) {
                FqName fqName = new FqName(qualifiedName);
                result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession, fqName, true));
            }
        }

        return result;
    }

    @NotNull
    public Collection<FunctionDescriptor> getTopLevelFunctionDescriptorsByName(
            @NotNull final String name,
            @NotNull JetSimpleNameExpression expression,
            @NotNull CancelableResolveSession resolveSession,
            @NotNull GlobalSearchScope scope
    ) {
        // name parameter can differ from expression.getReferenceName() when expression contains completion suffix
        Name referenceName = expression.getIdentifier() == null ? JetPsiUtil.getConventionName(expression) : Name.identifier(name);
        if (referenceName == null || referenceName.toString().isEmpty()) {
            return Collections.emptyList();
        }

        BindingContext context = resolveSession.resolveToElement(expression);
        JetScope jetScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);

        if (jetScope == null) {
            return Collections.emptyList();
        }

        Set<FunctionDescriptor> result = Sets.newHashSet();

        Collection<FqName> topLevelFunctionFqNames =
                ContainerUtil.filter(getTopLevelFunctionFqNames(project, scope, false), new Condition<FqName>() {
                    @Override
                    public boolean value(FqName fqName) {
                        return fqName.lastSegmentIs(Name.identifier(name));
                    }
                });
        for (FqName fqName : topLevelFunctionFqNames) {
            JetImportDirective importDirective = JetPsiFactory.createImportDirective(project, new ImportPath(fqName, false));
            Collection<? extends DeclarationDescriptor> declarationDescriptors = new QualifiedExpressionResolver().analyseImportReference(
                    importDirective, jetScope, new BindingTraceContext(), resolveSession.getModuleDescriptor());
            for (DeclarationDescriptor declarationDescriptor : declarationDescriptors) {
                if (declarationDescriptor instanceof FunctionDescriptor) {
                    result.add((FunctionDescriptor) declarationDescriptor);
                }
            }
        }

        Set<FqName> affectedPackages = Sets.newHashSet();
        Collection<JetNamedFunction> jetNamedFunctions =
                JetShortFunctionNameIndex.getInstance().get(referenceName.asString(), project, scope);
        for (JetNamedFunction jetNamedFunction : jetNamedFunctions) {
            PsiFile containingFile = jetNamedFunction.getContainingFile();
            if (containingFile instanceof JetFile) {
                JetFile jetFile = (JetFile) containingFile;
                String packageName = jetFile.getPackageName();
                if (packageName != null) {
                    affectedPackages.add(new FqName(packageName));
                }
            }
        }

        for (FqName affectedPackage : affectedPackages) {
            PackageViewDescriptor packageDescriptor = resolveSession.getModuleDescriptor().getPackage(affectedPackage);
            assert packageDescriptor != null : "There's a function in stub index with invalid package: " + affectedPackage;
            JetScope memberScope = packageDescriptor.getMemberScope();
            result.addAll(memberScope.getFunctions(referenceName));
        }

        return result;
    }

    private Collection<PsiElement> getJetExtensionFunctionsByName(@NotNull String name, @NotNull GlobalSearchScope scope) {
        HashSet<PsiElement> functions = new HashSet<PsiElement>();
        functions.addAll(JetExtensionFunctionNameIndex.getInstance().get(name, project, scope));

        return functions;
    }

    // TODO: Make it work for properties
    @NotNull
    public Collection<DeclarationDescriptor> getJetCallableExtensions(
            @NotNull final Condition<String> acceptedNameCondition,
            @NotNull JetSimpleNameExpression expression,
            @NotNull CancelableResolveSession resolveSession,
            @NotNull GlobalSearchScope searchScope
    ) {
        BindingContext context = resolveSession.resolveToElement(expression);
        JetExpression receiverExpression = expression.getReceiverExpression();

        if (receiverExpression == null) {
            return Collections.emptyList();
        }
        JetType expressionType = context.get(BindingContext.EXPRESSION_TYPE, receiverExpression);
        JetScope scope = context.get(BindingContext.RESOLUTION_SCOPE, receiverExpression);

        if (expressionType == null || scope == null || expressionType.isError()) {
            return Collections.emptyList();
        }

        Set<FqName> functionFQNs = extensionFunctionsFromSourceFqNames(acceptedNameCondition, searchScope);
        functionFQNs.addAll(ContainerUtil.filter(getTopLevelFunctionFqNames(project, searchScope, true), new Condition<FqName>() {
            @Override
            public boolean value(FqName fqName) {
                return acceptedNameCondition.value(fqName.shortName().asString());
            }
        }));

        Collection<DeclarationDescriptor> resultDescriptors = new ArrayList<DeclarationDescriptor>();
        // Iterate through the function with attempt to resolve found functions
        for (FqName functionFQN : functionFQNs) {
            for (CallableDescriptor functionDescriptor : ExpressionTypingUtils.canFindSuitableCall(
                    functionFQN, project, receiverExpression, expressionType, scope, resolveSession.getModuleDescriptor())) {

                resultDescriptors.add(functionDescriptor);
            }
        }

        return resultDescriptors;
    }

    @NotNull
    private Set<FqName> extensionFunctionsFromSourceFqNames(
            @NotNull Condition<String> acceptedNameCondition,
            @NotNull GlobalSearchScope searchScope
    ) {
        Set<String> extensionFunctionNames = new HashSet<String>(JetExtensionFunctionNameIndex.getInstance().getAllKeys(project));

        Set<FqName> functionFQNs = new java.util.HashSet<FqName>();

        // Collect all possible extension function qualified names
        for (String name : extensionFunctionNames) {
            if (acceptedNameCondition.value(name)) {
                Collection<PsiElement> extensionFunctions = getJetExtensionFunctionsByName(name, searchScope);

                for (PsiElement extensionFunction : extensionFunctions) {
                    if (extensionFunction instanceof JetNamedFunction) {
                        functionFQNs.add(JetPsiUtil.getFQName((JetNamedFunction) extensionFunction));
                    }
                    else if (extensionFunction instanceof PsiMethod) {
                        FqName functionFQN =
                                JetFromJavaDescriptorHelper.getJetTopLevelDeclarationFQN((PsiMethod) extensionFunction);
                        if (functionFQN != null) {
                            functionFQNs.add(functionFQN);
                        }
                    }
                }
            }
        }
        return functionFQNs;
    }

    public Collection<ClassDescriptor> getJetClassesDescriptors(
            @NotNull Condition<String> acceptedShortNameCondition,
            @NotNull KotlinCodeAnalyzer analyzer,
            @NotNull GlobalSearchScope searchScope
    ) {
        Collection<ClassDescriptor> classDescriptors = new ArrayList<ClassDescriptor>();

        for (String fqName : JetFullClassNameIndex.getInstance().getAllKeys(project)) {
            FqName classFQName = new FqName(fqName);
            if (acceptedShortNameCondition.value(classFQName.shortName().asString())) {
                classDescriptors.addAll(getJetClassesDescriptorsByFQName(analyzer, classFQName, searchScope));
            }
        }

        return classDescriptors;
    }

    private Collection<ClassDescriptor> getJetClassesDescriptorsByFQName(
            @NotNull KotlinCodeAnalyzer analyzer, @NotNull FqName classFQName, @NotNull GlobalSearchScope searchScope) {
        Collection<JetClassOrObject> jetClassOrObjects = JetFullClassNameIndex.getInstance().get(
                classFQName.asString(), project, searchScope);

        if (jetClassOrObjects.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return Collections.emptyList();
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassDescriptorsByFqName(analyzer, classFQName);
    }

    @NotNull
    @Override
    public PsiMethod[] getMethodsByName(@NonNls @NotNull String name, @NotNull GlobalSearchScope scope) {
        return NO_METHODS;
    }

    @NotNull
    @Override
    public PsiMethod[] getMethodsByNameIfNotMoreThan(@NonNls @NotNull String name, @NotNull GlobalSearchScope scope, int maxCount) {
        return NO_METHODS;
    }

    @NotNull
    @Override
    public PsiField[] getFieldsByNameIfNotMoreThan(@NonNls @NotNull String s, @NotNull GlobalSearchScope scope, int i) {
        return NO_FIELDS;
    }

    @Override
    public boolean processMethodsWithName(
            @NonNls @NotNull String name,
            @NotNull GlobalSearchScope scope,
            @NotNull Processor<PsiMethod> processor
    ) {
        return ContainerUtil.process(getMethodsByName(name, scope), processor);
    }

    @NotNull
    @Override
    public String[] getAllMethodNames() {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public void getAllMethodNames(@NotNull HashSet<String> set) {
        set.addAll(JetShortFunctionNameIndex.getInstance().getAllKeys(project));
    }

    @NotNull
    @Override
    public PsiField[] getFieldsByName(@NotNull @NonNls String name, @NotNull GlobalSearchScope scope) {
        return NO_FIELDS;
    }

    @NotNull
    @Override
    public String[] getAllFieldNames() {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public void getAllFieldNames(@NotNull HashSet<String> set) {
    }
}
