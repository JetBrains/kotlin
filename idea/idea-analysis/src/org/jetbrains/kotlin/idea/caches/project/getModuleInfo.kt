/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.asJava.classes.FakeLightClassForFileOfPackage
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.core.isInTestSourceContentKotlinAware
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.core.script.scriptRelatedModuleName
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.isInSourceContentWithoutInjected
import org.jetbrains.kotlin.idea.util.isKotlinBinary
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.script.getScriptDefinition
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.kotlin.utils.yieldIfNotNull
import kotlin.coroutines.experimental.buildSequence

var PsiFile.moduleInfo: ModuleInfo? by UserDataProperty(Key.create("MODULE_INFO"))

fun PsiElement.getModuleInfo(): IdeaModuleInfo = this.collectInfos(ModuleInfoCollector.NotNullTakeFirst)

fun PsiElement.getNullableModuleInfo(): IdeaModuleInfo? = this.collectInfos(ModuleInfoCollector.NullableTakeFirst)

fun PsiElement.getModuleInfos(): Sequence<IdeaModuleInfo> = this.collectInfos(ModuleInfoCollector.ToSequence)

fun getModuleInfoByVirtualFile(project: Project, virtualFile: VirtualFile): IdeaModuleInfo? =
    collectInfosByVirtualFile(
        project, virtualFile,
        treatAsLibrarySource = false,
        onOccurrence = { return@getModuleInfoByVirtualFile it }
    )

fun getBinaryLibrariesModuleInfos(project: Project, virtualFile: VirtualFile) =
    collectModuleInfosByType<BinaryModuleInfo>(
        project,
        virtualFile
    )

fun getLibrarySourcesModuleInfos(project: Project, virtualFile: VirtualFile) =
    collectModuleInfosByType<LibrarySourceInfo>(
        project,
        virtualFile
    )

fun collectAllModuleInfosFromIdeaModel(project: Project, platform: TargetPlatform): List<IdeaModuleInfo> {
    val ideaModules = ModuleManager.getInstance(project).modules.toList()
    val modulesSourcesInfos = ideaModules.flatMap(Module::correspondingModuleInfos)

    //TODO: (module refactoring) include libraries that are not among dependencies of any module
    val ideaLibraries = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<LibraryOrderEntry>().map {
            it.library
        }
    }.filterNotNull().toSet()

    val librariesInfos = ideaLibraries.map { LibraryInfo(project, it) }

    val sdksFromModulesDependencies = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<JdkOrderEntry>().map {
            it.jdk
        }
    }

    val sdksInfos = (sdksFromModulesDependencies + getAllProjectSdks()).filterNotNull().toSet().map {
        SdkInfo(
            project,
            it
        )
    }

    return mergePlatformModules(modulesSourcesInfos, platform) + librariesInfos + sdksInfos
}

private fun mergePlatformModules(
    allModules: List<IdeaModuleInfo>,
    platform: TargetPlatform
): List<IdeaModuleInfo> {
    if (platform == TargetPlatform.Common) return allModules

    val platformModules =
        allModules.flatMap { module ->
            if (module is ModuleSourceInfo && module.platform == platform && module.expectedBy.isNotEmpty())
                listOf(module to module.expectedBy)
            else emptyList()
        }.map { (module, expectedBys) ->
            PlatformModuleInfo(module, expectedBys)
        }

    val rest = allModules - platformModules.flatMap { it.containedModules }
    return rest + platformModules
}


fun getScriptRelatedModuleInfo(project: Project, virtualFile: VirtualFile): ModuleSourceInfo? {
    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    val moduleRelatedModuleInfo = getModuleRelatedModuleInfo(projectFileIndex, virtualFile)
    if (moduleRelatedModuleInfo != null) {
        return moduleRelatedModuleInfo
    }

    if (ScratchFileService.isInScratchRoot(virtualFile)) {
        val scratchModule = virtualFile.scriptRelatedModuleName?.let { ModuleManager.getInstance(project).findModuleByName(it) }
        return scratchModule?.testSourceInfo() ?: scratchModule?.productionSourceInfo()
    }

    return null
}

internal fun getAllProjectSdks(): Collection<Sdk> {
    return ProjectJdkTable.getInstance().allJdks.toList()
}

private typealias VirtualFileProcessor<T> = (Project, VirtualFile, Boolean, Boolean) -> T

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
        virtualFileProcessor = processor@ { project, virtualFile, isLibrarySource, isScript ->
            collectInfosByVirtualFile(
                project,
                virtualFile,
                isLibrarySource,
                isScript,
                {
                    return@processor it ?: NotUnderContentRootModuleInfo
                })
        }
    )

    object NullableTakeFirst : ModuleInfoCollector<IdeaModuleInfo?>(
        onResult = { it },
        onFailure = { reason ->
            LOG.warn("Could not find correct module information.\nReason: $reason")
            null
        },
        virtualFileProcessor = processor@ { project, virtualFile, isLibrarySource, isScript ->
            collectInfosByVirtualFile(
                project,
                virtualFile,
                isLibrarySource,
                isScript,
                { return@processor it })
        }
    )

    object ToSequence : ModuleInfoCollector<Sequence<IdeaModuleInfo>>(
        onResult = { result -> result?.let { sequenceOf(it) } ?: emptySequence() },
        onFailure = { reason ->
            LOG.warn("Could not find correct module information.\nReason: $reason")
            emptySequence()
        },
        virtualFileProcessor = { project, virtualFile, isLibrarySource, isScript ->
            buildSequence {
                collectInfosByVirtualFile(
                    project,
                    virtualFile,
                    isLibrarySource,
                    isScript,
                    { yieldIfNotNull(it) })
            }
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

    val containingFile =
        containingFile ?: return c.onFailure("Analyzing element of type ${this::class.java} with no containing file\nText:\n$text")

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
        val context = containingKtFile.getContext()
                ?: return c.onFailure("Analyzing code fragment of type ${containingKtFile::class.java} with no context element\nText:\n${containingKtFile.getText()}")
        return context.collectInfos(c)
    }

    val virtualFile = containingFile.originalFile.virtualFile
            ?: return c.onFailure("Analyzing element of type ${this::class.java} in non-physical file $containingFile of type ${containingFile::class.java}\nText:\n$text")

    return c.virtualFileProcessor(
        project,
        virtualFile,
        (containingFile as? KtFile)?.isCompiled ?: false,
        containingKtFile?.isScript() ?: false
    )
}

private fun <T> KtLightElement<*, *>.processLightElement(c: ModuleInfoCollector<T>): T {
    val decompiledClass = this.getParentOfType<KtLightClassForDecompiledDeclaration>(strict = false)
    if (decompiledClass != null) {
        return c.virtualFileProcessor(
            project,
            containingFile.virtualFile.sure { "Decompiled class should be build from physical file" },
            false,
            (containingFile as? KtFile)?.isScript() ?: false
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
    project: Project,
    virtualFile: VirtualFile,
    treatAsLibrarySource: Boolean,
    isScript: Boolean = getScriptDefinition(virtualFile, project) != null,
    onOccurrence: (IdeaModuleInfo?) -> T
): T {
    if (isScript) {
        getScriptDefinition(virtualFile, project)?.let {
            onOccurrence(ScriptModuleInfo(project, virtualFile, it))
        }
    }

    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    val moduleRelatedModuleInfo = getModuleRelatedModuleInfo(projectFileIndex, virtualFile)
    if (moduleRelatedModuleInfo != null) {
        onOccurrence(moduleRelatedModuleInfo)
    }

    projectFileIndex.getOrderEntriesForFile(virtualFile).forEach {
        it.toIdeaModuleInfo(project, virtualFile, treatAsLibrarySource)?.let(onOccurrence)
    }

    val isBinary = virtualFile.fileType.isKotlinBinary()
    val scriptConfigurationManager = ScriptDependenciesManager.getInstance(project)
    if (isBinary && virtualFile in scriptConfigurationManager.getAllScriptsClasspathScope()) {
        if (treatAsLibrarySource) {
            onOccurrence(ScriptDependenciesSourceInfo.ForProject(project))
        } else {
            onOccurrence(ScriptDependenciesInfo.ForProject(project))
        }
    }
    if (!isBinary && virtualFile in scriptConfigurationManager.getAllLibrarySourcesScope()) {
        onOccurrence(ScriptDependenciesSourceInfo.ForProject(project))
    }

    return onOccurrence(null)
}

private fun getModuleRelatedModuleInfo(projectFileIndex: ProjectFileIndex, virtualFile: VirtualFile): ModuleSourceInfo? {
    val module = projectFileIndex.getModuleForFile(virtualFile)
    if (module != null && !module.isDisposed) {
        val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex
        if (moduleFileIndex.isInTestSourceContentKotlinAware(virtualFile)) {
            return module.testSourceInfo()
        } else if (moduleFileIndex.isInSourceContentWithoutInjected(virtualFile)) {
            return module.productionSourceInfo()
        }
    }
    return null
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
    if (this is ModuleOrderEntry) return null
    if (!isValid) return null

    when (this) {
        is LibraryOrderEntry -> {
            val library = library ?: return null
            if (ProjectRootsUtil.isLibraryClassFile(project, virtualFile) && !treatAsLibrarySource) {
                return LibraryInfo(project, library)
            } else if (ProjectRootsUtil.isLibraryFile(project, virtualFile) || treatAsLibrarySource) {
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
