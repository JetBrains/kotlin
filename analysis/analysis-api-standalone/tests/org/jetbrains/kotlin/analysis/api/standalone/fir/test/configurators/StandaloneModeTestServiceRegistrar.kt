/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.LLFirStandaloneLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPsiDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPsiDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

@OptIn(KtAnalysisApiInternals::class)
public object StandaloneModeTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider::class.java)
            registerService(LLFirLibrarySymbolProviderFactory::class.java, LLFirStandaloneLibrarySymbolProviderFactory::class.java)
        }
    }

    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(
                KotlinPsiDeclarationProviderFactory::class.java,
                KotlinStaticPsiDeclarationProviderFactory::class.java
            )
        }
    }
}
