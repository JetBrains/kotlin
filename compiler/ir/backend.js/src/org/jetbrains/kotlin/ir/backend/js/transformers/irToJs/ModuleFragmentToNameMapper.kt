/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

interface ModuleFragmentToNameMapper {
    fun getNameForModule(fragment: IrModuleFragment, granularity: JsGenerationGranularity): String?
}

fun emptyModuleFragmentToNameMapper(): ModuleFragmentToNameMapper {
    return emptyMap<IrModuleFragment, String>().asModuleFragmentToNameMapper()
}

fun Map<IrModuleFragment, String>.asModuleFragmentToNameMapper(): ModuleFragmentToNameMapper {
    return ModuleFragmentToNameBasedOnMap(this)
}

private class ModuleFragmentToNameBasedOnMap(private val map: Map<IrModuleFragment, String>) : ModuleFragmentToNameMapper {
    override fun getNameForModule(fragment: IrModuleFragment, granularity: JsGenerationGranularity): String? {
        return map[fragment]
    }
}