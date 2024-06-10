/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.project.structure.DanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.project.structure.KtModuleStructureInternals
import org.jetbrains.kotlin.analysis.project.structure.analysisExtensionFileContextModule
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.psi.analysisContext
import org.jetbrains.kotlin.psi.doNotAnalyze

@OptIn(KtModuleStructureInternals::class)
public val KtFile.isDangling: Boolean
    get() = when {
        this is KtCodeFragment -> true
        virtualFile?.analysisExtensionFileContextModule != null -> false
        !isPhysical -> true
        analysisContext != null -> true
        doNotAnalyze != null -> true
        else -> false
    }

/**
 * Returns the resolution mode that is explicitly set for this dangling file.
 * Returns `null` for files that are not dangling, or if the mode was not set.
 *
 * Use the `analyzeCopy {}` method for specifying the analysis mode. Note that the effect is thread-local; this is made on purpose, as
 * the file might potentially be resolved in parallel in different threads.
 *
 * Note that the resolution mode affects equality of [KtDanglingFileModule]. It means that for each resolution mode, a separate
 * resolution session will be created.
 */
@OptIn(KtModuleStructureInternals::class)
public val KtFile.danglingFileResolutionMode: DanglingFileResolutionMode?
    get() = danglingFileResolutionModeState?.get()

/**
 * Runs the [action] with a resolution mode being explicitly set for the dangling [file].
 *
 * Avoid using this function in client-side code. Use `analyzeCopy {}` from Analysis API instead.
 */
@KtAnalysisApiInternals
public fun <R> withDanglingFileResolutionMode(file: KtFile, mode: DanglingFileResolutionMode, action: () -> R): R {
    require(file.isDangling) { "'withDanglingFileResolutionMode()' is only available to dangling files" }
    require(file.originalFile != file) { "'withDanglingFileResolutionMode()' is only available to file copies" }

    val modeState = getOrCreateDanglingFileResolutionModeState(file)

    val oldValue = modeState.get()
    try {
        modeState.set(mode)
        return action()
    } finally {
        modeState.set(oldValue)
    }
}

private fun getOrCreateDanglingFileResolutionModeState(file: KtFile): ThreadLocal<DanglingFileResolutionMode?> {
    synchronized(file) {
        val existingState = file.danglingFileResolutionModeState
        if (existingState != null) {
            return existingState
        }

        val newState = ThreadLocal<DanglingFileResolutionMode?>()
        file.danglingFileResolutionModeState = newState
        return newState
    }
}

private var KtFile.danglingFileResolutionModeState: ThreadLocal<DanglingFileResolutionMode?>?
        by UserDataProperty(Key.create("DANGLING_FILE_RESOLUTION_MODE"))
