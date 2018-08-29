/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

class DivergedInferenceMigrationInspection : AbstractKotlinInspection(), MigrationFix {
    override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isInferenceUpdate()
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file !is KtFile) return null

        // TODO
        val languageVersionSettingsBefore = LanguageVersionSettingsImpl.DEFAULT
        val languageVersionSettingsAfter = LanguageVersionSettingsImpl.DEFAULT

        val resolutionBefore = buildResolutionSummary(file, languageVersionSettingsBefore)
        val resolutionAfter = buildResolutionSummary(file, languageVersionSettingsAfter)

        val resolutionDifference = compareResolutionSummaries(resolutionBefore, resolutionAfter)

        return resolutionDifference.toProblemsDescriptors()
    }

    private fun compareResolutionSummaries(resolutionBefore: ResolutionSummary, resolutionAfter: ResolutionSummary): ResolutionDifference {
        // TODO()
        return ResolutionDifference()
    }

    @Suppress("UNREACHABLE_CODE")
    private fun buildResolutionSummary(file: KtFile, languageVersionSettings: LanguageVersionSettings): ResolutionSummary {
        val analysisResult = file.analyzeWithGivenLanguageVersionSettings(languageVersionSettings)
        val (ir, symbolTable) = buildIr(file, analysisResult, languageVersionSettings = TODO())

        return ResolutionSummary(analysisResult, ir, symbolTable, file)
    }

    private fun KtFile.analyzeWithGivenLanguageVersionSettings(languageVersionSettings: LanguageVersionSettings): AnalysisResult {
        // TODO
        KotlinCacheService.getInstance(project).getResolutionFacade(listOf(this)).analyzeWithAllCompilerChecks(listOf(this)).bindingContext
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
