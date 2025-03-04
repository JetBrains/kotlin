/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirModuleData
import java.nio.file.Path

abstract class ModuleDataProvider {
    abstract val allModuleData: Collection<FirModuleData>

    abstract fun getModuleData(path: Path?): FirModuleData?
}

class SingleModuleDataProvider(private val moduleData: FirModuleData) : ModuleDataProvider() {
    override val allModuleData: Collection<FirModuleData>
        get() = listOf(moduleData)

    override fun getModuleData(path: Path?): FirModuleData {
        return moduleData
    }
}

class MultipleModuleDataProvider(private val moduleDataWithFilters: Map<FirModuleData, LibraryPathFilter>) : ModuleDataProvider() {
    init {
        require(moduleDataWithFilters.isNotEmpty()) { "ModuleDataProvider must contain at least one module data" }
    }

    override val allModuleData: Collection<FirModuleData>
        get() = moduleDataWithFilters.keys

    override fun getModuleData(path: Path?): FirModuleData? {
        val normalizedPath = path?.normalize()
        for ((session, filter) in moduleDataWithFilters.entries) {
            if (filter.accepts(normalizedPath)) {
                return session
            }
        }
        return null
    }
}

