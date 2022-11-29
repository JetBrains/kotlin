/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.BuiltinTypes

internal class LLFirLibraryOrLibrarySourceResolvableModuleSession(
    override val ktModule: KtModule,
    override val project: Project,
    override val moduleComponents: LLFirModuleResolveComponents,
    builtinTypes: BuiltinTypes,
) : LLFirResolvableModuleSession(builtinTypes) {
    init {
        checkIsValidKtModule(ktModule)
    }

    companion object {
        fun checkIsValidKtModule(module: KtModule) {
            require(module is KtLibraryModule || module is KtLibrarySourceModule) {
                "Expected ${KtLibraryModule::class.simpleName} or ${KtLibrarySourceModule::class.simpleName}, but ${module::class.simpleName} found"
            }
        }
    }
}
