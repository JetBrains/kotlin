/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.ThreadSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.psi.KtFile

/**
 * Responsible for building [FirFile] by [KtFile]
 */
@ThreadSafe
internal class LLFirFileBuilder(
    val moduleComponents: LLFirModuleResolveComponents,
) {
    fun buildRawFirFileWithCaching(ktFile: KtFile): FirFile = moduleComponents.cache.fileCached(ktFile) {
        checkWithAttachmentBuilder(ktFile.getKtModule() == moduleComponents.module, { "Modules are inconsistent" }) {
            withEntry("file", ktFile.name)
            withEntry("file module", ktFile.getKtModule()) {
                it.toString()
            }
            withEntry("components module", moduleComponents.module) {
                it.toString()
            }
        }
        val bodyBuildingMode = when {
            ktFile.isScript() -> {
                // As 'FirScript' content is never transformed, lazy bodies are not replaced with calculated ones even on BODY_RESOLVE.
                // Such behavior breaks file structure mapping computation.
                // TODO: remove this clause when proper support for scripts is implemented in K2.
                BodyBuildingMode.NORMAL
            }
            else -> BodyBuildingMode.LAZY_BODIES
        }

        RawFirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            bodyBuildingMode = bodyBuildingMode
        ).buildFirFile(ktFile)
    }
}


