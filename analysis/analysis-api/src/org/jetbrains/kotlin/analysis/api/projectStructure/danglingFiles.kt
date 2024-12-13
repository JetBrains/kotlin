/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.psi.analysisContext
import org.jetbrains.kotlin.psi.doNotAnalyze

/**
 * Specifies how references to non-local declarations in the dangling files should be resolved.
 */
public enum class KaDanglingFileResolutionMode {
    /** Resolve first to declarations in the dangling file, and delegate to the original file or module only when needed. */
    PREFER_SELF,

    /** * Resolve only to declarations in the original file or module. Ignore all non-local declarations in the dangling file copy. */
    IGNORE_SELF
}

private val CONTEXT_MODULE_KEY = Key.create<KaModule>("CONTEXT_MODULE")

/**
 * A context module, against which analysis of this in-memory file should be performed.
 *
 * A [contextModule] can only be specified for an in-memory file.
 */
@KaExperimentalApi
public var KtFile.contextModule: KaModule?
    get() = getUserData(CONTEXT_MODULE_KEY)
    set(value) {
        require(this !is KtCodeFragment) { "'contextModule' cannot be set for code fragments" }

        val virtualFile = this.virtualFile
        if (virtualFile != null) {
            require(virtualFile is LightVirtualFile) { "'contextModule' is only available for in-memory files" }
        }
        putUserData(CONTEXT_MODULE_KEY, value)
    }

/**
 * A context module, against which analysis of this code fragment should be performed.
 * This is a [KtCodeFragment]-tailored version of [KtFile.contextModule].
 *
 * Normally, the context module is taken from the [PsiElement.getContext] element.
 * However, in some cases the code fragment needs to be analyzed in a refined environment.
 * Such as, the context element may be in the common module, while the code fragment is analyzed in its JVM counterpart.
 *
 * This is an advanced and rarely needed feature.
 * Use with caution.
 */
@KaImplementationDetail
public var KtCodeFragment.refinedContextModule: KaModule?
    get() = getUserData(CONTEXT_MODULE_KEY)
    set(value) = putUserData(CONTEXT_MODULE_KEY, value)

@OptIn(KaImplementationDetail::class, KaExperimentalApi::class)
public val KtFile.isDangling: Boolean
    get() = when {
        this is KtCodeFragment -> true
        contextModule != null -> true
        virtualFile?.analysisContextModule != null -> false
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
 * Note that the resolution mode affects equality of [KaDanglingFileModule]. It means that for each resolution mode, a separate
 * resolution session will be created.
 */
public val KtFile.danglingFileResolutionMode: KaDanglingFileResolutionMode?
    get() = danglingFileResolutionModeState?.get()

/**
 * Runs the [action] with a resolution mode being explicitly set for the dangling [file].
 *
 * Avoid using this function in client-side code. Use `analyzeCopy {}` from Analysis API instead.
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
