/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.KaAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.LLFirStandaloneLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.services.LLStandaloneFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPsiDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPsiDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Registers services specific to Standalone mode *tests*, in addition to the Standalone production services registered by
 * [FirStandaloneServiceRegistrar][org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.FirStandaloneServiceRegistrar].
 */
@OptIn(KaAnalysisApiInternals::class)
object StandaloneModeTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KaLifetimeTokenProvider::class.java, KaAlwaysAccessibleLifetimeTokenProvider::class.java)
            registerService(LLFirLibrarySymbolProviderFactory::class.java, LLFirStandaloneLibrarySymbolProviderFactory::class.java)
            registerService(LLFirElementByPsiElementChooser::class.java, LLStandaloneFirElementByPsiElementChooser::class.java)
        }
    }

    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        project.apply {
            registerService(
                KotlinPsiDeclarationProviderFactory::class.java,
                KotlinStaticPsiDeclarationProviderFactory::class.java
            )
        }
    }
}
