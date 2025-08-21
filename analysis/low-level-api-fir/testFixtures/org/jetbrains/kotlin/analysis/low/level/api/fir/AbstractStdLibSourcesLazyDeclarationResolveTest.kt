/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirStdlibSourceTestConfigurator
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance

abstract class AbstractStdLibSourcesLazyDeclarationResolveTest : AbstractByQualifiedNameLazyDeclarationResolveTest() {
    override fun checkResolutionFacade(resolutionFacade: LLResolutionFacade) {
        requireIsInstance<KaLibrarySourceModule>(resolutionFacade.useSiteModule)
    }

    override val configurator get() = AnalysisApiFirStdlibSourceTestConfigurator

}