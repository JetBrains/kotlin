/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

/**
 * Exception to signal that no [org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache] was
 * found for [missingModule] by [org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionProvider].
 *
 * TODO: This can be caused by the problem described in KT-51240. When the problem is fixed, this exception will probably become redundant.
 */
public class NoCacheForModuleException(
    private val missingModule: KtModule,
    private val existingModules: Set<KtModule>
) : NoSuchElementException() {
    override val message: String
        get() = "No cache was found for module '${missingModule.moduleDescription}'! " +
                "Caches exist for the following modules: ${existingModules.map { it.moduleDescription }}"
}