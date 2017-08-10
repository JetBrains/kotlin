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
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.FakeLightClassForFileOfPackage
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.isKotlinBinary
import org.jetbrains.kotlin.idea.util.isInSourceContentWithoutInjected
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.script.getScriptDefinition
import org.jetbrains.kotlin.utils.sure

fun PsiElement.getModuleInfo(): IdeaModuleInfo = this.getModuleInfo { reason ->
    LOG.error("Could not find correct module information.\nReason: $reason")
    NotUnderContentRootModuleInfo
} ?: NotUnderContentRootModuleInfo

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
                "Should not analyze element: $text in file ${containingJetFile.name}\n$doNotAnalyze"
        )
    }

    val explicitModuleInfo = containingJetFile?.moduleInfo ?: (containingJetFile?.originalFile as? KtFile)?.moduleInfo
    if (explicitModuleInfo is IdeaModuleInfo) return explicitModuleInfo

    if (containingJetFile is KtCodeFragment) {
        return containingJetFile.getContext()?.getModuleInfo()
               ?: onFailure("Analyzing code fragment of type ${containingJetFile::class.java} with no context element\nText:\n${containingJetFile.getText()}")
    }

    val containingFile = containingFile ?: return onFailure("Analyzing element of type ${this::class.java} with no containing file\nText:\n$text")

    val virtualFile = containingFile.originalFile.virtualFile
            ?: return onFailure("Analyzing element of type ${this::class.java} in non-physical file $containingFile of type ${containingFile::class.java}\nText:\n$text")

    return getModuleInfoByVirtualFile(
            project,
            virtualFile,
            treatAsLibrarySource = (containingFile as? KtFile)?.isCompiled ?: false
    )
}

fun getModuleInfoByVirtualFile(
        project: Project, virtualFile: VirtualFile
): IdeaModuleInfo? = getModuleInfoByVirtualFile(project, virtualFile, treatAsLibrarySource = false)

private fun getModuleInfoByVirtualFile(project: Project, virtualFile: VirtualFile, treatAsLibrarySource: Boolean): IdeaModuleInfo? {
    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    val module = projectFileIndex.getModuleForFile(virtualFile)
    if (module != null) {
        if (module.isDisposed) return null

        fun warnIfDecompiled() {
            if (treatAsLibrarySource) {
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

    projectFileIndex.getOrderEntriesForFile(virtualFile)
            .process(project, virtualFile, treatAsLibrarySource) { correspondingModuleInfo ->
                return correspondingModuleInfo
            }

    val scriptDefinition = getScriptDefinition(virtualFile, project)
    if (scriptDefinition != null) {
        return ScriptModuleInfo(project, virtualFile, scriptDefinition)
    }

    val isBinary = virtualFile.isKotlinBinary()
    val scriptConfigurationManager = ScriptDependenciesManager.getInstance(project)
    if (isBinary && virtualFile in scriptConfigurationManager.getAllScriptsClasspathScope()) {
        return if (treatAsLibrarySource) {
            ScriptDependenciesSourceModuleInfo(project)
        }
        else {
            ScriptDependenciesModuleInfo(project, null)
        }
    }
    if (!isBinary && virtualFile in scriptConfigurationManager.getAllLibrarySourcesScope()) {
        return ScriptDependenciesSourceModuleInfo(project)
    }

    return null
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

fun getBinaryLibrariesModuleInfos(project: Project, virtualFile: VirtualFile) = collectModuleInfosByType<BinaryModuleInfo>(project, virtualFile)
fun getLibrarySourcesModuleInfos(project: Project, virtualFile: VirtualFile) = collectModuleInfosByType<LibrarySourceInfo>(project, virtualFile)

private inline fun <reified T : IdeaModuleInfo> collectModuleInfosByType(project: Project, virtualFile: VirtualFile): Collection<T> {
    val orderEntries = ProjectFileIndex.SERVICE.getInstance(project).getOrderEntriesForFile(virtualFile)

    val result = linkedSetOf<T?>()
    orderEntries.process(project, virtualFile, treatAsLibrarySource = false) {
        result.add(it as? T)
    }
    // NOTE: non idea model infos can be obtained this way, like script related infos
    // only one though, luckily it covers existing cases
    result.add(getModuleInfoByVirtualFile(project, virtualFile) as? T)

    return result.filterNotNull()
}

private inline fun List<OrderEntry>.process(
        project: Project,
        virtualFile: VirtualFile,
        treatAsLibrarySource: Boolean = false,
        body: (IdeaModuleInfo) -> Unit
) {
    entries@ for (orderEntry in this) {
        if (!orderEntry.isValid) continue

        when (orderEntry) {
            is LibraryOrderEntry -> {
                val library = orderEntry.library ?: continue@entries
                if (ProjectRootsUtil.isLibraryClassFile(project, virtualFile) && !treatAsLibrarySource) {
                    body(LibraryInfo(project, library))
                }
                else if (ProjectRootsUtil.isLibraryFile(project, virtualFile) || treatAsLibrarySource) {
                    body(LibrarySourceInfo(project, library))
                }
            }
            is JdkOrderEntry -> {
                val sdk = orderEntry.jdk ?: continue@entries
                body(SdkInfo(project, sdk))
            }
        }
    }
}