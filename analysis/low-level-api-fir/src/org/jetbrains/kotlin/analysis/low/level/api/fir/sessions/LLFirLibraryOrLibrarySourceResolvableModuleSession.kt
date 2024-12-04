/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.fir.BuiltinTypes

internal class LLFirLibraryOrLibrarySourceResolvableModuleSession(
    ktModule: KaModule,
    override val moduleComponents: LLFirModuleResolveComponents,
    builtinTypes: BuiltinTypes,
) : LLFirResolvableModuleSession(ktModule, builtinTypes) {
    init {
        checkIsValidKtModule(ktModule)
    }

    companion object {
        fun checkIsValidKtModule(module: KaModule) {
            require(module is KaLibraryModule || module is KaLibrarySourceModule || module is KaBuiltinsModule) {
                "Expected ${KaLibraryModule::class.simpleName} or ${KaLibrarySourceModule::class.simpleName}, but ${module::class.simpleName} found"
            }
        }
    }
}
