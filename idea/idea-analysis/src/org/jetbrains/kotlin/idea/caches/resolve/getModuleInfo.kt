/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.FakeLightClassForFileOfPackage
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.core.script.KotlinScriptConfigurationManager
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.isInSourceContentWithoutInjected
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.script.getScriptDefinition
import org.jetbrains.kotlin.utils.sure

fun PsiElement.getModuleInfo(): IdeaModuleInfo = this.getModuleInfo { reason ->
    LOG.error("Could not find correct module information.\nReason: $reason")
    NotUnderContentRootModuleInfo
}.sure { "Defaulting to NotUnderContentRootModuleInfo so null is not possible" }

fun PsiElement.getNullableModuleInfo(): IdeaModuleInfo? = this.getModuleInfo { reason ->
    LOG.warn("Could not find correct module information.\nReason: $reason")
    null
}

private fun PsiElement.getModuleInfo(onFailure: (String) -> IdeaModuleInfo?): IdeaModuleInfo? {
    (containingFile?.moduleInfo as? IdeaModuleInfo)?.let { return it }

    if (this is KtLightElement<*, *>) return this.getModuleInfoForLightElement(onFailure)

    val containingJetFile = (this as? KtElement)?.containingFile as? KtFile
    val context = containingJetFile?.analysisContext
    if (context != null) return context.getModuleInfo()

    val doNotAnalyze = containingJetFile?.doNotAnalyze
    if (doNotAnalyze != null) {
        return onFailure(
                "Should not analyze element: ${text} in file ${containingJetFile?.name ?: " <no file>"}\n$doNotAnalyze"
        )
    }

    val explicitModuleInfo = containingJetFile?.moduleInfo ?: (containingJetFile?.originalFile as? KtFile)?.moduleInfo
    if (explicitModuleInfo is IdeaModuleInfo) return explicitModuleInfo

    if (containingJetFile is KtCodeFragment) {
        return containingJetFile.getContext()?.getModuleInfo()
               ?: onFailure("Analyzing code fragment of type ${containingJetFile.javaClass} with no context element\nText:\n${containingJetFile.getText()}")
    }

    val containingFile = containingFile ?: return onFailure("Analyzing element of type $javaClass with no containing file\nText:\n$text")

    val virtualFile = containingFile.originalFile.virtualFile
            ?: return onFailure("Analyzing element of type $javaClass in non-physical file $containingFile of type ${containingFile.javaClass}\nText:\n$text")

    return getModuleInfoByVirtualFile(
            project,
            virtualFile,
            isDecompiledFile = (containingFile as? KtFile)?.isCompiled ?: false
    )
}

private fun getModuleInfoByVirtualFile(project: Project, virtualFile: VirtualFile, isDecompiledFile: Boolean): IdeaModuleInfo {
    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    val module = projectFileIndex.getModuleForFile(virtualFile)
    if (module != null) {
        fun warnIfDecompiled() {
            if (isDecompiledFile) {
                LOG.warn("Decompiled file for ${virtualFile.canonicalPath} is in content of $module")
            }
        }

        val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex
        if (moduleFileIndex.isInTestSourceContent(virtualFile)) {
            warnIfDecompiled()
            return module.testSourceInfo()
        }
        else if (moduleFileIndex.isInSourceContentWithoutInjected(virtualFile)) {
            warnIfDecompiled()
            return module.productionSourceInfo()
        }
    }

    val orderEntries = projectFileIndex.getOrderEntriesForFile(virtualFile)

    entries@ for (orderEntry in orderEntries) {
        if (!orderEntry.isValid) continue@entries

        when (orderEntry) {
            is LibraryOrderEntry -> {
                val library = orderEntry.library ?: continue@entries
                if (ProjectRootsUtil.isLibraryClassFile(project, virtualFile) && !isDecompiledFile) {
                    return LibraryInfo(project, library)
                }
                else if (ProjectRootsUtil.isLibraryFile(project, virtualFile) || isDecompiledFile) {
                    return LibrarySourceInfo(project, library)
                }
            }
            is JdkOrderEntry -> {
                val sdk = orderEntry.jdk ?: continue@entries
                return SdkInfo(project, sdk)
            }
        }
    }

    val scriptDefinition = getScriptDefinition(virtualFile, project)
    if (scriptDefinition != null) {
        return ScriptModuleInfo(project, virtualFile, scriptDefinition)
    }

    if (KotlinScriptConfigurationManager.getInstance(project).getAllScriptsClasspathScope().contains(virtualFile)) {
        return ScriptDependenciesModuleInfo(project, null, null)
    }

    return NotUnderContentRootModuleInfo
}

private fun KtLightElement<*, *>.getModuleInfoForLightElement(onFailure: (String) -> IdeaModuleInfo?): IdeaModuleInfo? {
    val decompiledClass = this.getParentOfType<KtLightClassForDecompiledDeclaration>(strict = false)
    if (decompiledClass != null) {
        return getModuleInfoByVirtualFile(
                project,
                containingFile.virtualFile.sure { "Decompiled class should be build from physical file" },
                false
        )
    }
    val element = kotlinOrigin ?: when (this) {
        is FakeLightClassForFileOfPackage -> this.getContainingFile()!!
        is KtLightClassForFacade -> this.files.first()
        else -> return onFailure("Light element without origin is referenced by resolve:\n$this\n${this.clsDelegate.text}")
    }
    return element.getModuleInfo()
}
