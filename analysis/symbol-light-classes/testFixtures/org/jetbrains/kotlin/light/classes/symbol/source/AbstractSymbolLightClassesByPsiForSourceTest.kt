/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.source

import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesByPsiTest
import org.jetbrains.kotlin.light.classes.symbol.base.SymbolLightClassScriptTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.base.SymbolLightClassSourceTestConfigurator
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

abstract class AbstractSymbolLightClassesByPsiForSourceTest : AbstractSymbolLightClassesByPsiTest(
    configurator = SymbolLightClassSourceTestConfigurator(JvmPlatforms.defaultJvmPlatform),
    currentExtension = EXTENSIONS.JAVA,
    isTestAgainstCompiledCode = false,
)

abstract class AbstractScriptSymbolLightClassesByPsiForSourceTest : AbstractSymbolLightClassesByPsiTest(
    configurator = SymbolLightClassScriptTestConfigurator,
    currentExtension = EXTENSIONS.JAVA,
    isTestAgainstCompiledCode = false,
)

abstract class AbstractJsSymbolLightClassesByPsiForSourceTest : AbstractSymbolLightClassesByPsiTest(
    configurator = SymbolLightClassSourceTestConfigurator(JsPlatforms.defaultJsPlatform),
    currentExtension = EXTENSIONS.KMP_JAVA,
    isTestAgainstCompiledCode = false,
)