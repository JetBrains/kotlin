/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor

internal class LLFirDanglingFileSession @PrivateSessionConstructor constructor(
    ktModule: KaDanglingFileModule,
    override val moduleComponents: LLFirModuleResolveComponents,
    builtinTypes: BuiltinTypes
) : LLFirResolvableModuleSession(ktModule, builtinTypes) {
    private val cachedModificationStamp: Long = ktModule.modificationStamp

    val hasFileModifications: Boolean
        get() {
            val ktModule = this.ktModule as KaDanglingFileModule
            return cachedModificationStamp != ktModule.modificationStamp
        }
}

private val KaDanglingFileModule.modificationStamp: Long
    get() = files.sumOf { it.modificationStamp }