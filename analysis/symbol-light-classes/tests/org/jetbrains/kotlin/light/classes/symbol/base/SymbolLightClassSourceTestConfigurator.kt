/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.platform.TargetPlatform

class SymbolLightClassSourceTestConfigurator(
    override val defaultTargetPlatform: TargetPlatform
) : AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)