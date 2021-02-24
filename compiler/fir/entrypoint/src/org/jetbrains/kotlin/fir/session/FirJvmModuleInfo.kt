/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

class FirJvmModuleInfo(
    override val name: Name,
    val dependencies: List<ModuleInfo> = emptyList(),
    val friendPaths: List<String> = emptyList(),
    val outputDirectory: String? = null
) : ModuleInfo {
    companion object {
        val LIBRARIES_MODULE_NAME = Name.special("<dependencies>")

        fun createForLibraries(mainModuleName: String? = null): FirJvmModuleInfo {
            val name = mainModuleName?.let { Name.special("<dependencies of $it>") } ?: LIBRARIES_MODULE_NAME
            return FirJvmModuleInfo(name)
        }
    }

    constructor(module: Module, dependencies: List<ModuleInfo>) :
            this(Name.identifier(module.getModuleName()), dependencies, module.getFriendPaths(), module.getOutputDirectory())

    override val platform: TargetPlatform
        get() = JvmPlatforms.unspecifiedJvmPlatform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = JvmPlatformAnalyzerServices

    override fun dependencies(): List<ModuleInfo> =
        dependencies
}
