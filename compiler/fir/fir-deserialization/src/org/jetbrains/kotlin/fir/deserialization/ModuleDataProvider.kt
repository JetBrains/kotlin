/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.utils.addToStdlib.same
import java.nio.file.Path

abstract class ModuleDataProvider {
    abstract val platform: TargetPlatform
    abstract val analyzerServices: PlatformDependentAnalyzerServices
    abstract val allModuleData: Collection<FirModuleData>

    abstract fun getModuleData(path: Path?): FirModuleData?
}

class SingleModuleDataProvider(private val moduleData: FirModuleData) : ModuleDataProvider() {
    override val platform: TargetPlatform
        get() = moduleData.platform
    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = moduleData.analyzerServices
    override val allModuleData: Collection<FirModuleData>
        get() = listOf(moduleData)

    override fun getModuleData(path: Path?): FirModuleData {
        return moduleData
    }
}

class MultipleModuleDataProvider(private val moduleDataWithFilters: Map<FirModuleData, LibraryPathFilter>) : ModuleDataProvider() {
    init {
        require(moduleDataWithFilters.isNotEmpty()) { "ModuleDataProvider must contain at least one module data" }
        require(moduleDataWithFilters.keys.same { it.platform }) {
            "All module data should have same target platform, but was: ${moduleDataWithFilters.keys.joinToString { "${it.name.asString()}: ${it.platform}" }}"
        }
        require(moduleDataWithFilters.keys.same { it.analyzerServices }) {
            "All module data should have same analyzerServices, but was: ${moduleDataWithFilters.keys.joinToString { "${it.name.asString()}: ${it.analyzerServices::class.simpleName}" }}"
        }
    }

    override val platform: TargetPlatform = allModuleData.first().platform

    override val analyzerServices: PlatformDependentAnalyzerServices = allModuleData.first().analyzerServices

    override val allModuleData: Collection<FirModuleData>
        get() = moduleDataWithFilters.keys

    override fun getModuleData(path: Path?): FirModuleData? {
        for ((session, filter) in moduleDataWithFilters.entries) {
            if (filter.accepts(path)) {
                return session
            }
        }
        return null
    }
}

