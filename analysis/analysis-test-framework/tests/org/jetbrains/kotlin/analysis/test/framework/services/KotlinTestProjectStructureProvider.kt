/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.asDebugString
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KotlinStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModuleStructure
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

class KotlinTestProjectStructureProvider(
    override val globalLanguageVersionSettings: LanguageVersionSettings,
    private val builtinsModule: KaBuiltinsModule,
    private val ktTestModuleStructure: KtTestModuleStructure,
) : KotlinStaticProjectStructureProvider() {
    override fun getNotUnderContentRootModule(project: Project): KaNotUnderContentRootModule {
        error("Not-under content root modules most be initialized explicitly in tests")
    }

    override fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule {
        val containingFile = element.containingFile
        val virtualFile = containingFile.virtualFile

        if (virtualFile != null) {
            if (virtualFile.extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION) {
                return builtinsModule
            }

            ktTestModuleStructure.binaryModules
                .firstOrNull { binaryModule -> virtualFile in binaryModule.contentScope }
                ?.let { return it }
        }

        computeSpecialModule(containingFile)?.let { return it }

        return ktTestModuleStructure.mainModules.firstOrNull { module ->
            element in module.ktModule.contentScope
        }?.ktModule
            ?: throw KotlinExceptionWithAttachments("Cannot find KaModule; see the attachment for more details.")
                .withAttachment(
                    virtualFile?.path ?: containingFile.name,
                    allModules.joinToString(separator = System.lineSeparator()) { it.asDebugString() }
                )
    }

    override val allModules: List<KaModule> = ktTestModuleStructure.mainAndBinaryKtModules

    override val allSourceFiles: List<PsiFileSystemItem> = ktTestModuleStructure.allSourceFiles
}
