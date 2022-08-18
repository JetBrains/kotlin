/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.source

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesTest

abstract class AbstractSymbolLightClassesForSourceTest :
    AbstractSymbolLightClassesTest(
        AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false),
        EXTENSIONS.FIR_JAVA,
        stopIfCompilationErrorDirectivePresent = false
    )