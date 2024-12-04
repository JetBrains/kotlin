/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.psi.analysisContext
import org.jetbrains.kotlin.psi.doNotAnalyze

/**
 * Specifies how references to non-local declarations in dangling files should be resolved.
 */
public enum class KaDanglingFileResolutionMode {
    /**
     * Resolve first to declarations in the dangling file, and delegate to the original file or module only when needed.
     */
    PREFER_SELF,

    /**
     * Resolve only to declarations in the original file or module. Ignore all non-local declarations in the dangling file.
     */
    IGNORE_SELF
}

/**
 * Whether the [KtFile] is a *dangling* file.
 *
 * @see org.jetbrains.kotlin.analysis.api.analyzeCopy
 */
@OptIn(KaImplementationDetail::class)
public val KtFile.isDangling: Boolean
    get() = when {
        this is KtCodeFragment -> true
        virtualFile?.analysisContextModule != null -> false
        !isPhysical -> true
        analysisContext != null -> true
        doNotAnalyze != null -> true
        else -> false
    }

/**
 * Returns the resolution mode that is explicitly set for this dangling file, or `null` for files that are not dangling or if the mode was
 * not set.
 *
 * Use the [analyzeCopy][org.jetbrains.kotlin.analysis.api.analyzeCopy] function for specifying the analysis mode. The effect is
 * thread-local by design, as the file might potentially be resolved concurrently in different threads.
 *
 * The resolution mode affects equality of [KaDanglingFileModule]s. For each resolution mode, a separate resolution module and session will
 * be created.
 */
public val KtFile.danglingFileResolutionMode: KaDanglingFileResolutionMode?
    get() = danglingFileResolutionModeState?.get()

/**
 * Runs the [action] with a resolution mode being explicitly set for the dangling [file].
 *
 * Avoid using this function in client-side code. Use [analyzeCopy][org.jetbrains.kotlin.analysis.api.analyzeCopy] instead.
 */
@KaImplementationDetail
public fun <R> withDanglingFileResolutionMode(file: KtFile, mode: KaDanglingFileResolutionMode, action: () -> R): R {
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

private fun getOrCreateDanglingFileResolutionModeState(file: KtFile): ThreadLocal<KaDanglingFileResolutionMode?> {
    synchronized(file) {
        val existingState = file.danglingFileResolutionModeState
        if (existingState != null) {
            return existingState
        }

        val newState = ThreadLocal<KaDanglingFileResolutionMode?>()
        file.danglingFileResolutionModeState = newState
        return newState
    }
}

private var KtFile.danglingFileResolutionModeState: ThreadLocal<KaDanglingFileResolutionMode?>?
        by UserDataProperty(Key.create("DANGLING_FILE_RESOLUTION_MODE"))
