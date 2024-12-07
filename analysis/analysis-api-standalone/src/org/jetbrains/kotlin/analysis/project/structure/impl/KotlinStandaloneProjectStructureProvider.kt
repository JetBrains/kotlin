/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.asDebugString
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KotlinStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory.findJvmRootsForJavaFiles
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirBuiltinsSessionFactory
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

internal class KotlinStandaloneProjectStructureProvider(
    private val platform: TargetPlatform,
    private val project: Project,
    override val allModules: List<KaModule>,
) : KotlinStaticProjectStructureProvider() {
    private val ktNotUnderContentRootModuleWithoutPsiFile by lazy {
        KaNotUnderContentRootModuleImpl(
            name = "unnamed-outside-content-root",
            moduleDescription = "Standalone-not-under-content-root-module-without-psi-file",
            project = project,
        )
    }

    private val builtinsModule: KaBuiltinsModule by lazy {
        @OptIn(LLFirInternals::class)
        LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform).ktModule as KaBuiltinsModule
    }

    override fun getNotUnderContentRootModule(project: Project): KaNotUnderContentRootModule {
        return ktNotUnderContentRootModuleWithoutPsiFile
    }

    override fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule {
        val containingFile = element.containingFile
            ?: return ktNotUnderContentRootModuleWithoutPsiFile

        val virtualFile = containingFile.virtualFile
        if (virtualFile != null && virtualFile.extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION) {
            return builtinsModule
        }

        computeSpecialModule(containingFile)?.let { return it }

        if (virtualFile == null) {
            throw KotlinExceptionWithAttachments("Cannot find a KaModule for a non-physical file")
                .withPsiAttachment("containingFile", containingFile)
                .withAttachment("useSiteModule", useSiteModule?.asDebugString())
        }

        return allModules.firstOrNull { module -> virtualFile in module.contentScope }
            ?: throw KotlinExceptionWithAttachments("Cannot find a KaModule for the VirtualFile")
                .withPsiAttachment("containingFile", containingFile)
                .withAttachment("useSiteModule", useSiteModule?.asDebugString())
                .withAttachment("path", virtualFile.path)
                .withAttachment("modules", allModules.joinToString(separator = System.lineSeparator()) { it.asDebugString() })
    }

    override fun getImplementingModules(module: KaModule): List<KaModule> {
        return allModules.filter { module in it.directDependsOnDependencies }
    }

    internal val binaryModules: List<KaLibraryModule> by lazy {
        allModules
            .flatMap { it.allDirectDependencies() }
            .filterIsInstance<KaLibraryModule>()
    }

    @OptIn(KaExperimentalApi::class)
    override val allSourceFiles: List<PsiFileSystemItem> by lazy {
        buildList {
            val files = allModules.mapNotNull { (it as? KaSourceModule)?.psiRoots }.flatten()
            addAll(files)
            addAll(findJvmRootsForJavaFiles(files.filterIsInstance<PsiJavaFile>()))
        }
    }
}
