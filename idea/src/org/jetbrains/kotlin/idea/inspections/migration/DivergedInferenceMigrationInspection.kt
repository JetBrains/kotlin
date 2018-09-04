/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfoWithGivenLanguageSettings
import org.jetbrains.kotlin.idea.caches.project.forcedModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

class DivergedInferenceMigrationInspection : AbstractKotlinInspection(), MigrationFix {
    override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isInferenceUpdate()
    }

    override fun checkFile(oldFile: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (oldFile !is KtFile) return null
        val oldModuleInfo = oldFile.getModuleInfo() as? ModuleSourceInfo ?: return null

        val languageVersionSettingsBefore = oldFile.module?.languageVersionSettings!!
        val languageVersionSettingsAfter = languageVersionSettingsBefore.wrapEnablingNewInference()

        val syntheticFileCopy = oldFile.copy() as KtFile
        syntheticFileCopy.forcedModuleInfo = ModuleSourceInfoWithGivenLanguageSettings(oldModuleInfo, languageVersionSettingsAfter)

        val resolutionBefore = buildResolutionSummary(oldFile, oldFile.getResolutionFacade())
        val resolutionAfter = buildResolutionSummary(syntheticFileCopy, syntheticFileCopy.getResolutionFacade())

        val resolutionDifference = compareResolutionSummaries(resolutionBefore, resolutionAfter)

        return resolutionDifference.toProblemsDescriptors()
    }

    private fun LanguageVersionSettings.wrapEnablingNewInference(): LanguageVersionSettings {
        return object : LanguageVersionSettings by this {
            override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
                if (feature == LanguageFeature.NewInference) LanguageFeature.State.ENABLED else getFeatureSupport(feature)

            override fun supportsFeature(feature: LanguageFeature): Boolean =
                if (feature == LanguageFeature.NewInference) true else supportsFeature(feature)
        }
    }

    private fun compareResolutionSummaries(resolutionBefore: ResolutionSummary, resolutionAfter: ResolutionSummary): ResolutionDifference {
        return ResolutionDifference()
    }

    private fun buildResolutionSummary(file: KtFile, resolutionFacade: ResolutionFacade): ResolutionSummary {
        val analysisResult = resolutionFacade.analyzeWithAllCompilerChecks(listOf(file))
        val (ir, symbolTable) = buildIr(file, analysisResult, languageVersionSettings = resolutionFacade.frontendService())

        return ResolutionSummary(analysisResult, ir, symbolTable, file)
    }

    private fun buildIr(
        sourceFile: KtFile,
        analysisResult: AnalysisResult,
        languageVersionSettings: LanguageVersionSettings
    ): Pair<IrElement, SymbolTable> {
        val psi2IrConfiguration = Psi2IrConfiguration(ignoreErrors = true)
        val generatorContext = GeneratorContext(
            psi2IrConfiguration,
            analysisResult.moduleDescriptor,
            analysisResult.bindingContext,
            languageVersionSettings
        )

        val irTranslator = Psi2IrTranslator(languageVersionSettings, psi2IrConfiguration)

        val irModuleFragment = irTranslator.generateModuleFragment(generatorContext, listOf(sourceFile))

        return irModuleFragment to generatorContext.symbolTable
    }
}

private class ResolutionDifference {
    fun toProblemsDescriptors(): Array<ProblemDescriptor>? {
        // TODO
        return null
    }
}

private class ResolutionSummary(
    val analysisResult: AnalysisResult,
    val irRoot: IrElement,
    val irSymbolsTable: SymbolTable,
    val sourceFile: KtFile
)

private fun MigrationInfo.isInferenceUpdate(): Boolean {
    return !oldLanguageVersion.supportsNewInference() && newLanguageVersion.supportsNewInference()
}

private fun LanguageVersion.supportsNewInference(): Boolean = this >= LanguageFeature.NewInference.sinceVersion!!
