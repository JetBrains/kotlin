/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import kotlin.annotations.jvm.ReadOnly;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.incremental.components.LookupLocation;
import org.jetbrains.kotlin.incremental.components.LookupTracker;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.checkers.PlatformDiagnosticSuppressor;
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotations;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationsContextImpl;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.storage.*;
import org.jetbrains.kotlin.types.WrappedTypeFactory;
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker;
import org.jetbrains.kotlin.utils.SmartList;

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

    private final CacheWithNotNullValues<FqName, LazyPackageDescriptor> packages;
    private final PackageFragmentProvider packageFragmentProvider;

    private final MemoizedFunctionToNotNull<KtFile, LazyAnnotations> fileAnnotations;
    private final MemoizedFunctionToNotNull<KtFile, LazyAnnotations> danglingAnnotations;

    private AnnotationResolver annotationResolver;
    private DescriptorResolver descriptorResolver;
    private FunctionDescriptorResolver functionDescriptorResolver;
    private TypeResolver typeResolver;
    private LazyDeclarationResolver lazyDeclarationResolver;
    private FileScopeProvider fileScopeProvider;
    private DeclarationScopeProvider declarationScopeProvider;
    private LookupTracker lookupTracker;
    private LocalDescriptorResolver localDescriptorResolver;
    private SupertypeLoopChecker supertypeLoopsResolver;
    private LanguageVersionSettings languageVersionSettings;
    private DelegationFilter delegationFilter;
    private WrappedTypeFactory wrappedTypeFactory;
    private PlatformDiagnosticSuppressor platformDiagnosticSuppressor;
    private SamConversionResolver samConversionResolver;

    private final SyntheticResolveExtension syntheticResolveExtension;

    private final NewKotlinTypeChecker kotlinTypeChecker;

    private Project project;

    @Inject
    public void setAnnotationResolve(AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
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
    public void setLazyDeclarationResolver(LazyDeclarationResolver lazyDeclarationResolver) {
        this.lazyDeclarationResolver = lazyDeclarationResolver;
    }

    @Inject
    public void setFileScopeProvider(@NotNull FileScopeProvider fileScopeProvider) {
        this.fileScopeProvider = fileScopeProvider;
    }

    @Inject
    public void setDeclarationScopeProvider(@NotNull DeclarationScopeProviderImpl declarationScopeProvider) {
        this.declarationScopeProvider = declarationScopeProvider;
    }

    @Inject
    public void setLookupTracker(@NotNull LookupTracker lookupTracker) {
        this.lookupTracker = lookupTracker;
    }

    @Inject
    public void setLanguageVersionSettings(@NotNull LanguageVersionSettings languageVersionSettings) {
        this.languageVersionSettings = languageVersionSettings;
    }

    @Inject
    public void setDelegationFilter(@NotNull DelegationFilter delegationFilter) {
        this.delegationFilter = delegationFilter;
    }

    @Inject
    public void setWrappedTypeFactory(@NotNull WrappedTypeFactory wrappedTypeFactory) {
        this.wrappedTypeFactory = wrappedTypeFactory;
    }

    @Inject
    public void setPlatformDiagnosticSuppressor(@NotNull PlatformDiagnosticSuppressor platformDiagnosticSuppressor) {
        this.platformDiagnosticSuppressor = platformDiagnosticSuppressor;
    }

    @Inject
    public void setSamConversionResolver(@NotNull SamConversionResolver samConversionResolver) {
        this.samConversionResolver = samConversionResolver;
    }

    // Only calls from injectors expected
    @Deprecated
    public ResolveSession(
            @NotNull Project project,
            @NotNull GlobalContext globalContext,
            @NotNull ModuleDescriptor rootDescriptor,
            @NotNull DeclarationProviderFactory declarationProviderFactory,
            @NotNull BindingTrace delegationTrace,
            @NotNull NewKotlinTypeChecker kotlinTypeChecker
    ) {
        LockBasedLazyResolveStorageManager lockBasedLazyResolveStorageManager =
                new LockBasedLazyResolveStorageManager(globalContext.getStorageManager());

        this.storageManager = lockBasedLazyResolveStorageManager;
        this.exceptionTracker = globalContext.getExceptionTracker();
        this.trace = lockBasedLazyResolveStorageManager.createSafeTrace(delegationTrace);
        this.module = rootDescriptor;

        this.packages = storageManager.createCacheWithNotNullValues();

        this.declarationProviderFactory = declarationProviderFactory;

        this.packageFragmentProvider = new PackageFragmentProvider() {
            @Override
            public void collectPackageFragments(
                    @NotNull FqName fqName, @NotNull Collection<PackageFragmentDescriptor> packageFragments
            ) {
                LazyPackageDescriptor fragment = getPackageFragment(fqName);
                if (fragment != null) {
                    packageFragments.add(fragment);
                }
            }

            @NotNull
            @Override
            public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
                return ContainerUtil.createMaybeSingletonList(getPackageFragment(fqName));
            }

            @NotNull
            @Override
            public Collection<FqName> getSubPackagesOf(
                    @NotNull FqName fqName, @NotNull Function1<? super Name, Boolean> nameFilter
            ) {
                LazyPackageDescriptor packageDescriptor = getPackageFragment(fqName);
                if (packageDescriptor == null) {
                    return Collections.emptyList();
                }
                return packageDescriptor.getDeclarationProvider().getAllDeclaredSubPackages(nameFilter);
            }
        };

        fileAnnotations = storageManager.createMemoizedFunction(file -> createAnnotations(file, file.getAnnotationEntries()));

        danglingAnnotations = storageManager.createMemoizedFunction(file -> createAnnotations(file, file.getDanglingAnnotations()));

        syntheticResolveExtension = SyntheticResolveExtension.Companion.getInstance(project);

        this.project = project;
        this.kotlinTypeChecker = kotlinTypeChecker;
    }

    private LazyAnnotations createAnnotations(KtFile file, List<KtAnnotationEntry> annotationEntries) {
        LexicalScope scope = fileScopeProvider.getFileResolutionScope(file);
        LazyAnnotationsContextImpl lazyAnnotationContext =
                new LazyAnnotationsContextImpl(annotationResolver, storageManager, trace, scope);
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
        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName);
        if (provider == null) {
            return null;
        }

        return packages.computeIfAbsent(
                fqName,
                () -> new LazyPackageDescriptor(module, fqName, this, provider)
        );
    }


    @NotNull
    @Override
    public LazyPackageDescriptor getPackageFragmentOrDiagnoseFailure(@NotNull FqName fqName, @Nullable KtFile from) {
        LazyPackageDescriptor packageDescriptor = getPackageFragment(fqName);
        if (packageDescriptor == null) {
            declarationProviderFactory.diagnoseMissingPackageFragment(fqName, from);
            assert false : "diagnoseMissingPackageFragment should throw!";
        }
        return packageDescriptor;
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
    public Collection<ClassifierDescriptor> getTopLevelClassifierDescriptors(@NotNull FqName fqName, @NotNull LookupLocation location) {
        if (fqName.isRoot()) return Collections.emptyList();

        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName.parent());
        if (provider == null) return Collections.emptyList();

        Collection<ClassifierDescriptor> result = new SmartList<>();

        result.addAll(ContainerUtil.mapNotNull(
                provider.getClassOrObjectDeclarations(fqName.shortName()),
                classOrObjectInfo -> getClassDescriptor(classOrObjectInfo.getCorrespondingClassOrObject(), location)
        ));

        result.addAll(ContainerUtil.mapNotNull(
                provider.getScriptDeclarations(fqName.shortName()),
                scriptInfo -> getScriptDescriptor(scriptInfo.getScript())
        ));

        result.addAll(ContainerUtil.map(
                provider.getTypeAliasDeclarations(fqName.shortName()),
                alias -> (ClassifierDescriptor) lazyDeclarationResolver.resolveToDescriptor(alias)
        ));

        return result;
    }

    @Override
    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull KtClassOrObject classOrObject, @NotNull LookupLocation location) {
        return lazyDeclarationResolver.getClassDescriptor(classOrObject, location);
    }

    @NotNull
    public ClassDescriptorWithResolutionScopes getScriptDescriptor(@NotNull KtScript script) {
        return lazyDeclarationResolver.getScriptDescriptor(script, NoLookupLocation.FOR_SCRIPT);
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
    public DeclarationDescriptor resolveToDescriptor(@NotNull KtDeclaration declaration) {
        assertValid();
        if (!areDescriptorsCreatedForDeclaration(declaration)) {
            throw new IllegalStateException(
                    "No descriptors are created for declarations of type " + declaration.getClass().getSimpleName()
                    + "\n. Change the caller accordingly"
            );
        }
        final boolean isLocal = ReadAction.compute(() -> KtPsiUtil.isLocal(declaration));
        if (!isLocal){
            return lazyDeclarationResolver.resolveToDescriptor(declaration);
        }
        return localDescriptorResolver.resolveLocalDeclaration(declaration);
    }

    public static boolean areDescriptorsCreatedForDeclaration(@NotNull KtDeclaration declaration) {
        return !(declaration instanceof KtAnonymousInitializer || declaration instanceof KtDestructuringDeclaration);
    }

    @NotNull
    public Annotations getFileAnnotations(@NotNull KtFile file) {
        return fileAnnotations.invoke(file);
    }

    @NotNull
    public Annotations getDanglingAnnotations(@NotNull KtFile file) {
        return danglingAnnotations.invoke(file);
    }

    @NotNull
    private List<LazyPackageDescriptor> getAllPackages() {
        LazyPackageDescriptor rootPackage = getPackageFragment(FqName.ROOT);
        assert rootPackage != null : "Root package must be initialized";

        return collectAllPackages(Lists.newArrayList(), rootPackage);
    }

    @NotNull
    private List<LazyPackageDescriptor> collectAllPackages(
            @NotNull List<LazyPackageDescriptor> result,
            @NotNull LazyPackageDescriptor current
    ) {
        result.add(current);
        for (FqName subPackage : packageFragmentProvider.getSubPackagesOf(current.getFqName(), MemberScope.Companion.getALL_NAME_FILTER())) {
            LazyPackageDescriptor fragment = getPackageFragmentOrDiagnoseFailure(subPackage, null);
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
    public AnnotationResolver getAnnotationResolver() {
        return annotationResolver;
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
    @Override
    public FunctionDescriptorResolver getFunctionDescriptorResolver() {
        return functionDescriptorResolver;
    }

    @NotNull
    @Override
    public DeclarationScopeProvider getDeclarationScopeProvider() {
        return declarationScopeProvider;
    }

    @Override
    @NotNull
    public FileScopeProvider getFileScopeProvider() {
        return fileScopeProvider;
    }

    @NotNull
    @Override
    public LookupTracker getLookupTracker() {
        return lookupTracker;
    }

    @NotNull
    @Override
    public SupertypeLoopChecker getSupertypeLoopChecker() {
        return supertypeLoopsResolver;
    }

    @Inject
    public void setSupertypeLoopsResolver(@NotNull SupertypeLoopChecker supertypeLoopsResolver) {
        this.supertypeLoopsResolver = supertypeLoopsResolver;
    }

    @Inject
    public void setLocalDescriptorResolver(@NotNull LocalDescriptorResolver localDescriptorResolver) {
        this.localDescriptorResolver = localDescriptorResolver;
    }

    @NotNull
    @Override
    public LanguageVersionSettings getLanguageVersionSettings() {
        return languageVersionSettings;
    }

    @NotNull
    @Override
    public DelegationFilter getDelegationFilter() {
        return delegationFilter;
    }

    @NotNull
    @Override
    public SyntheticResolveExtension getSyntheticResolveExtension() {
        return syntheticResolveExtension;
    }

    @NotNull
    @Override
    public WrappedTypeFactory getWrappedTypeFactory() {
        return wrappedTypeFactory;
    }

    @NotNull
    public PlatformDiagnosticSuppressor getPlatformDiagnosticSuppressor() {
        return platformDiagnosticSuppressor;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @Override
    public void assertValid() {
        module.assertValid();
    }

    @NotNull
    @Override
    public NewKotlinTypeChecker getKotlinTypeChecker() {
        return kotlinTypeChecker;
    }

    @NotNull
    @Override
    public SamConversionResolver getSamConversionResolver() {
        return samConversionResolver;
    }
}
