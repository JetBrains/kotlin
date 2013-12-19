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

package org.jetbrains.jet.plugin.project;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.*;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForBodyResolve;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.lazy.LazyDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.LazyResolveStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ResolveElementCache {
    private static final BodyResolveContextForLazy EMPTY_CONTEXT = new BodyResolveContextForLazy(Functions.<JetScope>constant(null));

    private final CachedValue<MemoizedFunctionToNotNull<JetElement, BindingContext>> additionalResolveCache;
    private final ResolveSession resolveSession;

    public ResolveElementCache(ResolveSession resolveSession, Project project) {
        this.resolveSession = resolveSession;

        // Recreate internal cache after change of modification count
        this.additionalResolveCache =
                CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<MemoizedFunctionToNotNull<JetElement, BindingContext>>() {
                            @Nullable
                            @Override
                            public Result<MemoizedFunctionToNotNull<JetElement, BindingContext>> compute() {
                                LazyResolveStorageManager manager = ResolveElementCache.this.resolveSession.getStorageManager();
                                MemoizedFunctionToNotNull<JetElement, BindingContext> elementsCacheFunction =
                                        manager.createWeaklyRetainedMemoizedFunction(new Function1<JetElement, BindingContext>() {
                                            @Override
                                            public BindingContext invoke(JetElement jetElement) {
                                                return elementAdditionalResolve(jetElement);
                                            }
                                        });

                                return Result.create(elementsCacheFunction, PsiModificationTracker.MODIFICATION_COUNT);
                            }
                        },
                        false);
    }

    @NotNull
    public BindingContext resolveElement(@NotNull JetElement jetElement) {
        @SuppressWarnings("unchecked") JetElement elementOfAdditionalResolve = (JetElement) JetPsiUtil.getTopmostParentOfTypes(
                jetElement,
                JetNamedFunction.class,
                JetClassInitializer.class,
                JetProperty.class,
                JetDelegationSpecifierList.class,
                JetImportDirective.class,
                JetAnnotationEntry.class,
                JetTypeParameter.class,
                JetTypeConstraint.class,
                JetNamespaceHeader.class);

        if (elementOfAdditionalResolve != null) {
            if (elementOfAdditionalResolve instanceof JetNamespaceHeader) {
                elementOfAdditionalResolve = jetElement;
            }

            return additionalResolveCache.getValue().invoke(elementOfAdditionalResolve);
        }

        JetParameter parameter = PsiTreeUtil.getTopmostParentOfType(jetElement, JetParameter.class);
        if (parameter != null) {
            JetClass klass = PsiTreeUtil.getParentOfType(parameter, JetClass.class);
            if (klass != null && parameter.getParent() == klass.getPrimaryConstructorParameterList()) {
                return additionalResolveCache.getValue().invoke(klass);
            }

            // Parameters for function literal could be met inside other parameters. We can't make resolveToDescriptors for internal elements.
            jetElement = parameter;
        }

        JetDeclaration declaration = PsiTreeUtil.getParentOfType(jetElement, JetDeclaration.class, false);
        if (declaration != null) {
            // Activate descriptor resolution
            resolveSession.resolveToDescriptor(declaration);
        }

        return resolveSession.getBindingContext();
    }

    @NotNull
    private BindingContext elementAdditionalResolve(@NotNull JetElement resolveElement) {
        // All additional resolve should be done to separate trace
        BindingTrace trace = resolveSession.getStorageManager().createSafeTrace(
                new DelegatingBindingTrace(resolveSession.getBindingContext(), "trace to resolve element", resolveElement));

        JetFile file = (JetFile) resolveElement.getContainingFile();

        if (resolveElement instanceof JetNamedFunction) {
            functionAdditionalResolve(resolveSession, (JetNamedFunction) resolveElement, trace, file);
        }
        else if (resolveElement instanceof JetClassInitializer) {
            initializerAdditionalResolve(resolveSession, (JetClassInitializer) resolveElement, trace, file);
        }
        else if (resolveElement instanceof JetProperty) {
            propertyAdditionalResolve(resolveSession, (JetProperty) resolveElement, trace, file);
        }
        else if (resolveElement instanceof JetDelegationSpecifierList) {
            delegationSpecifierAdditionalResolve(resolveSession, (JetDelegationSpecifierList) resolveElement, trace, file);
        }
        else if (resolveElement instanceof JetImportDirective) {
            JetImportDirective importDirective = (JetImportDirective) resolveElement;
            JetScope scope = resolveSession.getInjector().getScopeProvider().getFileScope((JetFile) importDirective.getContainingFile());

            // Get all descriptors to force resolving all imports
            scope.getAllDescriptors();
        }
        else if (resolveElement instanceof JetAnnotationEntry) {
            annotationAdditionalResolve(resolveSession, (JetAnnotationEntry) resolveElement);
        }
        else if (resolveElement instanceof JetClass) {
            constructorAdditionalResolve(resolveSession, (JetClass) resolveElement, trace, file);
        }
        else if (resolveElement instanceof JetTypeParameter) {
            typeParameterAdditionalResolve(resolveSession, (JetTypeParameter) resolveElement);
        }
        else if (resolveElement instanceof JetTypeConstraint) {
            typeConstraintAdditionalResolve(resolveSession, (JetTypeConstraint) resolveElement);
        }
        else if (PsiTreeUtil.getParentOfType(resolveElement, JetNamespaceHeader.class) != null) {
            namespaceRefAdditionalResolve(resolveSession, trace, resolveElement);
        }
        else {
            assert false : "Invalid type of the topmost parent";
        }

        return trace.getBindingContext();
    }

    private static void namespaceRefAdditionalResolve(ResolveSession resolveSession, BindingTrace trace, JetElement jetElement) {
        if (jetElement instanceof JetSimpleNameExpression) {
            JetNamespaceHeader header = PsiTreeUtil.getParentOfType(jetElement, JetNamespaceHeader.class);
            assert header != null;

            JetSimpleNameExpression packageNameExpression = (JetSimpleNameExpression) jetElement;
            if (trace.getBindingContext().get(BindingContext.RESOLUTION_SCOPE, packageNameExpression) == null) {
                JetScope scope = getExpressionMemberScope(resolveSession, packageNameExpression);
                if (scope != null) {
                    trace.record(BindingContext.RESOLUTION_SCOPE, packageNameExpression, scope);
                }
            }

            if (Name.isValidIdentifier(packageNameExpression.getReferencedName())) {
                if (trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, packageNameExpression) == null) {
                    FqName fqName = header.getFqName(packageNameExpression);
                    PackageViewDescriptor packageDescriptor = resolveSession.getModuleDescriptor().getPackage(fqName);
                    assert packageDescriptor != null: "Package descriptor should be present in session for " + fqName;
                    trace.record(BindingContext.REFERENCE_TARGET, packageNameExpression, packageDescriptor);
                }
            }
        }
    }

    private static void typeConstraintAdditionalResolve(KotlinCodeAnalyzer analyzer, JetTypeConstraint jetTypeConstraint) {
        JetDeclaration declaration = PsiTreeUtil.getParentOfType(jetTypeConstraint, JetDeclaration.class);
        DeclarationDescriptor descriptor = analyzer.resolveToDescriptor(declaration);

        assert (descriptor instanceof ClassDescriptor);

        TypeConstructor constructor = ((ClassDescriptor) descriptor).getTypeConstructor();
        for (TypeParameterDescriptor parameterDescriptor : constructor.getParameters()) {
            LazyDescriptor lazyDescriptor = (LazyDescriptor) parameterDescriptor;
            lazyDescriptor.forceResolveAllContents();
        }
    }

    private static void annotationAdditionalResolve(KotlinCodeAnalyzer analyzer, JetAnnotationEntry jetAnnotationEntry) {
        JetDeclaration declaration = PsiTreeUtil.getParentOfType(jetAnnotationEntry, JetDeclaration.class);
        if (declaration != null) {
            Annotated descriptor = analyzer.resolveToDescriptor(declaration);

            // Activate annotation resolving
            descriptor.getAnnotations();
        }
    }

    private static void typeParameterAdditionalResolve(KotlinCodeAnalyzer analyzer, JetTypeParameter typeParameter) {
        DeclarationDescriptor descriptor = analyzer.resolveToDescriptor(typeParameter);
        assert descriptor instanceof LazyDescriptor;

        LazyDescriptor parameterDescriptor = (LazyDescriptor) descriptor;
        parameterDescriptor.forceResolveAllContents();
    }

    private static void delegationSpecifierAdditionalResolve(
            KotlinCodeAnalyzer analyzer,
            JetDelegationSpecifierList specifier, BindingTrace trace, JetFile file) {
        BodyResolver bodyResolver = createBodyResolverWithEmptyContext(trace, file, analyzer.getModuleDescriptor());

        JetClassOrObject classOrObject = (JetClassOrObject) specifier.getParent();
        LazyClassDescriptor descriptor = (LazyClassDescriptor) analyzer.resolveToDescriptor(classOrObject);

        // Activate resolving of supertypes
        descriptor.getTypeConstructor().getSupertypes();

        bodyResolver.resolveDelegationSpecifierList(classOrObject, descriptor,
                                                    descriptor.getUnsubstitutedPrimaryConstructor(),
                                                    descriptor.getScopeForClassHeaderResolution(),
                                                    descriptor.getScopeForMemberDeclarationResolution());
    }

    private static void propertyAdditionalResolve(ResolveSession resolveSession, final JetProperty jetProperty, BindingTrace trace, JetFile file) {
        final JetScope propertyResolutionScope = resolveSession.getInjector().getScopeProvider().getResolutionScopeForDeclaration(
                jetProperty);

        BodyResolveContextForLazy bodyResolveContext = new BodyResolveContextForLazy(new Function<JetDeclaration, JetScope>() {
            @Override
            public JetScope apply(JetDeclaration declaration) {
                assert declaration.getParent() == jetProperty : "Must be called only for property accessors, but called for " + declaration;
                return propertyResolutionScope;
            }
        });
        BodyResolver bodyResolver = createBodyResolver(trace, file, bodyResolveContext, resolveSession.getModuleDescriptor());
        PropertyDescriptor descriptor = (PropertyDescriptor) resolveSession.resolveToDescriptor(jetProperty);

        JetExpression propertyInitializer = jetProperty.getInitializer();
        if (propertyInitializer != null) {
            bodyResolver.resolvePropertyInitializer(jetProperty, descriptor, propertyInitializer, propertyResolutionScope);
        }

        JetExpression propertyDelegate = jetProperty.getDelegateExpression();
        if (propertyDelegate != null) {
            bodyResolver.resolvePropertyDelegate(jetProperty, descriptor, propertyDelegate, propertyResolutionScope, propertyResolutionScope);
        }

        bodyResolver.resolvePropertyAccessors(jetProperty, descriptor);
    }

    private static void functionAdditionalResolve(
            ResolveSession resolveSession,
            JetNamedFunction namedFunction,
            BindingTrace trace,
            JetFile file
    ) {
        BodyResolver bodyResolver = createBodyResolverWithEmptyContext(trace, file, resolveSession.getModuleDescriptor());
        JetScope scope = resolveSession.getInjector().getScopeProvider().getResolutionScopeForDeclaration(namedFunction);
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) resolveSession.resolveToDescriptor(namedFunction);
        bodyResolver.resolveFunctionBody(trace, namedFunction, functionDescriptor, scope);
    }

    private static void constructorAdditionalResolve(
            ResolveSession resolveSession,
            JetClass klass,
            BindingTrace trace,
            JetFile file
    ) {
        BodyResolver bodyResolver = createBodyResolverWithEmptyContext(trace, file, resolveSession.getModuleDescriptor());
        JetScope scope = resolveSession.getInjector().getScopeProvider().getResolutionScopeForDeclaration(klass);

        ClassDescriptor classDescriptor = (ClassDescriptor) resolveSession.resolveToDescriptor(klass);
        ConstructorDescriptor constructorDescriptor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        assert constructorDescriptor != null;

        bodyResolver.resolveConstructorParameterDefaultValuesAndAnnotations(trace, klass, constructorDescriptor, scope);
    }

    private static boolean initializerAdditionalResolve(
            KotlinCodeAnalyzer analyzer,
            JetClassInitializer classInitializer,
            BindingTrace trace,
            JetFile file
    ) {
        BodyResolver bodyResolver = createBodyResolverWithEmptyContext(trace, file, analyzer.getModuleDescriptor());
        JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(classInitializer, JetClassOrObject.class);
        LazyClassDescriptor classOrObjectDescriptor = (LazyClassDescriptor) analyzer.resolveToDescriptor(classOrObject);
        bodyResolver.resolveAnonymousInitializers(classOrObject, classOrObjectDescriptor.getUnsubstitutedPrimaryConstructor(),
                classOrObjectDescriptor.getScopeForPropertyInitializerResolution());

        return true;
    }

    private static BodyResolver createBodyResolver(BindingTrace trace, JetFile file, BodyResolveContextForLazy bodyResolveContext,
            ModuleDescriptor module) {
        TopDownAnalysisParameters parameters = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, true, Collections.<AnalyzerScriptParameter>emptyList());
        InjectorForBodyResolve bodyResolve = new InjectorForBodyResolve(file.getProject(), parameters, trace, bodyResolveContext, module);
        return bodyResolve.getBodyResolver();
    }

    private static BodyResolver createBodyResolverWithEmptyContext(
            BindingTrace trace,
            JetFile file,
            ModuleDescriptor module
    ) {
        return createBodyResolver(trace, file, EMPTY_CONTEXT, module);
    }

    private static JetScope getExpressionResolutionScope(@NotNull ResolveSession resolveSession, @NotNull JetExpression expression) {
        ScopeProvider provider = resolveSession.getInjector().getScopeProvider();
        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);
        if (parentDeclaration == null) {
            return provider.getFileScope((JetFile) expression.getContainingFile());
        }
        return provider.getResolutionScopeForDeclaration(parentDeclaration);
    }

    private static JetScope getExpressionMemberScope(@NotNull ResolveSession resolveSession, @NotNull JetExpression expression) {
        BindingTrace trace = resolveSession.getStorageManager().createSafeTrace(new DelegatingBindingTrace(
                resolveSession.getBindingContext(), "trace to resolve a member scope of expression", expression));

        if (BindingContextUtils.isExpressionWithValidReference(expression, resolveSession.getBindingContext())) {
            QualifiedExpressionResolver qualifiedExpressionResolver = resolveSession.getInjector().getQualifiedExpressionResolver();

            // In some type declaration
            if (expression.getParent() instanceof JetUserType) {
                JetUserType qualifier = ((JetUserType) expression.getParent()).getQualifier();
                if (qualifier != null) {
                    Collection<? extends DeclarationDescriptor> descriptors = qualifiedExpressionResolver
                            .lookupDescriptorsForUserType(qualifier, getExpressionResolutionScope(resolveSession, expression), trace);

                    for (DeclarationDescriptor descriptor : descriptors) {
                        if (descriptor instanceof LazyPackageDescriptor) {
                            return ((LazyPackageDescriptor) descriptor).getMemberScope();
                        }
                    }
                }
            }

            // Inside import
            if (PsiTreeUtil.getParentOfType(expression, JetImportDirective.class, false) != null) {
                PackageViewDescriptor rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT);
                assert rootPackage != null;

                if (expression.getParent() instanceof JetDotQualifiedExpression) {
                    JetExpression element = ((JetDotQualifiedExpression) expression.getParent()).getReceiverExpression();
                    String name = ((JetFile) expression.getContainingFile()).getPackageName();

                    PackageViewDescriptor filePackage = name != null
                                                        ? resolveSession.getModuleDescriptor().getPackage(new FqName(name))
                                                        : rootPackage;
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
                PackageViewDescriptor packageDescriptor = resolveSession.getModuleDescriptor().getPackage(
                        namespaceHeader.getFqName((JetSimpleNameExpression) expression).parent());
                if (packageDescriptor != null) {
                    return packageDescriptor.getMemberScope();
                }
            }
        }

        return null;
    }

    private static class BodyResolveContextForLazy implements BodiesResolveContext {

        private final Function<? super JetDeclaration, JetScope> declaringScopes;

        private BodyResolveContextForLazy(@NotNull Function<? super JetDeclaration, JetScope> declaringScopes) {
            this.declaringScopes = declaringScopes;
        }

        @Override
        public Collection<JetFile> getFiles() {
            return Collections.emptySet();
        }

        @Override
        public Map<JetClassOrObject, MutableClassDescriptor> getClasses() {
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
            //noinspection unchecked
            return (Function<JetDeclaration, JetScope>) declaringScopes;
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
        public DataFlowInfo getOuterDataFlowInfo() {
            return DataFlowInfo.EMPTY;
        }

        @Override
        public void setTopDownAnalysisParameters(TopDownAnalysisParameters parameters) {
        }

        @Override
        public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
            return true;
        }
    }
}
