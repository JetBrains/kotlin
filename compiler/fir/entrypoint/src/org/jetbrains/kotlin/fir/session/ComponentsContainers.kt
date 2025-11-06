/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.toKotlinVersion
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.analysis.checkers.FirInlineCheckerPlatformSpecificComponent
import org.jetbrains.kotlin.fir.analysis.checkers.FirPrimaryConstructorSuperTypeCheckerPlatformComponent
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNameConflictsTrackerImpl
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirGenericArrayClassLiteralSupport
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirComposedDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.diagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.jvm.FirJvmOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.FirJavaNullabilityWarningUpperBoundsProvider
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.FirJvmAnnotationsPlatformSpecificSupportComponent
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.FirJvmInlineCheckerComponent
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.FirJvmPrimaryConstructorSuperTypeCheckerPlatformComponent
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirThreadUnsafeCachesFactory
import org.jetbrains.kotlin.fir.cli.CliDiagnostics
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.java.FirJavaVisibilityChecker
import org.jetbrains.kotlin.fir.java.FirSyntheticPropertiesStorage
import org.jetbrains.kotlin.fir.java.JvmSupertypeUpdater
import org.jetbrains.kotlin.fir.java.deserialization.FirJvmDeserializationExtension
import org.jetbrains.kotlin.fir.java.enhancement.FirAnnotationTypeQualifierResolver
import org.jetbrains.kotlin.fir.java.enhancement.FirEnhancedSymbolsStorage
import org.jetbrains.kotlin.fir.java.scopes.FirRenamedForOverrideSymbolsStorage
import org.jetbrains.kotlin.fir.java.scopes.JavaOverridabilityRules
import org.jetbrains.kotlin.fir.modules.FirJavaModuleResolverProvider
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticNamesProvider
import org.jetbrains.kotlin.fir.resolve.calls.jvm.JvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.overloads.FirDeclarationOverloadabilityHelperImpl
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirQualifierResolverImpl
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeResolverImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirCliJumpingPhaseComputationSessionForLocalClassesProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirDummyCompilerLazyDeclarationResolver
import org.jetbrains.kotlin.fir.resolve.transformers.FirJumpingPhaseComputationSessionForLocalClassesProvider
import org.jetbrains.kotlin.fir.resolve.transformers.PlatformSupertypeUpdater
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatchingContextImpl
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportsProviderHolder
import org.jetbrains.kotlin.fir.scopes.FirLookupDefaultStarImportsInSourcesSettingHolder
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirOverrideService
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.jvm.FirJvmDelegatedMembersFilter
import org.jetbrains.kotlin.fir.scopes.jvm.JvmMappedScope.FirMappedSymbolStorage
import org.jetbrains.kotlin.fir.serialization.FirProvidedDeclarationsForMetadataService
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.JavaAnnotationProvider
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.resolve.jvm.JvmConstants
import org.jetbrains.kotlin.resolve.jvm.JvmTypeSpecificityComparator
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

// -------------------------- Required components --------------------------

@OptIn(SessionConfiguration::class)
fun FirSession.registerCommonComponents(languageVersionSettings: LanguageVersionSettings, isMetadataCompilation: Boolean) {
    register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(languageVersionSettings, isMetadataCompilation))
    register(TypeComponents::class, TypeComponents(this))
    register(InferenceComponents::class, InferenceComponents(this))

    register(FirDeclaredMemberScopeProvider::class, FirDeclaredMemberScopeProvider(this))
    register(FirCorrespondingSupertypesCache::class, FirCorrespondingSupertypesCache(this))
    register(FirDefaultParametersResolver::class, FirDefaultParametersResolver())

    register(FirExtensionService::class, FirExtensionService(this))

    register(FirSubstitutionOverrideStorage::class, FirSubstitutionOverrideStorage(this))
    register(FirIntersectionOverrideStorage::class, FirIntersectionOverrideStorage(this))
    register(FirTypealiasConstructorStorage::class, FirTypealiasConstructorStorage(this))
    register(FirSynthesizedStorage::class, FirSynthesizedStorage(this))
    register(FirGeneratedMemberDeclarationsStorage::class, FirGeneratedMemberDeclarationsStorage(this))
    register(FirSamConstructorStorage::class, FirSamConstructorStorage(this))
    register(FirOverrideService::class, FirOverrideService(this))
    register(FirDynamicMembersStorage::class, FirDynamicMembersStorage(this))
    register(FirOverrideChecker::class, FirStandardOverrideChecker(this))
    register(FirDeclarationOverloadabilityHelper::class, FirDeclarationOverloadabilityHelperImpl(this))
    register(FirAnnotationsPlatformSpecificSupportComponent.Default)
    register(FirPrimaryConstructorSuperTypeCheckerPlatformComponent.Default)
    register(FirGenericArrayClassLiteralSupport::class, FirGenericArrayClassLiteralSupport.Disabled)
    register(FirMissingDependencyStorage::class, FirMissingDependencyStorage(this))
    register(FirPlatformSpecificCastChecker::class, FirPlatformSpecificCastChecker.Default)
    register(FirComposedDiagnosticRendererFactory::class, FirComposedDiagnosticRendererFactory())
    register(FirMustUseReturnValueStatusComponent::class, FirMustUseReturnValueStatusComponent.create(languageVersionSettings))
    register(FirInlineCheckerPlatformSpecificComponent::class, FirInlineCheckerPlatformSpecificComponent.NonJvmDefault)
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerCommonComponentsAfterExtensionsAreConfigured() {
    register(FirFunctionTypeKindService::class, FirFunctionTypeKindServiceImpl(this))
    register(FirProvidedDeclarationsForMetadataService::class, FirProvidedDeclarationsForMetadataService.create(this))
}

val firCachesFactoryForCliMode: FirCachesFactory
    get() = FirThreadUnsafeCachesFactory

@OptIn(SessionConfiguration::class)
fun FirSession.registerCliCompilerAndCommonComponents(languageVersionSettings: LanguageVersionSettings, isMetadataCompilation: Boolean) {
    register(FirCachesFactory::class, firCachesFactoryForCliMode)

    registerCommonComponents(languageVersionSettings, isMetadataCompilation)

    diagnosticRendererFactory.registerFactories(listOf(CliDiagnostics.getRendererFactory()))

    register(SealedClassInheritorsProvider::class, SealedClassInheritorsProviderImpl)
    register(FirLazyDeclarationResolver::class, FirDummyCompilerLazyDeclarationResolver)
    register(FirExceptionHandler::class, FirCliExceptionHandler)
    register(
        FirLookupDefaultStarImportsInSourcesSettingHolder::class,
        FirLookupDefaultStarImportsInSourcesSettingHolder.createDefault(languageVersionSettings)
    )

    register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotationsImpl(this))
    register(FirPredicateBasedProvider::class, FirPredicateBasedProviderImpl(this))
    register(FirHiddenDeprecationProvider::class, FirHiddenDeprecationProvider(this))

    @OptIn(FirImplementationDetail::class)
    register(FirJumpingPhaseComputationSessionForLocalClassesProvider::class, FirCliJumpingPhaseComputationSessionForLocalClassesProvider)
}

class FirSharableJavaComponents(
    val enhancementStorage: FirEnhancedSymbolsStorage,
    val mappedStorage: FirMappedSymbolStorage,
    val renamedFunctionsStorage: FirRenamedForOverrideSymbolsStorage,
) {
    constructor(cachesFactory: FirCachesFactory) : this(
        FirEnhancedSymbolsStorage(cachesFactory),
        FirMappedSymbolStorage(cachesFactory),
        FirRenamedForOverrideSymbolsStorage(cachesFactory)
    )
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerJavaComponents(
    javaModuleResolver: JavaModuleResolver,
    predefinedComponents: FirSharableJavaComponents? = null,
    registerJvmDeserializationExtension: Boolean = true,
) {
    register(FirJavaModuleResolverProvider::class, FirJavaModuleResolverProvider(javaModuleResolver))
    val jsr305State =
        languageVersionSettings.getFlag(JvmAnalysisFlags.javaTypeEnhancementState)
            ?: JavaTypeEnhancementState.getDefault(languageVersionSettings.toKotlinVersion())
    register(
        FirAnnotationTypeQualifierResolver::class,
        FirAnnotationTypeQualifierResolver(this, jsr305State, javaModuleResolver)
    )
    register(FirEnhancedSymbolsStorage::class, predefinedComponents?.enhancementStorage ?: FirEnhancedSymbolsStorage(this))
    register(FirMappedSymbolStorage::class, predefinedComponents?.mappedStorage ?: FirMappedSymbolStorage(this))
    register(FirRenamedForOverrideSymbolsStorage::class, predefinedComponents?.renamedFunctionsStorage ?: FirRenamedForOverrideSymbolsStorage(this))
    register(FirSyntheticPropertiesStorage::class, FirSyntheticPropertiesStorage(this))
    register(PlatformSupertypeUpdater::class, JvmSupertypeUpdater(this))
    register(JavaOverridabilityRules(this))

    if (registerJvmDeserializationExtension) {
        register(FirJvmDeserializationExtension(this))
    }
    register(FirEnumEntriesSupport::class, FirJvmEnumEntriesSupport(this))
    register(FirAnnotationsPlatformSpecificSupportComponent::class, FirJvmAnnotationsPlatformSpecificSupportComponent)
    register(FirPrimaryConstructorSuperTypeCheckerPlatformComponent::class, FirJvmPrimaryConstructorSuperTypeCheckerPlatformComponent)

    register(FirJavaVisibilityChecker)
    register(JvmCallConflictResolverFactory)
    register(FirTypeSpecificityComparatorProvider.of(JvmTypeSpecificityComparator(typeContext)))
    register(FirJavaClassMapper(this))
    register(FirSyntheticNamesProvider::class, FirJavaSyntheticNamesProvider)
    register(FirJvmOverridesBackwardCompatibilityHelper)
    register(FirJvmInlineCheckerComponent())
    register(FirGenericArrayClassLiteralSupport::class, FirGenericArrayClassLiteralSupport.Enabled)
    register(FirJvmDelegatedMembersFilter(this))
    register(FirJavaNullabilityWarningUpperBoundsProvider(this))
    register(FirDefaultImportsProviderHolder.of(FirJvmDefaultImportsProvider))
    register(FirDeclarationNameInvalidCharsProvider::class, FirDeclarationNameInvalidCharsProvider.of(JvmConstants.INVALID_CHARS))
}

// -------------------------- Resolve components --------------------------

/*
 * Resolve components which are same on all platforms
 */
@OptIn(SessionConfiguration::class)
fun FirSession.registerResolveComponents(
    lookupTracker: LookupTracker? = null,
    enumWhenTracker: EnumWhenTracker? = null,
    importTracker: ImportTracker? = null,
    fileMappingTracker: ICFileMappingTracker? = null,
) {
    register(FirQualifierResolver::class, FirQualifierResolverImpl(this))
    register(FirTypeResolver::class, FirTypeResolverImpl(this))
    register(FirModuleVisibilityChecker::class, FirModuleVisibilityChecker.Standard(this))
    register(SourcesToPathsMapper::class, SourcesToPathsMapper())
    if (lookupTracker != null) {
        val firFileToPath: (KtSourceElement) -> String? = {
            sourcesToPathsMapper.getSourceFilePath(it)
        }
        register(
            FirLookupTrackerComponent::class,
            IncrementalPassThroughLookupTrackerComponent(this, lookupTracker, fileMappingTracker, firFileToPath)
        )
    }
    if (enumWhenTracker != null) {
        register(
            FirEnumWhenTrackerComponent::class,
            IncrementalPassThroughEnumWhenTrackerComponent(enumWhenTracker)
        )
    }
    if (importTracker != null) {
        register(
            FirImportTrackerComponent::class,
            IncrementalPassThroughImportTrackerComponent(importTracker)
        )
    }
    register(FirExpectActualMatchingContextFactory::class, FirExpectActualMatchingContextImpl.Factory)
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerCliCompilerOnlyResolveComponents() {
    register(FirNameConflictsTracker::class, FirNameConflictsTrackerImpl())

    // The Analysis API uses `LLCheckersFactory`.
    register(CheckersComponent::class, CheckersComponent())
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerModuleData(moduleData: FirModuleData) {
    register(FirModuleData::class, moduleData)
}
