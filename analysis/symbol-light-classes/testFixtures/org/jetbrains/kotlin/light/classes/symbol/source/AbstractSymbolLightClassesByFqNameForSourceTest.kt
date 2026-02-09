/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.source

import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesByFqNameTest
import org.jetbrains.kotlin.light.classes.symbol.base.SymbolLightClassScriptTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.base.SymbolLightClassSourceJsTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.base.SymbolLightClassSourceJvmTestConfigurator

abstract class AbstractSymbolLightClassesByFqNameForSourceTest : AbstractSymbolLightClassesByFqNameTest(
    configurator = SymbolLightClassSourceJvmTestConfigurator,
    isTestAgainstCompiledCode = false,
)

abstract class AbstractScriptSymbolLightClassesByFqNameForSourceTest : AbstractSymbolLightClassesByFqNameTest(
    configurator = SymbolLightClassScriptTestConfigurator,
    isTestAgainstCompiledCode = false,
)

abstract class AbstractJsSymbolLightClassesByFqNameForSourceTest : AbstractSymbolLightClassesByFqNameTest(
    configurator = SymbolLightClassSourceJsTestConfigurator,
    isTestAgainstCompiledCode = false,
)