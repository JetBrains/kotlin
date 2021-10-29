/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.analysis.FirOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNameConflictsTracker
import org.jetbrains.kotlin.fir.analysis.jvm.FirJvmOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirThreadUnsafeCachesFactory
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderImpl
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.java.FirJavaVisibilityChecker
import org.jetbrains.kotlin.fir.java.FirJvmDefaultModeComponent
import org.jetbrains.kotlin.fir.java.enhancement.FirAnnotationTypeQualifierResolver
import org.jetbrains.kotlin.fir.java.enhancement.FirEnhancedSymbolsStorage
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticNamesProvider
import org.jetbrains.kotlin.fir.resolve.calls.jvm.JvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirQualifierResolverImpl
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeResolverImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseCheckingPhaseManager
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.GeneratedClassIndex
import org.jetbrains.kotlin.fir.scopes.FirPlatformClassMapper
import org.jetbrains.kotlin.fir.scopes.impl.FirDeclaredMemberScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirIntersectionOverrideStorage
import org.jetbrains.kotlin.fir.scopes.impl.FirSubstitutionOverrideStorage
import org.jetbrains.kotlin.fir.symbols.FirPhaseManager
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache
import org.jetbrains.kotlin.fir.types.TypeComponents
import org.jetbrains.kotlin.incremental.components.LookupTracker
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
    register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotationsImpl(this))
    register(FirPredicateBasedProvider::class, FirPredicateBasedProviderImpl(this))
    register(GeneratedClassIndex::class, GeneratedClassIndex.create())

    register(FirSubstitutionOverrideStorage::class, FirSubstitutionOverrideStorage(this))
    register(FirIntersectionOverrideStorage::class, FirIntersectionOverrideStorage(this))
    register(FirSamConstructorStorage::class, FirSamConstructorStorage(this))
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerCliCompilerOnlyComponents() {
    register(FirCachesFactory::class, FirThreadUnsafeCachesFactory)
    register(SealedClassInheritorsProvider::class, SealedClassInheritorsProviderImpl)
    register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerCommonJavaComponents(javaModuleResolver: JavaModuleResolver) {
    val jsr305State = languageVersionSettings.getFlag(JvmAnalysisFlags.javaTypeEnhancementState)
    register(FirAnnotationTypeQualifierResolver::class, FirAnnotationTypeQualifierResolver(this, jsr305State, javaModuleResolver))
    register(FirEnhancedSymbolsStorage::class, FirEnhancedSymbolsStorage(this))
    register(
        FirJvmDefaultModeComponent::class,
        FirJvmDefaultModeComponent(languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode))
    )
}

// -------------------------- Resolve components --------------------------

/*
 * Resolve components which are same on all platforms
 */
@OptIn(SessionConfiguration::class)
fun FirSession.registerResolveComponents(lookupTracker: LookupTracker? = null) {
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
}

/*
 * Resolve components which have specific implementations on JVM
 */
@OptIn(SessionConfiguration::class)
fun FirSession.registerJavaSpecificResolveComponents() {
    register(FirVisibilityChecker::class, FirJavaVisibilityChecker)
    register(ConeCallConflictResolverFactory::class, JvmCallConflictResolverFactory)
    register(FirPlatformClassMapper::class, FirJavaClassMapper(this))
    register(FirSyntheticNamesProvider::class, FirJavaSyntheticNamesProvider)
    register(FirOverridesBackwardCompatibilityHelper::class, FirJvmOverridesBackwardCompatibilityHelper)
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerModuleData(moduleData: FirModuleData) {
    register(FirModuleData::class, moduleData)
}
