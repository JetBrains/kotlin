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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForBodyResolve;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.*;

public class ResolveSessionUtils {

    // This name is used as a key for the case when something has no name _due to a syntactic error_
    // Example: fun (x: Int) = 5
    //          There's no name for this function in the PSI
    // The name contains a GUID to avoid clashes, if a clash happens, it's not a big deal: the code does not compile anyway
    public static final Name NO_NAME_FOR_LAZY_RESOLVE = Name.identifier("no_name_in_PSI_for_lazy_resolve_3d19d79d_1ba9_4cd0_b7f5_b46aa3cd5d40");

    private ResolveSessionUtils() {
    }

    @SuppressWarnings("unchecked")
    private static final BodyResolveContextForLazy EMPTY_CONTEXT = new BodyResolveContextForLazy((Function) Functions.constant(null));

    private static class BodyResolveContextForLazy implements BodiesResolveContext {

        private final Function<JetDeclaration, JetScope> declaringScopes;

        private BodyResolveContextForLazy(@NotNull Function<JetDeclaration, JetScope> declaringScopes) {
            this.declaringScopes = declaringScopes;
        }

        @Override
        public Collection<JetFile> getFiles() {
            return Collections.emptySet();
        }

        @Override
        public Map<JetClass, MutableClassDescriptor> getClasses() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetObjectDeclaration, MutableClassDescriptor> getObjects() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetProperty, PropertyDescriptor> getProperties() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions() {
            return Collections.emptyMap();
        }

        @Override
        public Function<JetDeclaration, JetScope> getDeclaringScopes() {
            return declaringScopes;
        }

        @Override
        public Map<JetScript, ScriptDescriptor> getScripts() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetScript, WritableScope> getScriptScopes() {
            return Collections.emptyMap();
        }

        @Override
        public void setTopDownAnalysisParameters(TopDownAnalysisParameters parameters) {
        }

        @Override
        public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
            return true;
        }
    }

    public static @NotNull BindingContext resolveToExpression(
            @NotNull final LazyCodeAnalyzer analyzer,
            @NotNull JetExpression expression
    ) {
        final DelegatingBindingTrace trace = new DelegatingBindingTrace(
                analyzer.getBindingContext(), "trace to resolve expression", expression);
        JetFile file = (JetFile) expression.getContainingFile();

        @SuppressWarnings("unchecked")
        PsiElement topmostCandidateForAdditionalResolve = JetPsiUtil.getTopmostParentOfTypes(expression,
                JetNamedFunction.class, JetClassInitializer.class, JetProperty.class, JetDelegationSpecifierList.class);

        if (topmostCandidateForAdditionalResolve != null) {
            if (topmostCandidateForAdditionalResolve instanceof JetNamedFunction) {
                functionAdditionalResolve(analyzer, (JetNamedFunction) topmostCandidateForAdditionalResolve, trace, file);
            }
            else if (topmostCandidateForAdditionalResolve instanceof JetClassInitializer) {
                initializerAdditionalResolve(analyzer, (JetClassInitializer) topmostCandidateForAdditionalResolve, trace, file);
            }
            else if (topmostCandidateForAdditionalResolve instanceof JetProperty) {
                propertyAdditionalResolve(analyzer, (JetProperty) topmostCandidateForAdditionalResolve, trace, file);
            }
            else if (topmostCandidateForAdditionalResolve instanceof JetDelegationSpecifierList) {
                delegationSpecifierAdditionalResolve(analyzer, (JetDelegationSpecifierList) topmostCandidateForAdditionalResolve,
                                                     trace, file);
            }
            else {
                assert false : "Invalid type of the topmost parent";
            }

            return trace.getBindingContext();
        }

        // Setup resolution scope explicitly
        if (trace.getBindingContext().get(BindingContext.RESOLUTION_SCOPE, expression) == null) {
            JetScope scope = getExpressionMemberScope(analyzer, expression);
            if (scope != null) {
                trace.record(BindingContext.RESOLUTION_SCOPE, expression, scope);
            }
        }

        return trace.getBindingContext();
    }

    private static void delegationSpecifierAdditionalResolve(
            LazyCodeAnalyzer analyzer,
            JetDelegationSpecifierList specifier,
            DelegatingBindingTrace trace,
            JetFile file
    ) {
        BodyResolver bodyResolver = createBodyResolverWithEmptyContext(trace, file, analyzer.getModuleSourcesManager());

        JetClassOrObject classOrObject = (JetClassOrObject) specifier.getParent();
        LazyClassDescriptor descriptor = (LazyClassDescriptor) analyzer.getDescriptor(classOrObject);

        // Activate resolving of supertypes
        descriptor.getTypeConstructor().getSupertypes();

        bodyResolver.resolveDelegationSpecifierList(classOrObject, descriptor,
                                                    descriptor.getUnsubstitutedPrimaryConstructor(),
                                                    descriptor.getScopeForClassHeaderResolution(),
                                                    descriptor.getScopeForMemberDeclarationResolution());
    }

    private static void propertyAdditionalResolve(final LazyCodeAnalyzer analyzer, final JetProperty jetProperty, DelegatingBindingTrace trace, JetFile file) {
        final JetScope propertyResolutionScope = analyzer.getInjector().getScopeProvider().getResolutionScopeForDeclaration(
                jetProperty);

        BodyResolveContextForLazy bodyResolveContext = new BodyResolveContextForLazy(new Function<JetDeclaration, JetScope>() {
            @Override
            public JetScope apply(JetDeclaration declaration) {
                assert declaration.getParent() == jetProperty : "Must be called only for property accessors, but called for " + declaration;
                return propertyResolutionScope;
            }
        });
        BodyResolver bodyResolver = createBodyResolver(trace, file, bodyResolveContext, analyzer.getModuleSourcesManager());
        PropertyDescriptor descriptor = (PropertyDescriptor) analyzer.getDescriptor(jetProperty);

        JetExpression propertyInitializer = jetProperty.getInitializer();

        if (propertyInitializer != null) {
            bodyResolver.resolvePropertyInitializer(jetProperty, descriptor, propertyInitializer, propertyResolutionScope);
        }

        bodyResolver.resolvePropertyAccessors(jetProperty, descriptor);
    }

    private static void functionAdditionalResolve(
            LazyCodeAnalyzer analyzer,
            JetNamedFunction namedFunction,
            DelegatingBindingTrace trace,
            JetFile file
    ) {
        BodyResolver bodyResolver = createBodyResolverWithEmptyContext(trace, file, analyzer.getModuleSourcesManager());
        JetScope scope = analyzer.getInjector().getScopeProvider().getResolutionScopeForDeclaration(namedFunction);
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) analyzer.getDescriptor(namedFunction);
        bodyResolver.resolveFunctionBody(trace, namedFunction, functionDescriptor, scope);
    }

    private static boolean initializerAdditionalResolve(
            LazyCodeAnalyzer analyzer,
            JetClassInitializer classInitializer,
            DelegatingBindingTrace trace,
            JetFile file
    ) {
        BodyResolver bodyResolver = createBodyResolverWithEmptyContext(trace, file, analyzer.getModuleSourcesManager());
        JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(classInitializer, JetClassOrObject.class);
        LazyClassDescriptor classOrObjectDescriptor = (LazyClassDescriptor) analyzer.getDescriptor(classOrObject);
        bodyResolver.resolveAnonymousInitializers(classOrObject, classOrObjectDescriptor.getUnsubstitutedPrimaryConstructor(),
                classOrObjectDescriptor.getScopeForPropertyInitializerResolution());

        return true;
    }

    private static BodyResolver createBodyResolver(DelegatingBindingTrace trace, JetFile file, BodyResolveContextForLazy bodyResolveContext,
            ModuleSourcesManager moduleSourcesManager) {
        TopDownAnalysisParameters parameters = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, true, Collections.<AnalyzerScriptParameter>emptyList());
        InjectorForBodyResolve bodyResolve = new InjectorForBodyResolve(file.getProject(), parameters, trace, bodyResolveContext,
                                                                        moduleSourcesManager);
        return bodyResolve.getBodyResolver();
    }

    private static BodyResolver createBodyResolverWithEmptyContext(
            DelegatingBindingTrace trace,
            JetFile file,
            ModuleSourcesManager moduleSourcesManager
    ) {
        return createBodyResolver(trace, file, EMPTY_CONTEXT, moduleSourcesManager);
    }

    private static JetScope getExpressionResolutionScope(@NotNull LazyCodeAnalyzer analyzer, @NotNull JetExpression expression) {
        ScopeProvider provider = analyzer.getInjector().getScopeProvider();
        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);
        if (parentDeclaration == null) {
            return provider.getFileScope((JetFile) expression.getContainingFile());
        }
        return provider.getResolutionScopeForDeclaration(parentDeclaration);
    }

    public static JetScope getExpressionMemberScope(@NotNull LazyCodeAnalyzer analyzer, @NotNull JetExpression expression) {
        DelegatingBindingTrace trace = new DelegatingBindingTrace(
                analyzer.getBindingContext(), "trace to resolve a member scope of expression", expression);

        if (expression instanceof JetReferenceExpression) {
            QualifiedExpressionResolver qualifiedExpressionResolver = analyzer.getInjector().getQualifiedExpressionResolver();

            // In some type declaration
            if (expression.getParent() instanceof JetUserType) {
                JetUserType qualifier = ((JetUserType) expression.getParent()).getQualifier();
                if (qualifier != null) {
                    Collection<? extends DeclarationDescriptor> descriptors = qualifiedExpressionResolver
                            .lookupDescriptorsForUserType(qualifier, getExpressionResolutionScope(analyzer, expression), trace);

                    for (DeclarationDescriptor descriptor : descriptors) {
                        if (descriptor instanceof LazyPackageDescriptor) {
                            return ((LazyPackageDescriptor) descriptor).getMemberScope();
                        }
                    }
                }
            }

            SubModuleDescriptor subModule = analyzer.getModuleSourcesManager().getSubModuleForFile(expression.getContainingFile());

            // Inside import
            if (PsiTreeUtil.getParentOfType(expression, JetImportDirective.class, false) != null) {

                PackageViewDescriptor rootPackage = DescriptorUtils.getRootPackage(subModule);

                if (expression.getParent() instanceof JetDotQualifiedExpression) {
                    JetExpression element = ((JetDotQualifiedExpression) expression.getParent()).getReceiverExpression();
                    String name = ((JetFile) expression.getContainingFile()).getPackageName();

                    PackageViewDescriptor filePackage =
                            name != null ? subModule.getPackageView(new FqName(name)) : rootPackage;
                    assert filePackage != null : "File package should be already resolved and be found";

                    JetScope scope = filePackage.getMemberScope();
                    Collection<? extends DeclarationDescriptor> descriptors;

                    if (element instanceof JetDotQualifiedExpression) {
                        descriptors = qualifiedExpressionResolver.lookupDescriptorsForQualifiedExpression(
                                (JetDotQualifiedExpression) element, rootPackage.getMemberScope(), scope, trace,
                                QualifiedExpressionResolver.LookupMode.EVERYTHING, false);
                    }
                    else {
                        descriptors = qualifiedExpressionResolver.lookupDescriptorsForSimpleNameReference(
                                (JetSimpleNameExpression) element, rootPackage.getMemberScope(), scope, trace,
                                QualifiedExpressionResolver.LookupMode.EVERYTHING, false, false);
                    }

                    for (DeclarationDescriptor descriptor : descriptors) {
                        if (descriptor instanceof PackageViewDescriptor) {
                            return ((PackageViewDescriptor) descriptor).getMemberScope();
                        }
                    }
                }
                else {
                    return rootPackage.getMemberScope();
                }
            }

            // Inside package declaration
            JetNamespaceHeader namespaceHeader = PsiTreeUtil.getParentOfType(expression, JetNamespaceHeader.class, false);
            if (namespaceHeader != null) {
                PackageViewDescriptor packageDescriptor = subModule.getPackageView(
                        namespaceHeader.getParentFqName((JetReferenceExpression) expression));
                if (packageDescriptor != null) {
                    return packageDescriptor.getMemberScope();
                }
            }
        }

        return null;
    }

    @NotNull
    public static Collection<ClassDescriptor> getClassDescriptorsByFqName(
                @NotNull SubModuleDescriptor subModuleDescriptor,
                @NotNull FqName fqName
    ) {
        return getClassOrObjectDescriptorsByFqName(subModuleDescriptor, fqName, false);
    }

    @NotNull
    public static Collection<ClassDescriptor> getClassOrObjectDescriptorsByFqName(
            @NotNull SubModuleDescriptor subModule,
            @NotNull FqName fqName,
            boolean includeObjectDeclarations
    ) {
        if (fqName.isRoot()) {
            return Collections.emptyList();
        }

        Collection<ClassDescriptor> classDescriptors = Lists.newArrayList();

        FqName packageFqName = fqName.parent();
        while (true) {
            PackageViewDescriptor packageDescriptor = subModule.getPackageView(packageFqName);
            if (packageDescriptor != null) {
                FqName classInPackagePath = new FqName(QualifiedNamesUtil.tail(packageFqName, fqName));
                Collection<ClassDescriptor> descriptors = getClassOrObjectDescriptorsByFqName(packageDescriptor, classInPackagePath,
                                                                                              includeObjectDeclarations);
                classDescriptors.addAll(descriptors);
            }

            if (packageFqName.isRoot()) {
                break;
            }
            else {
                packageFqName = packageFqName.parent();
            }
        }

        return classDescriptors;
    }

    private static Collection<ClassDescriptor> getClassOrObjectDescriptorsByFqName(
            PackageViewDescriptor packageDescriptor,
            FqName path,
            boolean includeObjectDeclarations
    ) {
        if (path.isRoot()) {
            return Collections.emptyList();
        }

        Collection<JetScope> scopes = Arrays.asList(packageDescriptor.getMemberScope());

        List<Name> names = path.pathSegments();
        if (names.size() > 1) {
            for (Name subName : path.pathSegments().subList(0, names.size() - 1)) {
                Collection<JetScope> tempScopes = Lists.newArrayList();
                for (JetScope scope : scopes) {
                    ClassifierDescriptor classifier = scope.getClassifier(subName);
                    if (classifier instanceof ClassDescriptorBase) {
                        ClassDescriptorBase classDescriptor = (ClassDescriptorBase) classifier;
                        tempScopes.add(classDescriptor.getUnsubstitutedInnerClassesScope());
                    }
                }
                scopes = tempScopes;
            }
        }

        Name shortName = path.shortName();
        Collection<ClassDescriptor> resultClassifierDescriptors = Lists.newArrayList();
        for (JetScope scope : scopes) {
            ClassifierDescriptor classifier = scope.getClassifier(shortName);
            if (classifier instanceof ClassDescriptor) {
                resultClassifierDescriptors.add((ClassDescriptor) classifier);
            }
            if (includeObjectDeclarations) {
                ClassDescriptor objectDescriptor = scope.getObjectDescriptor(shortName);
                if (objectDescriptor != null) {
                    resultClassifierDescriptors.add(objectDescriptor);
                }
            }
        }

        return resultClassifierDescriptors;
    }

    @NotNull
    public static Name safeNameForLazyResolve(@NotNull JetNamed named) {
        Name name = named.getNameAsName();
        return safeNameForLazyResolve(name);
    }

    @NotNull
    public static Name safeNameForLazyResolve(@Nullable Name name) {
        return name != null ? name : NO_NAME_FOR_LAZY_RESOLVE;
    }
}
