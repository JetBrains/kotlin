/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

class FirTestModuleInfo(
    override val name: Name = Name.identifier("TestModule"),
    val dependencies: MutableList<ModuleInfo> = mutableListOf(),
    override val platform: TargetPlatform = JvmPlatforms.defaultJvmPlatform,
    override val analyzerServices: PlatformDependentAnalyzerServices = JvmPlatformAnalyzerServices
) : ModuleInfo {
    override fun dependencies(): List<ModuleInfo> = dependencies
}