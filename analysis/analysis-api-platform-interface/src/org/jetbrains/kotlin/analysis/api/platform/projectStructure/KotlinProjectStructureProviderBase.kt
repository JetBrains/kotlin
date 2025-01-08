/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.danglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.isDangling
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.analysis.api.projectStructure.analysisContextModule
import org.jetbrains.kotlin.analysis.api.projectStructure.contextModule
import org.jetbrains.kotlin.analysis.api.projectStructure.explicitModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.analysisContext

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

        val virtualFile = file.virtualFile
        if (virtualFile != null) {
            val contextModule = virtualFile.analysisContextModule
            if (contextModule != null) {
                return contextModule
            }
        }

        if (file is KtFile && file.isDangling) {
            val contextModule = computeContextModule(file)
            val resolutionMode = file.danglingFileResolutionMode ?: computeDefaultDanglingFileResolutionMode(file)
            return KaDanglingFileModuleImpl(listOf(file), contextModule, resolutionMode)
        }

        return null
    }

    private fun computeDefaultDanglingFileResolutionMode(file: KtFile): KaDanglingFileResolutionMode {
        if (!file.isPhysical && !file.viewProvider.isEventSystemEnabled && file.originalFile != file) {
            return KaDanglingFileResolutionMode.IGNORE_SELF
        }

        return KaDanglingFileResolutionMode.PREFER_SELF
    }

    @OptIn(KaImplementationDetail::class, KaExperimentalApi::class)
    private fun computeContextModule(file: KtFile): KaModule {
        val originalFile = file.originalFile.takeIf { it !== file }
        originalFile?.virtualFile?.analysisContextModule?.let { return it }

        file.contextModule?.let { return it }

        val contextElement = file.context
            ?: file.analysisContext
            ?: originalFile

        if (contextElement != null) {
            return getModule(contextElement, useSiteModule = null)
        }

        return getNotUnderContentRootModule(file.project)
    }
}

@OptIn(KaExperimentalApi::class)
@Deprecated("Use 'explicitModule' instead.")
public var KtCodeFragment.forcedSpecialModule: KaDanglingFileModule?
    get() = explicitModule as? KaDanglingFileModule
    set(value) {
        explicitModule = value
    }
