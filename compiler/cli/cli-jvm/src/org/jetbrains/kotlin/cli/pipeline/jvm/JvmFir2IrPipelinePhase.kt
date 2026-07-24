/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.jvm.FirDirectJavaActualDeclarationExtractor
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.isMaybeMainFunction
import org.jetbrains.kotlin.fir.java.findJvmNameValue
import org.jetbrains.kotlin.fir.java.findJvmStaticAnnotation
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object JvmFir2IrPipelinePhase : PipelinePhase<JvmFrontendPipelineArtifact, JvmFir2IrPipelineArtifact>(
    name = "JvmFir2IrPipelinePhase",
    preActions = setOf(PerformanceNotifications.TranslationToIrStarted),
    postActions = setOf(PerformanceNotifications.TranslationToIrFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JvmFrontendPipelineArtifact): JvmFir2IrPipelineArtifact? =
        executePhase(input, input.configuration.getCompilerExtensions(IrGenerationExtension))

    fun executePhase(input: JvmFrontendPipelineArtifact, irGenerationExtensions: List<IrGenerationExtension>): JvmFir2IrPipelineArtifact? {
        (val firResult = frontendOutput, val configuration, val environment, val sourceFiles) = input
        val fir2IrExtensions = JvmFir2IrExtensions()
        val fir2IrAndIrActualizerResult = firResult.convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            configuration,
            configuration.diagnosticsCollector,
            irGenerationExtensions
        )

        val mainClassFqName = runIf(configuration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            findMainClass(firResult.outputs.last().fir)
        }

        return JvmFir2IrPipelineArtifact(
            fir2IrAndIrActualizerResult,
            configuration,
            environment,
            sourceFiles,
            mainClassFqName,
        )
    }

    fun AllModulesFrontendOutput.convertToIrAndActualizeForJvm(
        fir2IrExtensions: Fir2IrExtensions,
        configuration: CompilerConfiguration,
        diagnosticsReporter: BaseDiagnosticsCollector,
        irGeneratorExtensions: Collection<IrGenerationExtension>,
    ): Fir2IrActualizedResult {
        val fir2IrConfiguration = Fir2IrConfiguration.forJvmCompilation(configuration, diagnosticsReporter)

        return convertToIrAndActualize(
            fir2IrExtensions,
            fir2IrConfiguration,
            irGeneratorExtensions,
            JvmIrMangler,
            FirJvmVisibilityConverter,
            DefaultBuiltIns.Instance,
            ::JvmIrTypeSystemContext,
            JvmIrSpecialAnnotationSymbolProvider(),
            if (configuration.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
                { emptyList() }
            } else {
                { listOfNotNull(FirDirectJavaActualDeclarationExtractor.initializeIfNeeded(it)) }
            },
        )
    }

    /**
     * Find a single class that contains a valid main function
     * The main function validity is determined by the [isMaybeMainFunction] and some additional check:
     * the function should either be top-level or a member of a non-anonymous object
     * If many main functions are found in the same file then the one with parameters is "chosen", so we do not consider it a conflict.
     * Otherwise, if many main functions are found in one or several files, no one is chosen and the function returns "null"
     */
    private fun findMainClass(fir: List<FirFile>): FqName? {
        val groupedMainFunctions = mutableMapOf<FirDeclaration, MutableList<FirNamedFunction>>()
        val visitor = FirMainClassFinder(groupedMainFunctions)
        fir.forEach { it.accept(visitor, it to null) }

        val singleGroup = groupedMainFunctions.asIterable().singleOrNull() ?: return null
        return when (val parent = singleGroup.key) {
            is FirFile -> {
                // if we have some parameterless mains and one main with parameters, it is considered valid
                if (singleGroup.value.size > 1 &&
                    singleGroup.value.count { it.valueParameters.isNotEmpty() || it.receiverParameter != null } > 1
                ) {
                    null
                } else {
                    PackagePartClassUtils.getPackagePartFqName(parent.packageFqName, parent.name)
                }
            }
            is FirRegularClass -> {
                if (singleGroup.value.size > 1) null
                else parent.classId.asSingleFqName()
            }
            else -> null
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private class FirMainClassFinder(
        private var groupedMainFunctions: MutableMap<FirDeclaration, MutableList<FirNamedFunction>>
    ) : FirVisitor<Unit, Pair<FirDeclaration, FirRegularClass?>>() {

        override fun visitElement(element: FirElement, parents: Pair<FirDeclaration, FirRegularClass?>) {}

        override fun visitFile(file: FirFile, parents: Pair<FirDeclaration, FirRegularClass?>) {
            file.acceptChildren(this, file to null)
        }

        override fun visitRegularClass(regularClass: FirRegularClass, parents: Pair<FirDeclaration, FirRegularClass?>) {
            if (!regularClass.isLocal) {
                regularClass.acceptChildren(this, regularClass to (if (regularClass.isCompanion) parents.first as? FirRegularClass else null))
            }
        }

        override fun visitNamedFunction(namedFunction: FirNamedFunction, parents: Pair<FirDeclaration, FirRegularClass?>) {

            if (!namedFunction.isMaybeMainFunction(
                    getPlatformName = { findJvmNameValue() },
                    isPlatformStatic = { findJvmStaticAnnotation() != null },
                )
            ) return

            val [parent, grandparent] = parents
            if (parent is FirRegularClass && parent.classKind != ClassKind.OBJECT) return

            groupedMainFunctions.getOrPut(grandparent ?: parent, defaultValue = { mutableListOf() }).add(namedFunction)
        }
    }
}
