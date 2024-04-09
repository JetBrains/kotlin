/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.constant.AnnotationValue
import org.jetbrains.kotlin.constant.ErrorValue
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.ConstValueProviderImpl
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.unwrapOr
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.nameWithPackage
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

private const val stopEvaluation = "// STOP_EVALUATION_CHECKS"
private const val startEvaluation = "// START_EVALUATION_CHECKS"

interface EvaluatorHandler {
    val testServices: TestServices

    val globalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    fun TestFile.extractRangesWithoutRender(): List<Pair<Int, Int>> {
        val content = testServices.sourceFileProvider.getContentOfSourceFile(this)
        return buildList {
            var indexOfStop = -1
            do {
                indexOfStop = content.indexOf(stopEvaluation, indexOfStop + 1)
                if (indexOfStop < 0) break

                val indexOfStart = content.indexOf(startEvaluation, indexOfStop).takeIf { it != -1 } ?: content.length
                add(indexOfStop to indexOfStart)
            } while (true)
        }
    }

    fun TestFile.getExpectedResult(): List<ParsedCodeMetaInfo> {
        return globalMetadataInfoHandler.getExistingMetaInfosForFile(this)
    }
}

interface IrInterpreterDumpHandler : EvaluatorHandler {
    fun processIrModule(module: TestModule): Map<TestFile, List<ParsedCodeMetaInfo>> {
        if (!module.isSuppressedForK2() && testServices.defaultsProvider.defaultFrontend == FrontendKinds.ClassicFrontend) {
            return module.files.associateWith { testFile -> testFile.getExpectedResult() }
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val evaluatedConstTracker = configuration.get(CommonConfigurationKeys.EVALUATED_CONST_TRACKER)
            ?: error("Couldn't find `EVALUATED_CONST_TRACKER` for IR interpreter dump handler")
        val irModule = testServices.dependencyProvider.getArtifact(module, BackendKinds.IrBackend).irModuleFragment

        return buildMap {
            for ((irFile, testFile) in matchIrFileWithTestFile(irModule, module)) {
                putAll(evaluatedConstTracker.processFile(testFile, irFile))
            }
        }
    }

    fun TestModule.isSuppressedForK2(): Boolean {
        val ignoredBackends = this.directives[IGNORE_BACKEND_K2]
        val targetBackend = testServices.defaultsProvider.defaultTargetBackend ?: this.targetBackend
        return targetBackend in ignoredBackends || TargetBackend.ANY in ignoredBackends
    }

    private fun EvaluatedConstTracker.processFile(testFile: TestFile, irFile: IrFile): Map<TestFile, List<ParsedCodeMetaInfo>> {
        val resultMap = mutableMapOf<TestFile, MutableList<ParsedCodeMetaInfo>>()
        val rangesThatAreNotSupposedToBeRendered = testFile.extractRangesWithoutRender()
        this.load(irFile.nameWithPackage)?.forEach { (pair, constantValue) ->
            if (constantValue is AnnotationValue) return@forEach
            
            val (start, end) = pair
            if (rangesThatAreNotSupposedToBeRendered.any { start >= it.first && start <= it.second }) return@forEach

            val message = constantValue.stringTemplateValue()
            val metaInfo = ParsedCodeMetaInfo(
                start, end,
                attributes = mutableListOf(),
                tag = if (constantValue is ErrorValue) "WAS_NOT_EVALUATED" else "EVALUATED",
                description = StringUtil.escapeLineBreak(message)
            )

            resultMap.getOrPut(testFile) { mutableListOf() }.add(metaInfo)
        }

        return resultMap
    }
}

interface FirEvaluatorDumpHandler : EvaluatorHandler {
    fun processFirModule(module: TestModule, info: FirOutputArtifact): Map<TestFile, List<ParsedCodeMetaInfo>> {
        return buildMap {
            info.partsForDependsOnModules.forEach {
                it.firFiles.forEach { (testFile, firFile) ->
                    val intrinsicConstEvaluation = it.session.languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)
                    if (intrinsicConstEvaluation) {
                        put(testFile, testFile.getExpectedResult())
                    } else {
                        putAll(processFile(testFile, firFile, it.session))
                    }
                }
            }
        }
    }

    private fun processFile(testFile: TestFile, firFile: FirFile, session: FirSession): Map<TestFile, List<ParsedCodeMetaInfo>> {
        val resultMap = mutableMapOf<TestFile, MutableList<ParsedCodeMetaInfo>>()
        val rangesThatAreNotSupposedToBeRendered = testFile.extractRangesWithoutRender()

        fun render(result: FirElement, start: Int, end: Int) {
            if (rangesThatAreNotSupposedToBeRendered.any { start >= it.first && start <= it.second }) return
            if (result !is FirLiteralExpression<*>) return

            val message = result.value.toString()
            val metaInfo = ParsedCodeMetaInfo(
                start, end,
                attributes = mutableListOf(),
                tag = "EVALUATED",
                description = StringUtil.escapeLineBreak(message)
            )

            resultMap.getOrPut(testFile) { mutableListOf() }.add(metaInfo)
        }

        fun render(result: FirLiteralExpression<*>, source: KtSourceElement?) {
            val start = source?.startOffset ?: return
            val end = result.source?.endOffset ?: return
            render(result, start, end)
        }

        data class Options(val renderLiterals: Boolean)

        class EvaluateAndRenderExpressions : FirVisitor<Unit, Options>() {
            // This set is used to avoid double rendering
            private val visitedElements = mutableSetOf<FirElement>()

            override fun visitElement(element: FirElement, data: Options) {
                element.acceptChildren(this, data)
            }

            override fun <T> visitLiteralExpression(literalExpression: FirLiteralExpression<T>, data: Options) {
                if (!data.renderLiterals) return
                render(literalExpression, literalExpression.source)
            }

            override fun visitProperty(property: FirProperty, data: Options) {
                if (property in visitedElements) return
                visitedElements.add(property)

                super.visitProperty(property, data)
                property.evaluatedInitializer?.unwrapOr<FirExpression> { }?.let { result ->
                    with(ConstValueProviderImpl) {
                        val (start, end) = property.initializer?.getCorrespondingIrOffset() ?: return
                        render(result, start, end)
                    }
                }
            }

            override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Options) {
                if (annotationCall in visitedElements) return
                visitedElements.add(annotationCall)

                super.visitAnnotationCall(annotationCall, data)
                FirExpressionEvaluator.evaluateAnnotationArguments(annotationCall, session)?.values?.forEach { evaluated ->
                    evaluated.unwrapOr<FirExpression> { }?.accept(this, data.copy(renderLiterals = true))
                }
            }

            override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Options) {
                // Visit annotations on type arguments
                resolvedTypeRef.delegatedTypeRef?.accept(this, data)
                return super.visitResolvedTypeRef(resolvedTypeRef, data)
            }
        }

        firFile.accept(EvaluateAndRenderExpressions(), Options(renderLiterals = false))

        return resultMap
    }
}

interface FirAndIrDumpHandler: FirEvaluatorDumpHandler, IrInterpreterDumpHandler {
    fun processModule(module: TestModule) {
        val firArtifact = testServices.dependencyProvider.getArtifactSafe(module, FrontendKinds.FIR)
        val irMetaInfo = processIrModule(module)
        val firMetaInfo = firArtifact?.let { processFirModule(module, it) } ?: irMetaInfo

        val commonMetaInfo = irMetaInfo.map { (irTestFile, irTestData) ->
            val firTestData = firMetaInfo[irTestFile] ?: return@map irTestFile to emptyList()
            val common = irTestData.filter { irMetaInfo ->
                firTestData.any { firMetaInfo ->
                    firMetaInfo.start == irMetaInfo.start && firMetaInfo.end == irMetaInfo.end && firMetaInfo.description == irMetaInfo.description
                }
            }
            irTestFile to common
        }.toMap()

        val irOnlyMetaInfo = irMetaInfo.map { (irTestFile, irTestData) ->
            val commonTestData = commonMetaInfo[irTestFile] ?: return@map irTestFile to irTestData
            val irOnly = irTestData.filter { irMetaInfo ->
                !commonTestData.contains(irMetaInfo)
            }
            irTestFile to irOnly
        }.toMap()

        val firOnlyMetaInfo = firMetaInfo.map { (firTestFile, firTestData) ->
            val commonTestData = commonMetaInfo[firTestFile] ?: return@map firTestFile to firTestData
            val firOnly = firTestData.filter { irMetaInfo ->
                !commonTestData.contains(irMetaInfo)
            }
            firTestFile to firOnly
        }.toMap()

        commonMetaInfo.forEach { (testFile, metaInfo) ->
            globalMetadataInfoHandler.addMetadataInfosForFile(testFile, metaInfo)
        }

        irOnlyMetaInfo.forEach { (testFile, metaInfo) ->
            globalMetadataInfoHandler.addMetadataInfosForFile(testFile, metaInfo.map { it.copy().apply { attributes.add("IR") } })
        }

        firOnlyMetaInfo.forEach { (testFile, metaInfo) ->
            globalMetadataInfoHandler.addMetadataInfosForFile(testFile, metaInfo.map { it.copy().apply { attributes.add("FIR") } })
        }
    }
}

class JvmIrInterpreterDumpHandler(testServices: TestServices) : FirAndIrDumpHandler, JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        processModule(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

class JsIrInterpreterDumpHandler(testServices: TestServices) : FirAndIrDumpHandler, JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        processModule(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

class WasmIrInterpreterDumpHandler(testServices: TestServices) : FirAndIrDumpHandler, WasmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        processModule(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

class KlibInterpreterDumpHandler(testServices: TestServices) : FirAndIrDumpHandler, KlibArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        if (JsEnvironmentConfigurator.isMainModule(module, testServices)) return
        processModule(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
