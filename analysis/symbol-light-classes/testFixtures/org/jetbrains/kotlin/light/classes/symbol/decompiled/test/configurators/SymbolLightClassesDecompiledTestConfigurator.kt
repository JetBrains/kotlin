/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators

import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryDecompiledTestConfigurator
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.services.TestServices

abstract class SymbolLightClassesDecompiledTestConfigurator(
    defaultTargetPlatform: TargetPlatform,
    override val testPrefixes: List<String>,
) : AnalysisApiFirLibraryBinaryDecompiledTestConfigurator(defaultTargetPlatform) {
    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.serviceRegistrars +
                AnalysisApiSymbolLightClassesDecompiledTestServiceRegistrar
}

object SymbolLightClassesDecompiledJvmTestConfigurator : SymbolLightClassesDecompiledTestConfigurator(
    JvmPlatforms.defaultJvmPlatform,
    listOf("lib"),
)

object SymbolLightClassesDecompiledJsTestConfigurator : SymbolLightClassesDecompiledTestConfigurator(
    JsPlatforms.defaultJsPlatform,
    listOf("kmp.lib"),
)
