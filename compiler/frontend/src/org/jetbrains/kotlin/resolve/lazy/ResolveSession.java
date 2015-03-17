/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.kotlin.resolve.lazy.data.JetScriptInfo;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotations;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationsContextImpl;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.storage.*;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ResolveSession implements KotlinCodeAnalyzer, LazyClassContext {
    private final LazyResolveStorageManager storageManager;
    private final ExceptionTracker exceptionTracker;

    private final ModuleDescriptor module;

    private final BindingTrace trace;
    private final DeclarationProviderFactory declarationProviderFactory;

    private final MemoizedFunctionToNullable<FqName, LazyPackageDescriptor> packages;
    private final PackageFragmentProvider packageFragmentProvider;

    private final MemoizedFunctionToNotNull<JetScript, LazyScriptDescriptor> scriptDescriptors;

    private final MemoizedFunctionToNotNull<JetFile, LazyAnnotations> fileAnnotations;
    private final MemoizedFunctionToNotNull<JetFile, LazyAnnotations> danglingAnnotations;

    private ScopeProvider scopeProvider;

    private JetImportsFactory jetImportFactory;
    private AnnotationResolver annotationResolve;
    private DescriptorResolver descriptorResolver;
    private FunctionDescriptorResolver functionDescriptorResolver;
    private TypeResolver typeResolver;
    private QualifiedExpressionResolver qualifiedExpressionResolver;
    private ScriptBodyResolver scriptBodyResolver;
    private LazyDeclarationResolver lazyDeclarationResolver;

    @Inject
    public void setJetImportFactory(JetImportsFactory jetImportFactory) {
        this.jetImportFactory = jetImportFactory;
    }

    @Inject
    public void setAnnotationResolve(AnnotationResolver annotationResolve) {
        this.annotationResolve = annotationResolve;
    }

    @Inject
    public void setDescriptorResolver(DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setFunctionDescriptorResolver(FunctionDescriptorResolver functionDescriptorResolver) {
        this.functionDescriptorResolver = functionDescriptorResolver;
    }

    @Inject
    public void setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setQualifiedExpressionResolver(QualifiedExpressionResolver qualifiedExpressionResolver) {
        this.qualifiedExpressionResolver = qualifiedExpressionResolver;
    }

    @Inject
    public void setScopeProvider(ScopeProvider scopeProvider) {
        this.scopeProvider = scopeProvider;
    }

    @Inject
    public void setScriptBodyResolver(ScriptBodyResolver scriptBodyResolver) {
        this.scriptBodyResolver = scriptBodyResolver;
    }

    @Inject
    public void setLazyDeclarationResolver(LazyDeclarationResolver lazyDeclarationResolver) {
        this.lazyDeclarationResolver = lazyDeclarationResolver;
    }

    // Only calls from injectors expected
    @Deprecated
    public ResolveSession(
            @NotNull Project project,
            @NotNull GlobalContext globalContext,
            @NotNull ModuleDescriptorImpl rootDescriptor,
            @NotNull DeclarationProviderFactory declarationProviderFactory,
            @NotNull BindingTrace delegationTrace
    ) {
        LockBasedLazyResolveStorageManager lockBasedLazyResolveStorageManager =
                new LockBasedLazyResolveStorageManager(globalContext.getStorageManager());

        this.storageManager = lockBasedLazyResolveStorageManager;
        this.exceptionTracker = globalContext.getExceptionTracker();
        this.trace = lockBasedLazyResolveStorageManager.createSafeTrace(delegationTrace);
        this.module = rootDescriptor;

        this.packages =
                storageManager.createMemoizedFunctionWithNullableValues(new MemoizedFunctionToNullable<FqName, LazyPackageDescriptor>() {
                    @Nullable
                    @Override
                    public LazyPackageDescriptor invoke(FqName fqName) {
                        return createPackage(fqName);
                    }
                });

        this.declarationProviderFactory = declarationProviderFactory;

        this.packageFragmentProvider = new PackageFragmentProvider() {
            @NotNull
            @Override
            public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
                return ContainerUtil.<PackageFragmentDescriptor>createMaybeSingletonList(getPackageFragment(fqName));
            }

            @NotNull
            @Override
            public Collection<FqName> getSubPackagesOf(
                    @NotNull FqName fqName, @NotNull Function1<? super Name, ? extends Boolean> nameFilter
            ) {
                LazyPackageDescriptor packageDescriptor = getPackageFragment(fqName);
                if (packageDescriptor == null) {
                    return Collections.emptyList();
                }
                return packageDescriptor.getDeclarationProvider().getAllDeclaredSubPackages();
            }
        };

        this.scriptDescriptors = storageManager.createMemoizedFunction(
                new Function1<JetScript, LazyScriptDescriptor>() {
                    @Override
                    public LazyScriptDescriptor invoke(JetScript script) {
                        return new LazyScriptDescriptor(
                                ResolveSession.this,
                                scriptBodyResolver,
                                script,
                                ScriptPriorities.getScriptPriority(script)
                        );
                    }
                }
        );

        fileAnnotations = storageManager.createMemoizedFunction(new Function1<JetFile, LazyAnnotations>() {
            @Override
            public LazyAnnotations invoke(JetFile file) {
                return createAnnotations(file, file.getAnnotationEntries());
            }
        });

        danglingAnnotations = storageManager.createMemoizedFunction(new Function1<JetFile, LazyAnnotations>() {
            @Override
            public LazyAnnotations invoke(JetFile file) {
                return createAnnotations(file, file.getDanglingAnnotations());
            }
        });
    }

    private LazyAnnotations createAnnotations(JetFile file, List<JetAnnotationEntry> annotationEntries) {
        JetScope scope = getScopeProvider().getFileScope(file);
        LazyAnnotationsContextImpl lazyAnnotationContext = new LazyAnnotationsContextImpl(annotationResolve, storageManager, trace, scope);
        return new LazyAnnotations(lazyAnnotationContext, annotationEntries);
    }

    @Override
    @NotNull
    public PackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }

    @Override
    @Nullable
    public LazyPackageDescriptor getPackageFragment(@NotNull FqName fqName) {
        return packages.invoke(fqName);
    }

    @Nullable
    private LazyPackageDescriptor createPackage(FqName fqName) {
        if (!fqName.isRoot() && getPackageFragment(fqName.parent()) == null) {
            return null;
        }
        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName);
        if (provider == null) {
            return null;
        }
        return new LazyPackageDescriptor(module, fqName, this, provider);
    }

    @NotNull
    @Override
    public ModuleDescriptor getModuleDescriptor() {
        return module;
    }

    @NotNull
    @Override
    public LazyResolveStorageManager getStorageManager() {
        return storageManager;
    }

    @NotNull
    public ExceptionTracker getExceptionTracker() {
        return exceptionTracker;
    }

    @Override
    @NotNull
    @ReadOnly
    public Collection<ClassDescriptor> getTopLevelClassDescriptors(@NotNull FqName fqName) {
        if (fqName.isRoot()) return Collections.emptyList();

        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName.parent());
        if (provider == null) return Collections.emptyList();

        return ContainerUtil.mapNotNull(
                provider.getClassOrObjectDeclarations(fqName.shortName()),
                new Function<JetClassLikeInfo, ClassDescriptor>() {
                    @Override
                    public ClassDescriptor fun(JetClassLikeInfo classLikeInfo) {
                        if (classLikeInfo instanceof JetScriptInfo) {
                            return getClassDescriptorForScript(((JetScriptInfo) classLikeInfo).getScript());
                        }
                        JetClassOrObject classOrObject = classLikeInfo.getCorrespondingClassOrObject();
                        if (classOrObject == null) return null;
                        return getClassDescriptor(classOrObject);
                    }
                }
        );
    }

    @Override
    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject) {
        return lazyDeclarationResolver.getClassDescriptor(classOrObject);
    }

    @NotNull
    public ClassDescriptor getClassDescriptorForScript(@NotNull JetScript script) {
        JetScope resolutionScope = lazyDeclarationResolver.resolutionScopeToResolveDeclaration(script);
        FqName fqName = ScriptNameUtil.classNameForScript(script);
        ClassifierDescriptor classifier = resolutionScope.getClassifier(fqName.shortName());
        assert classifier != null : "No descriptor for " + fqName + " in file " + script.getContainingFile();
        return (ClassDescriptor) classifier;
    }

    @Override
    @NotNull
    public ScriptDescriptor getScriptDescriptor(@NotNull JetScript script) {
        return scriptDescriptors.invoke(script);
    }

    @Override
    @NotNull
    public BindingContext getBindingContext() {
        return trace.getBindingContext();
    }

    @Override
    @NotNull
    public BindingTrace getTrace() {
        return trace;
    }

    @Override
    @NotNull
    public DeclarationProviderFactory getDeclarationProviderFactory() {
        return declarationProviderFactory;
    }

    @Override
    @NotNull
    public DeclarationDescriptor resolveToDescriptor(@NotNull JetDeclaration declaration) {
        return lazyDeclarationResolver.resolveToDescriptor(declaration);
    }

    @NotNull
    public Annotations getFileAnnotations(@NotNull JetFile file) {
        return fileAnnotations.invoke(file);
    }

    @NotNull
    public Annotations getDanglingAnnotations(@NotNull JetFile file) {
        return danglingAnnotations.invoke(file);
    }

    @NotNull
    private List<LazyPackageDescriptor> getAllPackages() {
        LazyPackageDescriptor rootPackage = getPackageFragment(FqName.ROOT);
        assert rootPackage != null : "Root package must be initialized";

        return collectAllPackages(Lists.<LazyPackageDescriptor>newArrayList(), rootPackage);
    }

    @NotNull
    private List<LazyPackageDescriptor> collectAllPackages(
            @NotNull List<LazyPackageDescriptor> result,
            @NotNull LazyPackageDescriptor current
    ) {
        result.add(current);
        for (FqName subPackage : packageFragmentProvider.getSubPackagesOf(current.getFqName(), JetScope.ALL_NAME_FILTER)) {
            LazyPackageDescriptor fragment = getPackageFragment(subPackage);
            assert fragment != null : "Couldn't find fragment for " + subPackage;
            collectAllPackages(result, fragment);
        }
        return result;
    }

    @Override
    public void forceResolveAll() {
        for (LazyPackageDescriptor lazyPackage : getAllPackages()) {
            ForceResolveUtil.forceResolveAllContents(lazyPackage);
        }
    }

    @Override
    @NotNull
    public ScopeProvider getScopeProvider() {
        return scopeProvider;
    }

    @NotNull
    public JetImportsFactory getJetImportsFactory() {
        return jetImportFactory;
    }

    @Override
    @NotNull
    public AnnotationResolver getAnnotationResolver() {
        return annotationResolve;
    }

    @Override
    @NotNull
    public DescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    @Override
    @NotNull
    public TypeResolver getTypeResolver() {
        return typeResolver;
    }

    @NotNull
    public QualifiedExpressionResolver getQualifiedExpressionResolver() {
        return qualifiedExpressionResolver;
    }

    @NotNull
    @Override
    public FunctionDescriptorResolver getFunctionDescriptorResolver() {
        return functionDescriptorResolver;
    }
}
