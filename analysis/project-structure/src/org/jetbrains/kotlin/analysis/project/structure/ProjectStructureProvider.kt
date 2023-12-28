/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.project.structure.impl.KtDanglingFileModuleImpl
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.psi.analysisContext
import org.jetbrains.kotlin.psi.doNotAnalyze

public abstract class ProjectStructureProvider {
    /**
     * Returns a [KtModule] for a given [element] in the context of the [contextualModule].
     *
     * The contextual module is the [KtModule] from which [getModule] is called. It is a way to disambiguate the [KtModule] of [element]s
     * with whom multiple modules might be associated. In particular:
     *
     *  1. It allows replacing the original [KtModule] of [element] with another module, e.g. for supporting outsider files (see below).
     *  2. It helps to distinguish between multiple possible [KtModule]s for library elements.
     *
     * #### Outsider Modules
     *
     * Normally, every Kotlin source file either belongs to some module (e.g. a source module, or a library module), or is self-contained
     * (a script file, or a file outside content roots). However, in certain cases there might be special modules that include both
     * existing source files, and also some additional files.
     *
     * An example of such a module is one that owns an 'outsider' source file. Outsiders are used in IntelliJ for displaying files that
     * technically belong to some module, but are not included in the module's content roots (e.g. a file from a previous VCS revision).
     * As there might be cross-references between the outsider file and other files in the module, they need to be analyzed as a single
     * synthetic module. Inside an analysis session for such a module (which would be the [contextualModule]), sources that originally
     * belong to a source module should be treated rather as a part of the synthetic one.
     */
    public abstract fun getModule(element: PsiElement, contextualModule: KtModule?): KtModule

    protected abstract fun getNotUnderContentRootModule(project: Project): KtNotUnderContentRootModule

    @OptIn(KtModuleStructureInternals::class)
    protected fun computeSpecialModule(file: PsiFile): KtModule? {
        val virtualFile = file.virtualFile
        if (virtualFile != null) {
            val contextModule = virtualFile.analysisExtensionFileContextModule
            if (contextModule != null) {
                return contextModule
            }
        }

        if (file is KtFile && file.isDangling) {
            val contextModule = computeContextModule(file)
            val resolutionMode = file.danglingFileResolutionMode ?: computeDefaultDanglingFileResolutionMode(file)
            return KtDanglingFileModuleImpl(file, contextModule, resolutionMode)
        }

        return null
    }

    private fun computeDefaultDanglingFileResolutionMode(file: KtFile): DanglingFileResolutionMode {
        if (!file.isPhysical && !file.viewProvider.isEventSystemEnabled && file.originalFile != file) {
            return DanglingFileResolutionMode.IGNORE_SELF
        }

        return DanglingFileResolutionMode.PREFER_SELF
    }

    @OptIn(KtModuleStructureInternals::class)
    private fun computeContextModule(file: KtFile): KtModule {
        val contextElement = file.context
            ?: file.analysisContext
            ?: file.originalFile.takeIf { it !== file }

        if (contextElement != null) {
            return getModule(contextElement, contextualModule = null)
        }

        return getNotUnderContentRootModule(file.project)
    }

    /**
     * Project-global [LanguageVersionSettings] for source modules lacking explicit settings (such as [KtNotUnderContentRootModule]).
     */
    public open val globalLanguageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    /**
     * Project-global [LanguageVersionSettings] for [KtLibraryModule]s and [KtLibrarySourceModule]s.
     */
    public open val libraryLanguageVersionSettings: LanguageVersionSettings
        get() = globalLanguageVersionSettings

    public companion object {
        public fun getInstance(project: Project): ProjectStructureProvider {
            return project.getService(ProjectStructureProvider::class.java)
        }

        public fun getModule(project: Project, element: PsiElement, contextualModule: KtModule?): KtModule {
            return getInstance(project).getModule(element, contextualModule)
        }
    }
}

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
@KtModuleStructureInternals
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