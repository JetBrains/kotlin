/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory.findJvmRootsForJavaFiles
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirBuiltinsSessionFactory
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

internal class KtStandaloneProjectStructureProvider(
    private val platform: TargetPlatform,
    private val project: Project,
    override val allKtModules: List<KtModule>,
) : KtStaticProjectStructureProvider() {
    private val ktNotUnderContentRootModuleWithoutPsiFile by lazy {
        KtNotUnderContentRootModuleImpl(
            name = "unnamed-outside-content-root",
            moduleDescription = "Standalone-not-under-content-root-module-without-psi-file",
            project = project,
        )
    }

    private val builtinsModule: KtBuiltinsModule by lazy {
        LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform).ktModule as KtBuiltinsModule
    }

    override fun getNotUnderContentRootModule(project: Project): KtNotUnderContentRootModule {
        return ktNotUnderContentRootModuleWithoutPsiFile
    }

    @OptIn(KtModuleStructureInternals::class)
    override fun getModule(element: PsiElement, contextualModule: KtModule?): KtModule {
        val containingFile = element.containingFile
            ?: return ktNotUnderContentRootModuleWithoutPsiFile

        val virtualFile = containingFile.virtualFile
            ?: error("${containingFile.name} is not a physical file")

        if (virtualFile.extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION) {
            return builtinsModule
        }

        computeSpecialModule(containingFile)?.let { return it }

        return allKtModules.firstOrNull { module -> virtualFile in module.contentScope }
            ?: throw KotlinExceptionWithAttachments("Cannot find KtModule; see the attachment for more details.")
                .withAttachment(
                    virtualFile.path,
                    allKtModules.joinToString(separator = System.lineSeparator()) { it.asDebugString() }
                )
    }

    internal val binaryModules: List<KtBinaryModule> by lazy {
        allKtModules
            .flatMap { it.allDirectDependencies() }
            .filterIsInstance<KtBinaryModule>()
    }

    override val allSourceFiles: List<PsiFileSystemItem> by lazy {
        buildList {
            val files = allKtModules.mapNotNull { (it as? KtSourceModuleImpl)?.sourceRoots }.flatten()
            addAll(files)
            addAll(findJvmRootsForJavaFiles(files.filterIsInstance<PsiJavaFile>()))
        }
    }
}