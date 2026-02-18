/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesByPsiTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators.SymbolLightClassesDecompiledJsTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators.SymbolLightClassesDecompiledJvmTestConfigurator

abstract class AbstractSymbolLightClassesByPsiForLibraryTest : AbstractSymbolLightClassesByPsiTest(
    SymbolLightClassesDecompiledJvmTestConfigurator,
    isTestAgainstCompiledCode = true,
)

abstract class AbstractJsSymbolLightClassesByPsiForLibraryTest : AbstractSymbolLightClassesByPsiTest(
    SymbolLightClassesDecompiledJsTestConfigurator,
    isTestAgainstCompiledCode = true,
)