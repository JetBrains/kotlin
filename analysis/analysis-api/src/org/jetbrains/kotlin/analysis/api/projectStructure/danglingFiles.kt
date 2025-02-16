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
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
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
     * The mode is only supported for single-file dangling file modules.
     */
    IGNORE_SELF
}

private val CONTEXT_MODULE_KEY = Key.create<KaModule>("CONTEXT_MODULE")

/**
 * A context module against which analysis of this in-memory file should be performed.
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
 * A context module against which analysis of this code fragment should be performed.
 *
 * [refinedContextModule] is a [KtCodeFragment]-tailored version of [KtFile.contextModule].
 *
 * Normally, the context module is taken from the [context element][PsiElement.getContext].
 * However, in some cases the code fragment needs to be analyzed in a refined environment.
 * Such as, the context element may be in the common module, while the code fragment is analyzed in its JVM counterpart.
 *
 * This is an advanced and rarely needed feature. Use it with caution.
 */
@KaImplementationDetail
public var KtCodeFragment.refinedContextModule: KaModule?
    get() = getUserData(CONTEXT_MODULE_KEY)
    set(value) = putUserData(CONTEXT_MODULE_KEY, value)

private val EXPLICIT_MODULE_KEY = Key.create<KaModule>("EXPLICIT_MODULE")

/**
 * A module to be used for analyzing the given file.
 * Currently, only [KaDanglingFileModule]s can be set as explicit modules.
 *
 * [explicitModule] can be useful for constructing dangling file modules consisting of more than one file:
 * 1. Create all in-memory files;
 * 2. Manually initiate a [KaDanglingFileModule], passing all created files.
 * 3. Set the newly created module as an [explicitModule] to all of the created files.
 *
 * Use with extreme care – [explicitModule] overrides all other configuration.
 * If you only need to provide the context module for an in-memory file, use [contextModule] instead.
 *
 * An explicit module can only be specified for an in-memory file.
 */
@KaExperimentalApi
public var KtFile.explicitModule: KaModule?
    get() = getUserData(EXPLICIT_MODULE_KEY)
    set(value) {
        @OptIn(KaPlatformInterface::class)
        require(value is KaDanglingFileModule) { "Only dangling file modules can be set as explicit modules" }

        val virtualFile = this.virtualFile
        if (virtualFile != null) {
            require(virtualFile is LightVirtualFile) { "'explicitModule' is only available for in-memory files" }
        }
        putUserData(EXPLICIT_MODULE_KEY, value)
    }

/**
 * Whether the [KtFile] is a *dangling* file.
 *
 * @see org.jetbrains.kotlin.analysis.api.analyzeCopy
 */
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
