/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

class MakeModuleExperimentalFix(
    file: KtFile,
    private val module: Module,
    private val annotationFqName: FqName
) : KotlinQuickFixAction<KtFile>(file) {
    override fun getText(): String = "Add '-Xuse-experimental=$annotationFqName' to module ${module.name} compiler arguments"

    override fun getFamilyName(): String = "Make module experimental"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val modelsProvider = IdeModifiableModelsProviderImpl(project)
        val facet = module.getOrCreateFacet(modelsProvider, useProjectSettings = false, commitModel = true)
        val facetSettings = facet.configuration.settings
        val compilerSettings = facetSettings.compilerSettings ?: CompilerSettings().also {
            facetSettings.compilerSettings = it
        }

        compilerSettings.additionalArguments += " -Xuse-experimental=$annotationFqName"
        facetSettings.updateMergedArguments()
    }
}