/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaElementFinder
import org.jetbrains.kotlin.idea.fir.low.level.api.DeclarationProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.KtPackageProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtElement

abstract class FirModuleResolveStateConfigurator {
    abstract fun createPackagePartsProvider(scope: GlobalSearchScope): PackagePartProvider

    abstract fun createModuleDataProvider(moduleInfo: ModuleSourceInfoBase): ModuleDataProvider

    abstract fun getLanguageVersionSettings(moduleInfo: ModuleSourceInfoBase): LanguageVersionSettings
    abstract fun getModuleSourceScope(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope
    abstract fun createScopeForModuleLibraries(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope
    abstract fun createSealedInheritorsProvider(): SealedClassInheritorsProvider
    abstract fun getModuleInfoFor(element: KtElement): ModuleInfo

    abstract fun configureSourceSession(session: FirSession)
}

val Project.stateConfigurator: FirModuleResolveStateConfigurator
    get() = ServiceManager.getService(this, FirModuleResolveStateConfigurator::class.java)

internal fun KtElement.getModuleInfo(): ModuleInfo =
    project.stateConfigurator.getModuleInfoFor(this)