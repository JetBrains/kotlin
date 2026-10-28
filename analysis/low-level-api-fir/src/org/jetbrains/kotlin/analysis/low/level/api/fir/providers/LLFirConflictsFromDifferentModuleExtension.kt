/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.api.impl.base.util.withKaModuleEntry
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.danglingFileContextFile
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.analysis.checkers.FirConflictsFromDifferentModuleExtension
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

/**
 * [FirConflictsFromDifferentModuleExtension] for [KaDanglingFileModule]s.
 *
 * Declarations of a dangling file conflict with declarations of its context module, except for declarations of its context file, which
 * the dangling file stands in for. This only applies when the dangling files stand in for files of the context module, see
 * [conflictsWithContextModule].
 *
 * @param contextSession The session of [danglingFileModule]'s context module.
 */
internal class LLFirConflictsFromDifferentModuleExtension(
    private val danglingFileModule: KaDanglingFileModule,
    val contextSession: LLFirSession,
) : FirConflictsFromDifferentModuleExtension() {
    /**
     * Whether each dangling file stands in for a file of the context module, i.e., it has a context file which belongs to the context
     * module.
     *
     * Otherwise, the dangling files may be files of another module analyzed in the context module, e.g., a common file copied into
     * a dependent JVM module by the compiler facility (such a copy has no context file). Their declarations don't conflict with the context
     * module: an `expect` declaration of the copy and its `actual` counterpart in the context module are legitimate.
     *
     * Computed once per dangling file session, as only checkers need it.
     */
    val conflictsWithContextModule: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val contextModule = danglingFileModule.contextModule
        danglingFileModule.files.all { danglingFile ->
            val contextFile = danglingFile.danglingFileContextFile ?: return@all false
            KotlinProjectStructureProvider.getModule(contextFile.project, contextFile, useSiteModule = null) == contextModule
        }
    }

    override fun canConflictWithDeclarationsOfModule(otherModuleData: FirModuleData): Boolean {
        requireWithAttachment(
            otherModuleData is LLFirModuleData,
            { "Expected ${LLFirModuleData::class.simpleName}, got ${otherModuleData::class.simpleName}" },
        ) {
            withKaModuleEntry("danglingFileModule", danglingFileModule)
            withEntry("otherModuleData", otherModuleData) { it.toString() }
        }

        return otherModuleData.ktModule == danglingFileModule.contextModule && conflictsWithContextModule
    }

    override fun isAnalyzedInPlaceOf(useSiteFile: FirFile, targetFile: FirFile): Boolean {
        val danglingFile = useSiteFile.psi as? KtFile ?: return false
        val contextFile = danglingFile.danglingFileContextFile ?: return false
        return targetFile.psi == contextFile && conflictsWithContextModule
    }
}
