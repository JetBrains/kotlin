/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

class KtModuleBasedModuleData(
    val module: KtModule,
) : FirModuleData() {
    override val name: Name get() = Name.special("<${module.moduleDescription}>")

    override val dependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.directRegularDependencies.map(::KtModuleBasedModuleData)
    }

    override val dependsOnDependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.directRefinementDependencies.map(::KtModuleBasedModuleData)
    }

    override val friendDependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.directRefinementDependencies.map(::KtModuleBasedModuleData)
    }

    override val platform: TargetPlatform get() = module.platform

    override val analyzerServices: PlatformDependentAnalyzerServices get() = module.analyzerServices

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtModuleBasedModuleData

        if (module != other.module) return false

        return true
    }

    override fun hashCode(): Int {
        return module.hashCode()
    }
}
