/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.additionalArgumentsAsList
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.idea.quickfix.ExperimentalFixesFactory.fqNameIsExisting
import org.jetbrains.kotlin.idea.roots.invalidateProjectRoots
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker

class MakeModuleExperimentalFix(
    file: KtFile,
    private val module: Module,
    private val annotationFqName: FqName
) : KotlinQuickFixAction<KtFile>(file) {
    private val experimentalPrefix = if (module.toDescriptor()?.fqNameIsExisting(ExperimentalUsageChecker.REQUIRES_OPT_IN_FQ_NAME) == true)
        "opt-in"
    else
        "use-experimental"

    override fun getText(): String = KotlinBundle.message("add.0.to.module.1.compiler.arguments", compilerArgument, module.name)

    override fun getFamilyName(): String = KotlinBundle.message("add.an.opt.in.requirement.marker.compiler.argument")

    private val compilerArgument = "-X$experimentalPrefix=$annotationFqName"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val modelsProvider = IdeModifiableModelsProviderImpl(project)
        try {
            val facet = module.getOrCreateFacet(modelsProvider, useProjectSettings = false, commitModel = true)
            val facetSettings = facet.configuration.settings
            val compilerSettings = facetSettings.compilerSettings ?: CompilerSettings().also {
                facetSettings.compilerSettings = it
            }

            compilerSettings.additionalArguments += " $compilerArgument"
            facetSettings.updateMergedArguments()
            project.invalidateProjectRoots()
        } finally {
            modelsProvider.dispose()
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        if (module.getBuildSystemType() != BuildSystemType.JPS) return false
        val facet = KotlinFacet.get(module) ?: return true
        val facetSettings = facet.configuration.settings
        val compilerSettings = facetSettings.compilerSettings ?: return true
        return if (annotationFqName != ExperimentalUsageChecker.REQUIRES_OPT_IN_FQ_NAME && annotationFqName != ExperimentalUsageChecker.OLD_EXPERIMENTAL_FQ_NAME) {
            compilerArgument !in compilerSettings.additionalArgumentsAsList
        } else {
            compilerSettings.additionalArgumentsAsList.none {
                it.startsWith("-Xopt-in=") || it.startsWith("-Xuse-experimental=") || it.startsWith("-Xexperimental=")
            }
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val containingKtFile = diagnostic.psiElement.containingFile as? KtFile ?: return null
            val module = containingKtFile.module ?: return null
            return MakeModuleExperimentalFix(
                containingKtFile,
                module,
                ExperimentalUsageChecker.REQUIRES_OPT_IN_FQ_NAME.takeIf {
                    module.toDescriptor()?.fqNameIsExisting(it) == true
                } ?: ExperimentalUsageChecker.OLD_EXPERIMENTAL_FQ_NAME
            )
        }
    }
}
