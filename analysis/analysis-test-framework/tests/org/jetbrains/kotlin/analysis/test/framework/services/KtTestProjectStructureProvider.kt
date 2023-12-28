/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

class KtTestProjectStructureProvider(
    override val globalLanguageVersionSettings: LanguageVersionSettings,
    private val builtinsModule: KtBuiltinsModule,
    private val projectStructure: KtModuleProjectStructure,
) : KtStaticProjectStructureProvider() {
    override fun getNotUnderContentRootModule(project: Project): KtNotUnderContentRootModule {
        error("Not-under content root modules most be initialized explicitly in tests")
    }

    @OptIn(KtModuleStructureInternals::class)
    override fun getModule(element: PsiElement, contextualModule: KtModule?): KtModule {
        val containingFile = element.containingFile
        val virtualFile = containingFile.virtualFile

        if (virtualFile != null) {
            if (virtualFile.extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION) {
                return builtinsModule
            }

            projectStructure.binaryModules
                .firstOrNull { binaryModule -> virtualFile in binaryModule.contentScope }
                ?.let { return it }
        }

        computeSpecialModule(containingFile)?.let { return it }

        return projectStructure.mainModules.firstOrNull { module ->
            element in module.ktModule.contentScope
        }?.ktModule
            ?: throw KotlinExceptionWithAttachments("Cannot find KtModule; see the attachment for more details.")
                .withAttachment(
                    virtualFile?.path ?: containingFile.name,
                    allKtModules.joinToString(separator = System.lineSeparator()) { it.asDebugString() }
                )
    }

    override val allKtModules: List<KtModule> = projectStructure.allKtModules()

    override val allSourceFiles: List<PsiFileSystemItem> = projectStructure.allSourceFiles()
}