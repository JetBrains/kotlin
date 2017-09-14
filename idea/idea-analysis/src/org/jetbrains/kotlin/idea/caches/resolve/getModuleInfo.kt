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
import org.jetbrains.kotlin.idea.util.isInSourceContentWithoutInjected
import org.jetbrains.kotlin.idea.util.isKotlinBinary
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.script.getScriptDefinition
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.sure

fun PsiElement.getModuleInfo(): IdeaModuleInfo = this.collectInfos(ModuleInfoCollector.NotNullTakeFirst)

fun PsiElement.getNullableModuleInfo(): IdeaModuleInfo? = this.collectInfos(ModuleInfoCollector.NullableTakeFirst)

fun PsiElement.getModuleInfos(): List<IdeaModuleInfo> = this.collectInfos(ModuleInfoCollector.ToList)

fun getModuleInfoByVirtualFile(project: Project, virtualFile: VirtualFile): IdeaModuleInfo? = collectInfosByVirtualFile(
        project, virtualFile,
        treatAsLibrarySource = false,
        onOccurrence = { return@getModuleInfoByVirtualFile it }
)

fun getBinaryLibrariesModuleInfos(project: Project, virtualFile: VirtualFile)
        = collectModuleInfosByType<BinaryModuleInfo>(project, virtualFile)
fun getLibrarySourcesModuleInfos(project: Project, virtualFile: VirtualFile)
        = collectModuleInfosByType<LibrarySourceInfo>(project, virtualFile)

private typealias VirtualFileProcessor<T> = (Project, VirtualFile, Boolean) -> T

private sealed class ModuleInfoCollector<out T>(
        val onResult: (IdeaModuleInfo?) -> T,
        val onFailure: (String) -> T,
        val virtualFileProcessor: VirtualFileProcessor<T>
) {
    object NotNullTakeFirst : ModuleInfoCollector<IdeaModuleInfo>(
            onResult = { it ?: NotUnderContentRootModuleInfo },
            onFailure = { reason ->
                LOG.error("Could not find correct module information.\nReason: $reason")
                NotUnderContentRootModuleInfo
            },
            virtualFileProcessor = processor@ { project, virtualFile, isLibrarySource ->
                collectInfosByVirtualFile(project, virtualFile, isLibrarySource, { return@processor it ?: NotUnderContentRootModuleInfo })
            }
    )

    object NullableTakeFirst: ModuleInfoCollector<IdeaModuleInfo?>(
            onResult = { it },
            onFailure = { reason ->
                LOG.warn("Could not find correct module information.\nReason: $reason")
                null
            },
            virtualFileProcessor = processor@ { project, virtualFile, isLibrarySource ->
                collectInfosByVirtualFile(project, virtualFile, isLibrarySource, { return@processor it })
            }
    )

    object ToList : ModuleInfoCollector<List<IdeaModuleInfo>>(
            onResult = { it?.let(::listOf).orEmpty() },
            onFailure = { reason ->
                LOG.warn("Could not find correct module information.\nReason: $reason")
                emptyList()
            },
            virtualFileProcessor = { project, virtualFile, isLibrarySource ->
                val result = mutableListOf<IdeaModuleInfo>()
                collectInfosByVirtualFile(project, virtualFile, isLibrarySource, { result.addIfNotNull(it) })
                result
            }
    )
}

private fun <T> PsiElement.collectInfos(c: ModuleInfoCollector<T>): T {
    (containingFile?.moduleInfo as? IdeaModuleInfo)?.let {
        return c.onResult(it)
    }

    if (this is KtLightElement<*, *>) {
        return this.processLightElement(c)
    }

    val containingFile = containingFile ?:
                         return c.onFailure("Analyzing element of type ${this::class.java} with no containing file\nText:\n$text")

    val containingKtFile = (this as? KtElement)?.containingFile as? KtFile
    containingKtFile?.analysisContext?.let {
        return it.collectInfos(c)
    }

    containingKtFile?.doNotAnalyze?.let {
        return c.onFailure("Should not analyze element: $text in file ${containingKtFile.name}\n$it")
    }

    val explicitModuleInfo = containingKtFile?.moduleInfo ?: (containingKtFile?.originalFile as? KtFile)?.moduleInfo
    if (explicitModuleInfo is IdeaModuleInfo) {
        return c.onResult(explicitModuleInfo)
    }

    if (containingKtFile is KtCodeFragment) {
        val context = containingKtFile.getContext() ?:
                      return c.onFailure("Analyzing code fragment of type ${containingKtFile::class.java} with no context element\nText:\n${containingKtFile.getText()}")
        return context.collectInfos(c)
    }

    val virtualFile = containingFile.originalFile.virtualFile
                      ?: return c.onFailure("Analyzing element of type ${this::class.java} in non-physical file $containingFile of type ${containingFile::class.java}\nText:\n$text")

    return c.virtualFileProcessor(
            project,
            virtualFile,
            (containingFile as? KtFile)?.isCompiled ?: false
    )
}

private fun <T> KtLightElement<*, *>.processLightElement(c: ModuleInfoCollector<T>): T {
    val decompiledClass = this.getParentOfType<KtLightClassForDecompiledDeclaration>(strict = false)
    if (decompiledClass != null) {
        return c.virtualFileProcessor(
                project,
                containingFile.virtualFile.sure { "Decompiled class should be build from physical file" },
                false
        )
    }

    val element = kotlinOrigin ?: when (this) {
        is FakeLightClassForFileOfPackage -> this.getContainingFile()!!
        is KtLightClassForFacade -> this.files.first()
        else -> return c.onFailure("Light element without origin is referenced by resolve:\n$this\n${this.clsDelegate.text}")
    }

    return element.collectInfos(c)
}

private inline fun <T> collectInfosByVirtualFile(
        project: Project, virtualFile: VirtualFile,
        treatAsLibrarySource: Boolean, onOccurrence: (IdeaModuleInfo?) -> T
): T {
    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    val module = projectFileIndex.getModuleForFile(virtualFile)
    if (module != null && !module.isDisposed) {
        val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex
        if (moduleFileIndex.isInTestSourceContent(virtualFile)) {
            onOccurrence(module.testSourceInfo())
        }
        else if (moduleFileIndex.isInSourceContentWithoutInjected(virtualFile)) {
            onOccurrence(module.productionSourceInfo())
        }
    }

    projectFileIndex.getOrderEntriesForFile(virtualFile).forEach {
        it.toIdeaModuleInfo(project, virtualFile, treatAsLibrarySource)?.let(onOccurrence)
    }

    val scriptDefinition = getScriptDefinition(virtualFile, project)
    if (scriptDefinition != null) {
        onOccurrence(ScriptModuleInfo(project, virtualFile, scriptDefinition))
    }

    val isBinary = virtualFile.isKotlinBinary()
    val scriptConfigurationManager = ScriptDependenciesManager.getInstance(project)
    if (isBinary && virtualFile in scriptConfigurationManager.getAllScriptsClasspathScope()) {
        if (treatAsLibrarySource) {
            onOccurrence(ScriptDependenciesSourceModuleInfo(project))
        }
        else {
            onOccurrence(ScriptDependenciesModuleInfo(project, null))
        }
    }
    if (!isBinary && virtualFile in scriptConfigurationManager.getAllLibrarySourcesScope()) {
        onOccurrence(ScriptDependenciesSourceModuleInfo(project))
    }

    return onOccurrence(null)
}

private inline fun <reified T : IdeaModuleInfo> collectModuleInfosByType(project: Project, virtualFile: VirtualFile): Collection<T> {
    val result = linkedSetOf<T>()
    collectInfosByVirtualFile(project, virtualFile, treatAsLibrarySource = false) {
        result.addIfNotNull(it as? T)
    }

    return result
}

private fun OrderEntry.toIdeaModuleInfo(
        project: Project,
        virtualFile: VirtualFile,
        treatAsLibrarySource: Boolean = false
): IdeaModuleInfo? {
    if (!isValid) return null

    when (this) {
        is LibraryOrderEntry -> {
            val library = library ?: return null
            if (ProjectRootsUtil.isLibraryClassFile(project, virtualFile) && !treatAsLibrarySource) {
                return LibraryInfo(project, library)
            }
            else if (ProjectRootsUtil.isLibraryFile(project, virtualFile) || treatAsLibrarySource) {
                return LibrarySourceInfo(project, library)
            }
        }
        is JdkOrderEntry -> {
            return SdkInfo(project, jdk ?: return null)
        }
        else -> return null
    }
    return null
}