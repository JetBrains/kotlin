/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator

object AnalysisApiSymbolLibraryLightClassesDecompiledTestConfigurator : AbstractAnalysisApiSymbolLightClassesDecompiledTestConfigurator() {
    override fun analysisApiTestConfigurator(): AnalysisApiTestConfigurator = AnalysisApiFirLibraryBinaryTestConfigurator
}