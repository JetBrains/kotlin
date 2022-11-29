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

package org.jetbrains.kotlin.frontend.di

import org.jetbrains.kotlin.cfg.ControlFlowInformationProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.container.useInstanceIfNotNull
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.TypeAttributeTranslatorExtension
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.components.ClassicTypeSystemContextForCS
import org.jetbrains.kotlin.resolve.calls.inference.components.ClassicConstraintSystemUtilContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.resolve.calls.tower.KotlinResolutionStatelessCallbacksImpl
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.OptInUsageChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.isTypeRefinementEnabled
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.optimization.OptimizingOptions
import org.jetbrains.kotlin.types.KotlinTypeRefinerImpl
import org.jetbrains.kotlin.types.checker.KotlinTypePreparator
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.NewKotlinTypeCheckerImpl
import org.jetbrains.kotlin.types.expressions.DeclarationScopeProviderForLocalClassifierAnalyzer
import org.jetbrains.kotlin.types.expressions.LocalClassDescriptorHolder
import org.jetbrains.kotlin.types.expressions.LocalLazyDeclarationResolver
import org.jetbrains.kotlin.util.ProgressManagerBasedCancellationChecker

fun StorageComponentContainer.configureModule(
    moduleContext: ModuleContext,
    platform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    trace: BindingTrace,
    languageVersionSettings: LanguageVersionSettings,
    sealedProvider: SealedClassInheritorsProvider = CliSealedClassInheritorsProvider,
    optimizingOptions: OptimizingOptions? = null,
) {
    useInstance(sealedProvider)
    useInstance(moduleContext)
    useInstance(moduleContext.module)
    useInstance(moduleContext.project)
    useInstance(moduleContext.storageManager)
    useInstance(moduleContext.module.builtIns)
    useInstance(trace)
    useInstance(languageVersionSettings)

    useInstanceIfNotNull(optimizingOptions)

    useInstance(platform)
    useInstance(analyzerServices)

    val nonTrivialPlatformVersion = platform
        .mapNotNull { it.targetPlatformVersion.takeIf { it != TargetPlatformVersion.NoVersion } }
        .singleOrNull()

    useInstance(nonTrivialPlatformVersion ?: TargetPlatformVersion.NoVersion)

    analyzerServices.platformConfigurator.configureModuleComponents(this)
    analyzerServices.platformConfigurator.configureModuleDependentCheckers(this)

    useInstance(TypeAttributeTranslatorExtension.createTranslators(moduleContext.project))

    for (extension in StorageComponentContainerContributor.getInstances(moduleContext.project)) {
        extension.registerModuleComponents(this, platform, moduleContext.module)
    }

    useImpl<NewKotlinTypeCheckerImpl>()

    if (moduleContext.module.isTypeRefinementEnabled()) {
        useImpl<KotlinTypeRefinerImpl>()
    } else {
        useInstance(KotlinTypeRefiner.Default)
    }

    useInstance(KotlinTypePreparator.Default)

    configurePlatformIndependentComponents()
}

private fun StorageComponentContainer.configurePlatformIndependentComponents() {
    useImpl<SupertypeLoopCheckerImpl>()
    useImpl<KotlinResolutionStatelessCallbacksImpl>()
    useImpl<DataFlowValueFactoryImpl>()

    useImpl<OptInUsageChecker>()
    useImpl<OptInUsageChecker.Overrides>()
    useImpl<OptInUsageChecker.ClassifierUsage>()

    useImpl<ContractDeserializerImpl>()
    useImpl<CompilerDeserializationConfiguration>()

    useImpl<ClassicTypeSystemContextForCS>()
    useImpl<ClassicConstraintSystemUtilContext>()
    useInstance(ProgressManagerBasedCancellationChecker)
}

/**
 * Actually, those should be present in 'configurePlatformIndependentComponents',
 * but, unfortunately, this is currently impossible, because in some lightweight
 * containers (see [createContainerForBodyResolve] and similar) some dependencies
 * are missing
 *
 * If you're not doing some trickery with containers, you should use them.
 */
fun StorageComponentContainer.configureStandardResolveComponents() {
    useImpl<ResolveSession>()
    useImpl<LazyTopDownAnalyzer>()
    useImpl<AnnotationResolverImpl>()
}

fun StorageComponentContainer.configureIncrementalCompilation(
    lookupTracker: LookupTracker,
    expectActualTracker: ExpectActualTracker,
    inlineConstTracker: InlineConstTracker,
    enumWhenTracker: EnumWhenTracker
) {
    useInstance(lookupTracker)
    useInstance(expectActualTracker)
    useInstance(inlineConstTracker)
    useInstance(enumWhenTracker)
}

fun createContainerForBodyResolve(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    platform: TargetPlatform,
    statementFilter: StatementFilter,
    analyzerServices: PlatformDependentAnalyzerServices,
    languageVersionSettings: LanguageVersionSettings,
    moduleStructureOracle: ModuleStructureOracle,
    sealedProvider: SealedClassInheritorsProvider,
    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
): StorageComponentContainer = createContainer("BodyResolve", analyzerServices) {
    configureModule(moduleContext, platform, analyzerServices, bindingTrace, languageVersionSettings, sealedProvider)

    useInstance(statementFilter)

    useInstance(BodyResolveCache.ThrowException)
    useImpl<AnnotationResolverImpl>()

    useImpl<BodyResolver>()
    useInstance(moduleStructureOracle)
    useInstance(controlFlowInformationProviderFactory)
    useInstance(InlineConstTracker.DoNothing)
}

fun createContainerForLazyBodyResolve(
    moduleContext: ModuleContext,
    kotlinCodeAnalyzer: KotlinCodeAnalyzer,
    bindingTrace: BindingTrace,
    platform: TargetPlatform,
    bodyResolveCache: BodyResolveCache,
    analyzerServices: PlatformDependentAnalyzerServices,
    languageVersionSettings: LanguageVersionSettings,
    moduleStructureOracle: ModuleStructureOracle,
    mainFunctionDetectorFactory: MainFunctionDetector.Factory,
    sealedProvider: SealedClassInheritorsProvider,
    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
    optimizingOptions: OptimizingOptions? = null
): StorageComponentContainer = createContainer("LazyBodyResolve", analyzerServices) {
    configureModule(moduleContext, platform, analyzerServices, bindingTrace, languageVersionSettings, sealedProvider, optimizingOptions)
    useInstance(mainFunctionDetectorFactory)
    useInstance(kotlinCodeAnalyzer)
    useInstance(kotlinCodeAnalyzer.fileScopeProvider)
    useInstance(bodyResolveCache)
    useImpl<AnnotationResolverImpl>()
    useImpl<LazyTopDownAnalyzer>()
    useImpl<BasicAbsentDescriptorHandler>()
    useInstance(moduleStructureOracle)
    useInstance(controlFlowInformationProviderFactory)
    useInstance(InlineConstTracker.DoNothing)

    // All containers except common inject ExpectedActualDeclarationChecker, so for common we do that
    // explicitly.
    // Note that it is not possible to move this code to [CommonPlatformConfigurator], because during
    // compilation of common-module to metadata we should skip those checks
    if (platform.isCommon()) useImpl<ExpectedActualDeclarationChecker>()
}

fun createContainerForLazyLocalClassifierAnalyzer(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    platform: TargetPlatform,
    lookupTracker: LookupTracker,
    languageVersionSettings: LanguageVersionSettings,
    statementFilter: StatementFilter,
    localClassDescriptorHolder: LocalClassDescriptorHolder,
    analyzerServices: PlatformDependentAnalyzerServices,
    controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
): StorageComponentContainer = createContainer("LocalClassifierAnalyzer", analyzerServices) {
    configureModule(moduleContext, platform, analyzerServices, bindingTrace, languageVersionSettings)

    useInstance(localClassDescriptorHolder)
    useInstance(lookupTracker)
    useInstance(ExpectActualTracker.DoNothing)
    useInstance(InlineConstTracker.DoNothing)
    useInstance(EnumWhenTracker.DoNothing)

    useImpl<LazyTopDownAnalyzer>()

    useInstance(NoTopLevelDescriptorProvider)

    TargetEnvironment.configureCompilerEnvironment(this)
    useInstance(controlFlowInformationProviderFactory)

    useInstance(FileScopeProvider.ThrowException)
    useImpl<AnnotationResolverImpl>()

    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()
    useImpl<LocalLazyDeclarationResolver>()


    useInstance(statementFilter)
}

fun createContainerForLazyResolve(
    moduleContext: ModuleContext,
    declarationProviderFactory: DeclarationProviderFactory,
    bindingTrace: BindingTrace,
    platform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    targetEnvironment: TargetEnvironment,
    languageVersionSettings: LanguageVersionSettings
): StorageComponentContainer = createContainer("LazyResolve", analyzerServices) {
    configureModule(moduleContext, platform, analyzerServices, bindingTrace, languageVersionSettings)

    configureStandardResolveComponents()

    useInstance(declarationProviderFactory)
    useInstance(InlineConstTracker.DoNothing)


    targetEnvironment.configure(this)

}
