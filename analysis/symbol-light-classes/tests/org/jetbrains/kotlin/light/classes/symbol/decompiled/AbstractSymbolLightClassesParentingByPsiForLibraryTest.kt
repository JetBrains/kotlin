/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesParentingTestByPsi
import org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators.AnalysisApiSymbolLightClassesDecompiledTestConfigurator
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

abstract class AbstractSymbolLightClassesParentingByPsiForLibraryTest :
    AbstractSymbolLightClassesParentingTestByPsi(
        AnalysisApiSymbolLightClassesDecompiledTestConfigurator(JvmPlatforms.defaultJvmPlatform),
        EXTENSIONS.LIB_JAVA,
        stopIfCompilationErrorDirectivePresent = true
    )
