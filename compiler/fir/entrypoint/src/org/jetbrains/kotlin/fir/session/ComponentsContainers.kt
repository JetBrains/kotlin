/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNameConflictsTracker
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirThreadUnsafeCachesFactory
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderImpl
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.FirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.java.FirJavaVisibilityChecker
import org.jetbrains.kotlin.fir.java.enhancement.FirJsr305StateContainer
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticNamesProvider
import org.jetbrains.kotlin.fir.resolve.calls.jvm.JvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirQualifierResolverImpl
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeResolverImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseCheckingPhaseManager
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseManager
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.GeneratedClassIndex
import org.jetbrains.kotlin.fir.scopes.impl.FirDeclaredMemberScopeProvider
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache
import org.jetbrains.kotlin.incremental.components.LookupTracker

// -------------------------- Required components --------------------------

@OptIn(SessionConfiguration::class)
fun FirSession.registerCommonComponents(languageVersionSettings: LanguageVersionSettings) {
    register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(languageVersionSettings))
    register(InferenceComponents::class, InferenceComponents(this))

    register(FirDeclaredMemberScopeProvider::class, FirDeclaredMemberScopeProvider())
    register(FirCorrespondingSupertypesCache::class, FirCorrespondingSupertypesCache(this))
    register(FirDefaultParametersResolver::class, FirDefaultParametersResolver())

    register(FirExtensionService::class, FirExtensionService(this))
    register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.create(this))
    register(FirPredicateBasedProvider::class, FirPredicateBasedProvider.create(this))
    register(GeneratedClassIndex::class, GeneratedClassIndex.create())
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerThreadUnsafeCaches() {
    register(FirCachesFactory::class, FirThreadUnsafeCachesFactory)
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerSealedClassInheritorsProvider() {
    register(SealedClassInheritorsProvider::class, SealedClassInheritorsProviderImpl)
}

@OptIn(SessionConfiguration::class)
fun FirSession.registerFirCliPhaseManager() {
    register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
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
    if (lookupTracker != null) {
        val firFileToPath: (FirSourceElement) -> String = {
            val psiSource = (it as? FirPsiSourceElement<*>) ?: TODO("Not implemented for non-FirPsiSourceElement")
            ((psiSource.psi as? PsiFile) ?: psiSource.psi.containingFile).virtualFile.path
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
    register(FirModuleVisibilityChecker::class, FirJvmModuleVisibilityChecker(this))
    register(ConeCallConflictResolverFactory::class, JvmCallConflictResolverFactory)
    register(FirPlatformClassMapper::class, FirJavaClassMapper(this))
    register(FirSyntheticNamesProvider::class, FirJavaSyntheticNamesProvider)
    register(FirJsr305StateContainer::class, FirJsr305StateContainer.Default)
}
