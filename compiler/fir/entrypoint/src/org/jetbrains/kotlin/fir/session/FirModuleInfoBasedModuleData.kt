/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

class FirModuleInfoBasedModuleData(val moduleInfo: ModuleInfo) : FirModuleData() {
    override val name: Name
        get() = moduleInfo.name

    override val dependencies: List<FirModuleData> by lazy {
        moduleInfo.dependencies()
            .filterNot { it == moduleInfo }
            .map { FirModuleInfoBasedModuleData(it) }
    }

    override val dependsOnDependencies: List<FirModuleData> = moduleInfo.expectedBy
        .filterNot { it == moduleInfo }
        .map { FirModuleInfoBasedModuleData(it) }

    override val friendDependencies: List<FirModuleData> = moduleInfo.modulesWhoseInternalsAreVisible()
        .filterNot { it == moduleInfo }
        .map { FirModuleInfoBasedModuleData(it) }
    override val platform: TargetPlatform
        get() = moduleInfo.platform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = moduleInfo.analyzerServices

    override fun equals(other: Any?): Boolean {
        if (other !is FirModuleInfoBasedModuleData) return false
        return moduleInfo == other.moduleInfo
    }

    override fun hashCode(): Int {
        return moduleInfo.hashCode()
    }
}
