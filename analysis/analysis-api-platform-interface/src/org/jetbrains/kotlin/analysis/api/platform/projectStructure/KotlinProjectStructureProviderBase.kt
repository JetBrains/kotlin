/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.analysisContext

@KaPlatformInterface
public abstract class KotlinProjectStructureProviderBase : KotlinProjectStructureProvider {
    protected abstract fun getNotUnderContentRootModule(project: Project): KaNotUnderContentRootModule

    @OptIn(KaImplementationDetail::class, KaExperimentalApi::class)
    protected fun computeSpecialModule(file: PsiFile): KaModule? {
        if (file is KtFile) {
            val explicitModule = file.explicitModule
            if (explicitModule != null) {
                return explicitModule
            }
        }

        file.virtualFile?.resolveExtensionFileModule?.let { return it }

        if (file is KtFile && file.isDangling) {
            val contextModule = computeContextModule(file)
            val resolutionMode = file.danglingFileResolutionMode ?: computeDefaultDanglingFileResolutionMode(file)
            return KaDanglingFileModuleImpl(listOf(file), contextModule, resolutionMode)
        }

        return null
    }

    @OptIn(KaExperimentalApi::class, KaImplementationDetail::class)
    private fun computeDefaultDanglingFileResolutionMode(file: KtFile): KaDanglingFileResolutionMode {
        if (autoDanglingResolutionMode) {
            file.calculatedDanglingFileResolutionMode?.let { return it }

            val calculatedMode = KaDanglingFileResolutionModeProvider.calculateMode(file)

            if (file.isPhysical && file.copyOrigin != null) {
                file.calculatedDanglingFileResolutionMode = calculatedMode
            }

            return calculatedMode
        }

        if (!file.isPhysical && !file.viewProvider.isEventSystemEnabled && file.copyOrigin != null) {
            return KaDanglingFileResolutionMode.IGNORE_SELF
        }

        return KaDanglingFileResolutionMode.PREFER_SELF
    }

    private val autoDanglingResolutionMode by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Registry.`is`("kotlin.analysis.autoDanglingResolutionMode", true)
    }

    @OptIn(KaImplementationDetail::class, KaExperimentalApi::class)
    private fun computeContextModule(file: KtFile): KaModule {
        val originalFile = file.copyOrigin

        @Suppress("DEPRECATION")
        originalFile?.virtualFile?.analysisContextModule?.let { return it }

        file.contextModule?.let { return it }

        val contextElement = originalFile
            ?: file.context?.takeIf(::isSupportedContextElement)
            ?: file.analysisContext?.takeIf(::isSupportedContextElement)

        if (contextElement != null) {
            val contextModule = getModule(contextElement, useSiteModule = null)
            if (contextModule is KaDanglingFileModule && file !is KtCodeFragment) {
                // Only code fragments can have dangling file modules in contexts
                return contextModule.baseContextModule
            }
            return contextModule
        }

        return getNotUnderContentRootModule(file.project)
    }

    private fun isSupportedContextElement(context: PsiElement): Boolean {
        // Support Kotlin files and Java/Kotlin packages
        return context.language == KotlinLanguage.INSTANCE || context is PsiDirectory
    }
}

@KaPlatformInterface
@OptIn(KaExperimentalApi::class)
@Deprecated("Use 'explicitModule' instead.")
public var KtCodeFragment.forcedSpecialModule: KaDanglingFileModule?
    get() = explicitModule as? KaDanglingFileModule
    set(value) {
        explicitModule = value
    }

private val DANGLING_FILE_RESOLUTION_MODE: Key<KaDanglingFileResolutionMode?> =
    Key.create<KaDanglingFileResolutionMode>("DANGLING_FILE_RESOLUTION_MODE")

/**
 * Stores [KaDanglingFileResolutionMode] calculated by [KaDanglingFileResolutionModeProvider] for [this].
 *
 * This caching is only supported for physical dangling files with non-null [copyOrigin].
 */
@KaImplementationDetail
public var KtFile.calculatedDanglingFileResolutionMode: KaDanglingFileResolutionMode?
    get() = getUserData(DANGLING_FILE_RESOLUTION_MODE)
    set(value) {
        require(this.isDangling) { "Only dangling files can have KaDanglingFileResolutionMode" }
        @OptIn(KaExperimentalApi::class)
        require(this.copyOrigin != null) { "Only dangling files with non-null `copyOrigin` can have KaDanglingFileResolutionMode" }
        require(this.isPhysical) { "Only physical dangling files can have cached KaDanglingFileResolutionMode" }

        putUserData(DANGLING_FILE_RESOLUTION_MODE, value)
    }