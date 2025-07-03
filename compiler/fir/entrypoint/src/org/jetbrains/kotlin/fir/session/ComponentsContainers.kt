/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.jvmDefaultMode
import org.jetbrains.kotlin.config.toKotlinVersion
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.analysis.FirDefaultOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.analysis.FirOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.analysis.checkers.FirIdentityLessPlatformDeterminer
import org.jetbrains.kotlin.fir.analysis.checkers.FirInlineCheckerPlatformSpecificComponent
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformUpperBoundsProvider
import org.jetbrains.kotlin.fir.analysis.checkers.FirPrimaryConstructorSuperTypeCheckerPlatformComponent
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNameConflictsTracker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirGenericArrayClassLiteralSupport
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirComposedDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.jvm.FirJvmOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.FirJavaNullabilityWarningUpperBoundsProvider
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.FirJvmAnnotationsPlatformSpecificSupportComponent
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.FirJvmInlineCheckerComponent
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.FirJvmPrimaryConstructorSuperTypeCheckerPlatformComponent
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirThreadUnsafeCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationExtension
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.java.FirJavaVisibilityChecker
import org.jetbrains.kotlin.fir.java.FirJvmDefaultModeComponent
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
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.overloads.DefaultCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.overloads.FirDeclarationOverloadabilityHelperImpl
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirQualifierResolverImpl
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeResolverImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirCliJumpingPhaseComputationSessionForLocalClassesProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirDummyCompilerLazyDeclarationResolver
import org.jetbrains.kotlin.fir.resolve.transformers.FirJumpingPhaseComputationSessionForLocalClassesProvider
import org.jetbrains.kotlin.fir.resolve.transformers.PlatformSupertypeUpdater
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatchingContextImpl
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.jvm.FirJvmDelegatedMembersFilter
import org.jetbrains.kotlin.fir.scopes.jvm.JvmMappedScope.FirMappedSymbolStorage
import org.jetbrains.kotlin.fir.serialization.FirProvidedDeclarationsForMetadataService
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.resolve.jvm.JvmTypeSpecificityComparator
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

// -------------------------- Required components --------------------------

@OptIn(SessionConfiguration::class)
fun FirSession.registerCommonComponents(languageVersionSettings: LanguageVersionSettings) {
    register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(languageVersionSettings))
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
    register(FirEnumEntriesSupport::class, FirEnumEntriesSupport(this))
    register(FirOverrideChecker::class, FirStandardOverrideChecker(this))
    register(FirDeclarationOverloadabilityHelper::class, FirDeclarationOverloadabilityHelperImpl(this))
    register(FirAnnotationsPlatformSpecificSupportComponent::class, FirAnnotationsPlatformSpecificSupportComponent.Default)
    register(FirPrimaryConstructorSuperTypeCheckerPlatformComponent::class, FirPrimaryConstructorSuperTypeCheckerPlatformComponent.Default)
    register(FirGenericArrayClassLiteralSupport::class, FirGenericArrayClassLiteralSupport.Disabled)
    register(FirMissingDependencyStorage::class, FirMissingDependencyStorage(this))
    register(FirPlatformSpecificCastChecker::class, FirPlatformSpecificCastChecker.Default)
    register(FirComposedDiagnosticRendererFactory::class, FirComposedDiagnosticRendererFactory())
    register(FirMustUseReturnValueStatusComponent::class, FirMustUseReturnValueStatusComponent.create(languageVersionSettings))
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerCommonComponentsAfterExtensionsAreConfigured() {
    register(FirFunctionTypeKindService::class, FirFunctionTypeKindServiceImpl(this))
    register(FirProvidedDeclarationsForMetadataService::class, FirProvidedDeclarationsForMetadataService.create(this))
}

val firCachesFactoryForCliMode: FirCachesFactory
    get() = FirThreadUnsafeCachesFactory

@OptIn(SessionConfiguration::class)
fun FirSession.registerCliCompilerOnlyComponents(languageVersionSettings: LanguageVersionSettings) {
    register(FirCachesFactory::class, firCachesFactoryForCliMode)
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
    register(FirJvmDefaultModeComponent::class, FirJvmDefaultModeComponent(languageVersionSettings.jvmDefaultMode))
    register(PlatformSupertypeUpdater::class, JvmSupertypeUpdater(this))
    register(PlatformSpecificOverridabilityRules::class, JavaOverridabilityRules(this))
    register(FirDeserializationExtension::class, FirJvmDeserializationExtension(this))
    register(FirEnumEntriesSupport::class, FirJvmEnumEntriesSupport(this))
    register(FirAnnotationsPlatformSpecificSupportComponent::class, FirJvmAnnotationsPlatformSpecificSupportComponent)
    register(FirPrimaryConstructorSuperTypeCheckerPlatformComponent::class, FirJvmPrimaryConstructorSuperTypeCheckerPlatformComponent)

    register(FirVisibilityChecker::class, FirJavaVisibilityChecker)
    register(ConeCallConflictResolverFactory::class, JvmCallConflictResolverFactory)
    register(
        FirTypeSpecificityComparatorProvider::class,
        FirTypeSpecificityComparatorProvider(JvmTypeSpecificityComparator(typeContext))
    )
    register(FirPlatformClassMapper::class, FirJavaClassMapper(this))
    register(FirSyntheticNamesProvider::class, FirJavaSyntheticNamesProvider)
    register(FirOverridesBackwardCompatibilityHelper::class, FirJvmOverridesBackwardCompatibilityHelper)
    register(FirInlineCheckerPlatformSpecificComponent::class, FirJvmInlineCheckerComponent())
    register(FirGenericArrayClassLiteralSupport::class, FirGenericArrayClassLiteralSupport.Enabled)
    register(FirDelegatedMembersFilter::class, FirJvmDelegatedMembersFilter(this))
    register(FirPlatformUpperBoundsProvider::class, FirJavaNullabilityWarningUpperBoundsProvider(this))
    register(FirDefaultImportProviderHolder::class, FirDefaultImportProviderHolder(FirJvmDefaultImportProvider))
}

/**
 * Registers default components for [FirSession]
 * They could be overridden by calling a function that registers specific platform components
 */
@OptIn(SessionConfiguration::class)
fun FirSession.registerDefaultComponents() {
    register(FirVisibilityChecker::class, FirVisibilityChecker.Default)
    register(ConeCallConflictResolverFactory::class, DefaultCallConflictResolverFactory)
    register(FirPlatformClassMapper::class, FirPlatformClassMapper.Default)
    register(FirOverridesBackwardCompatibilityHelper::class, FirDefaultOverridesBackwardCompatibilityHelper)
    register(FirDelegatedMembersFilter::class, FirDelegatedMembersFilter.Default)
    register(FirPlatformSpecificCastChecker::class, FirPlatformSpecificCastChecker.Default)
    register(FirDefaultImportProviderHolder::class, FirDefaultImportProviderHolder(CommonPlatformAnalyzerServices))
    register(FirIdentityLessPlatformDeterminer::class, FirIdentityLessPlatformDeterminer.Default)
}

// -------------------------- Resolve components --------------------------

/*
 * Resolve components which are same on all platforms
 */
@OptIn(SessionConfiguration::class)
fun FirSession.registerResolveComponents(lookupTracker: LookupTracker? = null, enumWhenTracker: EnumWhenTracker? = null, importTracker: ImportTracker? = null) {
    register(FirQualifierResolver::class, FirQualifierResolverImpl(this))
    register(FirTypeResolver::class, FirTypeResolverImpl(this))
    register(CheckersComponent::class, CheckersComponent())
    register(FirNameConflictsTrackerComponent::class, FirNameConflictsTracker())
    register(FirModuleVisibilityChecker::class, FirModuleVisibilityChecker.Standard(this))
    register(SourcesToPathsMapper::class, SourcesToPathsMapper())
    if (lookupTracker != null) {
        val firFileToPath: (KtSourceElement) -> String? = {
            sourcesToPathsMapper.getSourceFilePath(it)
        }
        register(
            FirLookupTrackerComponent::class,
            IncrementalPassThroughLookupTrackerComponent(lookupTracker, firFileToPath)
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
fun FirSession.registerModuleData(moduleData: FirModuleData) {
    register(FirModuleData::class, moduleData)
}
