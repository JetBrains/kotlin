/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
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
            return KaDanglingFileResolutionModeProvider.calculateMode(file)
        }

        if (!file.isPhysical && !file.viewProvider.isEventSystemEnabled && file.copyOrigin != null) {
            return KaDanglingFileResolutionMode.IGNORE_SELF
        }

        return KaDanglingFileResolutionMode.PREFER_SELF
    }

    private val autoDanglingResolutionMode by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Registry.`is`("kotlin.analysis.autoDanglingResolutionMode", false)
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
