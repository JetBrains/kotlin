/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.KotlinLightElement
import org.jetbrains.kotlin.psi.*
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.JdkOrderEntry
import org.jetbrains.kotlin.asJava.FakeLightClassForFileOfPackage
import org.jetbrains.kotlin.asJava.KotlinLightClassForFacade
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil

fun PsiElement.getModuleInfo(): IdeaModuleInfo {
    fun logAndReturnDefault(message: String): IdeaModuleInfo {
        LOG.error("Could not find correct module information.\nReason: $message")
        return NotUnderContentRootModuleInfo
    }

    if (this is KotlinLightElement<*, *>) return this.getModuleInfoForLightElement()

    val containingJetFile = (this as? JetElement)?.getContainingFile() as? JetFile
    val context = containingJetFile?.analysisContext
    if (context != null) return context.getModuleInfo()

    val doNotAnalyze = containingJetFile?.doNotAnalyze
    if (doNotAnalyze != null) {
        return logAndReturnDefault(
                "Should not analyze element: ${getText()} in file ${containingJetFile?.getName() ?: " <no file>"}\n$doNotAnalyze"
        )
    }

    val explicitModuleInfo = containingJetFile?.moduleInfo
    if (explicitModuleInfo is IdeaModuleInfo) return explicitModuleInfo

    if (containingJetFile is JetCodeFragment) {
        return containingJetFile.getContext()?.getModuleInfo()
               ?: logAndReturnDefault("Analyzing code fragment of type ${containingJetFile.javaClass} with no context element\nText:\n${containingJetFile.getText()}")
    }

    val project = getProject()
    val containingFile = getContainingFile()
            ?: return logAndReturnDefault("Analyzing element of type $javaClass with no containing file\nText:\n${getText()}")

    val virtualFile = containingFile.getOriginalFile().getVirtualFile()
            ?: return logAndReturnDefault("Analyzing non-physical file $containingFile of type ${containingFile.javaClass}")

    return getModuleInfoByVirtualFile(project, virtualFile, (containingFile as? JetFile)?.isCompiled() ?: false)
}

private fun getModuleInfoByVirtualFile(project: Project, virtualFile: VirtualFile, isDecompiledFile: Boolean): IdeaModuleInfo {
    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    val module = projectFileIndex.getModuleForFile(virtualFile)
    if (module != null) {
        fun warnIfDecompiled() {
            if (isDecompiledFile) {
                LOG.warn("Decompiled file for ${virtualFile.getCanonicalPath()} is in content of $module")
            }
        }

        val moduleFileIndex = ModuleRootManager.getInstance(module).getFileIndex()
        if (moduleFileIndex.isInTestSourceContent(virtualFile)) {
            warnIfDecompiled()
            return module.testSourceInfo()
        }
        else if (moduleFileIndex.isInSourceContent(virtualFile)) {
            warnIfDecompiled()
            return module.productionSourceInfo()
        }
    }

    val orderEntries = projectFileIndex.getOrderEntriesForFile(virtualFile)

    entries@ for (orderEntry in orderEntries) {
        when (orderEntry) {
            is LibraryOrderEntry -> {
                val library = orderEntry.getLibrary() ?: continue@entries
                if (ProjectRootsUtil.isLibraryClassFile(project, virtualFile) && !isDecompiledFile) {
                    return LibraryInfo(project, library)
                }
                else if (ProjectRootsUtil.isLibraryFile(project, virtualFile) || isDecompiledFile) {
                    return LibrarySourceInfo(project, library)
                }
            }
            is JdkOrderEntry -> {
                val sdk = orderEntry.getJdk() ?: continue@entries
                return SdkInfo(project, sdk)
            }
        }
    }
    return NotUnderContentRootModuleInfo
}

private fun KotlinLightElement<*, *>.getModuleInfoForLightElement(): IdeaModuleInfo {
    if (this is KotlinLightClassForDecompiledDeclaration) {
        return getModuleInfoByVirtualFile(getProject(), getContainingFile().getVirtualFile(), false)
    }
    val element = getOrigin() ?: when (this) {
        is FakeLightClassForFileOfPackage -> this.getContainingFile()!!
        is KotlinLightClassForFacade -> this.files.first()
        else -> throw IllegalStateException("Unknown light class without origin is referenced by IDE lazy resolve: $javaClass")
    }
    return element.getModuleInfo()
}
