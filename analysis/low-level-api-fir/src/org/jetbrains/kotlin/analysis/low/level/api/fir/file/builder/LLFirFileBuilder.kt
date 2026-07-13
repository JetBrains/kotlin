/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.backReferences.assignBackReferencesToDeclarations
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.ThreadSafe

/**
 * Responsible for building [FirFile]s from [KtFile]s.
 */
@ThreadSafe
internal class LLFirFileBuilder(val moduleComponents: LLFirModuleResolveComponents) {
    private val projectStructureProvider by lazy { KotlinProjectStructureProvider.getInstance(moduleComponents.session.project) }

    fun buildRawFirFileWithCaching(ktFile: KtFile): FirFile = moduleComponents.cache.fileCached(ktFile) {
        val contextualModule = moduleComponents.module
        val actualFileModule = projectStructureProvider.getModule(ktFile, contextualModule)

        checkWithAttachment(actualFileModule == contextualModule, { "Modules are inconsistent" }) {
            withEntry("file", ktFile.name)
            withEntry("file module", actualFileModule) {
                it.toString()
            }
            withEntry("components module", contextualModule) {
                it.toString()
            }
        }

        PsiRawFirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            bodyBuildingMode = BodyBuildingMode.LAZY_BODIES
        ).buildFirFile(ktFile).also { firFile ->
            // "Back references to FIR" (KT-70517): assign a back reference to the containing file to every non-local declaration. Bodies
            // are lazy at this point, so local declarations are covered later, when their bodies are built (see RawFirNonLocalDeclarationBuilder).
            firFile.assignBackReferencesToDeclarations()
        }
    }
}
