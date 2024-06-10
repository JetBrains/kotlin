/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.projectStructure.danglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.isDangling
import org.jetbrains.kotlin.analysis.project.structure.DanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtModuleStructureInternals
import org.jetbrains.kotlin.analysis.project.structure.KtNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.project.structure.analysisExtensionFileContextModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtDanglingFileModuleImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.analysisContext

public abstract class KotlinProjectStructureProviderBase : KotlinProjectStructureProvider {
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
            return getModule(contextElement, useSiteModule = null)
        }

        return getNotUnderContentRootModule(file.project)
    }
}
