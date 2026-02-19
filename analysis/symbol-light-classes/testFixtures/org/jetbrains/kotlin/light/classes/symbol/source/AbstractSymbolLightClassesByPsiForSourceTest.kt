/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.source

import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesByPsiTest
import org.jetbrains.kotlin.light.classes.symbol.base.SymbolLightClassScriptTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.base.SymbolLightClassSourceJsTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.base.SymbolLightClassSourceJvmTestConfigurator

abstract class AbstractSymbolLightClassesByPsiForSourceTest : AbstractSymbolLightClassesByPsiTest(
    configurator = SymbolLightClassSourceJvmTestConfigurator,
    isTestAgainstCompiledCode = false,
)

abstract class AbstractScriptSymbolLightClassesByPsiForSourceTest : AbstractSymbolLightClassesByPsiTest(
    configurator = SymbolLightClassScriptTestConfigurator,
    isTestAgainstCompiledCode = false,
)

abstract class AbstractJsSymbolLightClassesByPsiForSourceTest : AbstractSymbolLightClassesByPsiTest(
    configurator = SymbolLightClassSourceJsTestConfigurator,
    isTestAgainstCompiledCode = false,
)