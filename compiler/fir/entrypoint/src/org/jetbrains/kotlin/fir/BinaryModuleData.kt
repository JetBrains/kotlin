/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

class BinaryModuleData(
    val regular: FirModuleData,
    val dependsOn: FirModuleData,
    val friends: FirModuleData
) {
    companion object {
        fun createDependencyModuleData(
            name: Name,
            platform: TargetPlatform,
            analyzerServices: PlatformDependentAnalyzerServices,
            capabilities: FirModuleCapabilities = FirModuleCapabilities.Empty
        ): FirModuleData {
            return FirModuleDataImpl(
                name,
                dependencies = emptyList(),
                dependsOnDependencies = emptyList(),
                friendDependencies = emptyList(),
                platform,
                analyzerServices,
                capabilities
            )
        }

        fun initialize(
            mainModuleName: Name,
            platform: TargetPlatform,
            analyzerServices: PlatformDependentAnalyzerServices
        ): BinaryModuleData {
            fun createData(name: String): FirModuleData =
                createDependencyModuleData(Name.special(name), platform, analyzerServices)

            return BinaryModuleData(
                createData("<regular dependencies of $mainModuleName>"),
                createData("<dependsOn dependencies of $mainModuleName"),
                createData("<friends dependencies of $mainModuleName")
            )
        }
    }
}