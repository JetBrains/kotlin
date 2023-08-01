/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

class KtStaticModuleProvider(
    private val builtinsModule: KtBuiltinsModule,
    private val projectStructure: KtModuleProjectStructure,
) : KtStaticProjectStructureProvider() {
    @OptIn(KtModuleStructureInternals::class)
    override fun getModule(element: PsiElement, contextualModule: KtModule?): KtModule {
        // Unwrap context-dependent file copies coming from dependent sessions
        val containingPsiFile = element.containingFile.originalFile

        val containingVirtualFile = containingPsiFile.virtualFile
        if (containingVirtualFile.extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION) {
            return builtinsModule
        }

        containingVirtualFile.analysisExtensionFileContextModule?.let { return it }

        projectStructure.binaryModules.firstOrNull { binaryModule ->
            containingVirtualFile in binaryModule.contentScope
        }?.let { return it }

        return projectStructure.mainModules.firstOrNull { module ->
            element in module.ktModule.contentScope
        }?.ktModule
            ?: throw KotlinExceptionWithAttachments("Cannot find KtModule; see the attachment for more details.")
                .withAttachment(
                    containingVirtualFile.path,
                    allKtModules.joinToString(separator = System.lineSeparator()) { it.asDebugString() }
                )
    }

    override val allKtModules: List<KtModule> = projectStructure.allKtModules()

    override val allSourceFiles: List<PsiFileSystemItem> = projectStructure.allSourceFiles()
}