/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.checkers.utils.TypeOfCall
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelineArtifact
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.pipeline.collectLostDiagnosticsOnFile
import org.jetbrains.kotlin.fir.pipeline.runCheckers
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.RenderingInternals
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.fir.util.Multimap
import org.jetbrains.kotlin.fir.util.listMultimapOf
import org.jetbrains.kotlin.fir.util.plusAssign
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.assertFileDoesntExist
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.METADATA_ONLY_COMPILATION
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.SEPARATE_KMP_COMPILATION
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.USE_LATEST_LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.frontend.fir.FirCliBasedOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputPartForDependsOnModule
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.AbstractTwoAttributesMetaInfoProcessor
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.DummyDelegate
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FullDiagnosticsRenderer(private val directive: SimpleDirective) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")

    fun assertCollectedDiagnostics(testServices: TestServices, expectedExtension: String) {
        val directives = testServices.moduleStructure.allDirectives
        if (USE_LATEST_LANGUAGE_VERSION in directives) return

        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.parentFile.resolve("${testDataFile.nameWithoutExtension.removeSuffix(".fir")}$expectedExtension")

        if (directive !in directives) {
            if (DiagnosticsDirectives.RENDER_ALL_DIAGNOSTICS_FULL_TEXT !in directives) {
                testServices.assertions.assertFileDoesntExist(expectedFile, directive)
            }
            return
        }
        if (dumper.isEmpty() && !expectedFile.exists()) {
            return
        }
        val resultDump = dumper.generateResultingDump()
        testServices.assertions.assertEqualsToFile(expectedFile, resultDump)
    }

    fun storeFullDiagnosticRender(module: TestModule, diagnostics: List<KtDiagnostic>, file: TestFile) {
        if (directive !in module.directives) return
        if (diagnostics.isEmpty()) return

        class DiagnosticData(val textRanges: List<TextRange>, val severity: String, val message: String)

        val reportedDiagnostics = diagnostics
            .map {
                DiagnosticData(
                    textRanges = when (it) {
                        is KtDiagnosticWithSource -> it.textRanges
                        is KtDiagnosticWithoutSource -> listOf(it.firstRange)
                    },
                    severity = AnalyzerWithCompilerReport.convertSeverity(it.severity).toString().toLowerCaseAsciiOnly(),
                    message = it.renderMessage()
                )
            }
            .sortedWith(compareBy<DiagnosticData> { it.textRanges.first().startOffset }.thenBy { it.message })

        dumper.builderForModule(module).appendLine(reportedDiagnostics.joinToString(separator = "\n\n") {
            "/${file.name}:${it.textRanges.first()}: ${it.severity}: ${it.message}"
        })
    }
}

@OptIn(SymbolInternals::class)
class FirDiagnosticsHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    private val diagnosticsService: DiagnosticsService
        get() = testServices.diagnosticsService

    override val directiveContainers: List<DirectivesContainer> =
        listOf(DiagnosticsDirectives)

    override val additionalServices: List<ServiceRegistrationData> =
        listOf(service(::DiagnosticsService), service(::FirDiagnosticCollectorService))

    override val additionalAfterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
        get() = listOf(::FirIdenticalChecker)

    private val fullDiagnosticsRenderer = FullDiagnosticsRenderer(DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT)

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        fullDiagnosticsRenderer.assertCollectedDiagnostics(testServices, ".fir.diag.txt")
    }

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        val frontendDiagnosticsPerFile = testServices.firDiagnosticCollectorService.getFrontendDiagnosticsForModule(info)

        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            val lightTreeComparingModeEnabled = FirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in currentModule.directives
            val lightTreeEnabled = currentModule.directives.singleValue(FirDiagnosticsDirectives.FIR_PARSER) == FirParser.LightTree
            val forceRenderArguments = FirDiagnosticsDirectives.RENDER_DIAGNOSTICS_MESSAGES in currentModule.directives

            for (file in currentModule.files) {
                val firFile = info.mainFirFiles[file] ?: continue
                var diagnostics = frontendDiagnosticsPerFile[firFile]
                if (AdditionalFilesDirectives.CHECK_TYPE in currentModule.directives) {
                    diagnostics = diagnostics.filter { it.diagnostic.factory.name != FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS.name }
                }
                if (LanguageSettingsDirectives.API_VERSION in currentModule.directives) {
                    diagnostics = diagnostics.filter { it.diagnostic.factory.name != FirErrors.NEWER_VERSION_IN_SINCE_KOTLIN.name }
                }
                val diagnosticsMetadataInfos = diagnostics
                    .groupBy({ it.kmpCompilationMode }, { it.diagnostic })
                    .flatMap { (kmpCompilation, diagnostics) ->
                        diagnostics.diagnosticCodeMetaInfos(
                            currentModule, file,
                            diagnosticsService, globalMetadataInfoHandler,
                            lightTreeEnabled, lightTreeComparingModeEnabled,
                            forceRenderArguments,
                            kmpCompilation
                        )
                    }
                globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnosticsMetadataInfos)
                collectDebugInfoDiagnostics(currentModule, file, firFile, lightTreeEnabled, lightTreeComparingModeEnabled)
                fullDiagnosticsRenderer.storeFullDiagnosticRender(module, diagnostics.map { it.diagnostic }, file)
            }
        }
    }

    private fun collectDebugInfoDiagnostics(
        module: TestModule,
        testFile: TestFile,
        firFile: FirFile,
        lightTreeEnabled: Boolean,
        lightTreeComparingModeEnabled: Boolean
    ) {
        val result = mutableListOf<KtDiagnostic>()

        val diagnosedRangesToDiagnosticNames = globalMetadataInfoHandler.getExistingMetaInfosForFile(testFile)
            .groupBy(keySelector = { it.start..it.end }, valueTransform = { it.tag })
            .mapValues { (_, value) -> value.toSet() }

        val consumer = DebugDiagnosticConsumer(result, diagnosedRangesToDiagnosticNames)
        val shouldRenderDynamic = DiagnosticsDirectives.MARK_DYNAMIC_CALLS in module.directives

        object : FirDefaultVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element is FirExpression) {
                    consumer.reportExpressionTypeDiagnostic(element)
                }
                if (shouldRenderDynamic && element is FirResolvable) {
                    reportDynamic(element)
                }
                if (element is FirSmartCastExpression) {
                    element.originalExpression.acceptChildren(this)
                } else {
                    element.acceptChildren(this)
                }
            }

            private fun reportDynamic(element: FirResolvable) {
                val calleeDeclaration = element.calleeReference.toResolvedCallableSymbol() ?: return
                val isInvokeCallWithDynamicReceiver = calleeDeclaration.name == OperatorNameConventions.INVOKE
                        && element is FirQualifiedAccessExpression
                        && element.dispatchReceiver?.resolvedType?.isFunctionTypeWithDynamicReceiver(firFile.moduleData.session) == true

                if (calleeDeclaration.origin !is FirDeclarationOrigin.DynamicScope && !isInvokeCallWithDynamicReceiver) {
                    return
                }

                val source = element.calleeReference.source

                // Unfortunately I had to repeat positioning strategy logic here
                // (we need to check diagnostic range before applying it)
                val target = when (calleeDeclaration.name) {
                    OperatorNameConventions.INVOKE -> when {
                        isInvokeCallWithDynamicReceiver -> source
                        else -> source?.parentAsSourceElement ?: source
                    }
                    in OperatorNameConventions.ALL_BINARY_OPERATION_NAMES,
                    in OperatorNameConventions.UNARY_OPERATION_NAMES,
                    in OperatorNameConventions.ASSIGNMENT_OPERATIONS,
                    OperatorNameConventions.GET, OperatorNameConventions.SET -> {
                        source?.operatorSignIfBinary ?: source
                    }
                    else -> {
                        source
                    }
                }

                consumer.reportDynamicDiagnostic(target)
            }

            override fun visitFunctionCall(functionCall: FirFunctionCall) {
                val reference = functionCall.calleeReference
                consumer.reportCallDiagnostic(functionCall, reference)
                consumer.reportContainingClassDiagnostic(functionCall, reference)

                super.visitFunctionCall(functionCall)
            }

            override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression) {
                val selector = safeCallExpression.selector
                if (selector is FirQualifiedAccessExpression) {
                    val reference = selector.calleeReference as FirNamedReference
                    consumer.reportCallDiagnostic(safeCallExpression, reference)
                    consumer.reportContainingClassDiagnostic(safeCallExpression, reference)
                }

                super.visitSafeCallExpression(safeCallExpression)
            }

            override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
                val reference = propertyAccessExpression.calleeReference
                consumer.reportContainingClassDiagnostic(propertyAccessExpression, reference)

                super.visitPropertyAccessExpression(propertyAccessExpression)
            }

            override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall) {
                val reference = delegatedConstructorCall.calleeReference as FirNamedReference
                consumer.reportContainingClassDiagnostic(delegatedConstructorCall, reference)

                super.visitDelegatedConstructorCall(delegatedConstructorCall)
            }
        }.let(firFile::accept)

        val codeMetaInfos = result.flatMap { diagnostic ->
            diagnostic.toMetaInfos(
                module,
                testFile,
                globalMetadataInfoHandler,
                lightTreeEnabled,
                lightTreeComparingModeEnabled,
                forceRenderArguments = true
            )
        }

        globalMetadataInfoHandler.addMetadataInfosForFile(testFile, codeMetaInfos)
    }

    private fun DebugDiagnosticConsumer.reportExpressionTypeDiagnostic(element: FirExpression) {
        report(KtDebugInfoDiagnostics.EXPRESSION_TYPE, element) {
            element.resolvedType.renderForDebugInfo()
        }
    }

    private fun DebugDiagnosticConsumer.reportCallDiagnostic(element: FirElement, reference: FirNamedReference) {
        report(KtDebugInfoDiagnostics.CALL, element) {
            val resolvedSymbol = (reference as? FirResolvedNamedReference)?.resolvedSymbol
            val fqName = resolvedSymbol?.fqNameUnsafe()
            Renderers.renderCallInfo(fqName, getTypeOfCall(reference, resolvedSymbol))
        }
    }

    @OptIn(RenderingInternals::class)
    private fun DebugDiagnosticConsumer.reportContainingClassDiagnostic(element: FirElement, reference: FirNamedReference) {
        report(KtDebugInfoDiagnostics.CALLABLE_OWNER, element) {
            val resolvedSymbol = (reference as? FirResolvedNamedReference)?.resolvedSymbol
            val callable = resolvedSymbol?.fir as? FirCallableDeclaration ?: return@report ""
            buildString {
                append(callable.symbol.callableIdForRendering.asFqNameForDebugInfo().asString())
                append(" in ")
                if (callable.containingClassForStaticMemberAttr == null) {
                    append("implicit ")
                }
                append(callable.containingClassLookupTag()?.classId?.asFqNameString() ?: "<unknown>")
            }
        }
    }

    private fun DebugDiagnosticConsumer.reportDynamicDiagnostic(sourceElement: KtSourceElement?) {
        report(KtDebugInfoDiagnostics.DYNAMIC, sourceElement)
    }

    private fun getTypeOfCall(
        reference: FirNamedReference,
        resolvedSymbol: FirBasedSymbol<*>?
    ): String {
        if (resolvedSymbol == null) return TypeOfCall.UNRESOLVED.nameToRender

        if ((resolvedSymbol as? FirFunctionSymbol)?.callableId?.callableName == OperatorNameConventions.INVOKE
            && reference.name != OperatorNameConventions.INVOKE
        ) {
            return TypeOfCall.VARIABLE_THROUGH_INVOKE.nameToRender
        }

        return when (val fir = resolvedSymbol.fir) {
            is FirProperty -> {
                TypeOfCall.PROPERTY_GETTER.nameToRender
            }
            is FirFunction -> buildString {
                if (fir.status.isInline) append("inline ")
                if (fir.status.isInfix) append("infix ")
                if (fir.status.isOperator) append("operator ")
                if (fir.receiverParameter != null) append("extension ")
                append(TypeOfCall.FUNCTION.nameToRender)
            }
            else -> TypeOfCall.OTHER.nameToRender
        }
    }

    private fun FirBasedSymbol<*>.fqNameUnsafe(): FqNameUnsafe? = when (this) {
        is FirClassLikeSymbol<*> -> classId.asSingleFqName().toUnsafe()
        is FirCallableSymbol<*> -> callableId?.asFqNameForDebugInfo()?.toUnsafe()
        else -> null
    }
}

fun List<KtDiagnostic>.diagnosticCodeMetaInfos(
    module: TestModule,
    file: TestFile,
    diagnosticsService: DiagnosticsService,
    globalMetadataInfoHandler: GlobalMetadataInfoHandler,
    lightTreeEnabled: Boolean,
    lightTreeComparingModeEnabled: Boolean,
    forceRenderArguments: Boolean = false,
    kmpCompilationMode: KmpCompilationMode? = null
): List<FirDiagnosticCodeMetaInfo> = flatMap { diagnostic ->
    if (!diagnosticsService.shouldRenderDiagnostic(
            module,
            diagnostic.factory.name,
            diagnostic.severity
        )
    ) return@flatMap emptyList()
    diagnostic.toMetaInfos(
        module,
        file,
        globalMetadataInfoHandler,
        lightTreeEnabled,
        lightTreeComparingModeEnabled,
        forceRenderArguments,
        kmpCompilationMode
    )
}

private fun ConeKotlinType.isFunctionTypeWithDynamicReceiver(session: FirSession): Boolean {
    val hasExplicitDynamicReceiver = receiverType(session) is ConeDynamicType
    val hasImplicitDynamicReceiver = isExtensionFunctionType && this.typeArguments.firstOrNull()?.type is ConeDynamicType
    return hasExplicitDynamicReceiver || hasImplicitDynamicReceiver
}

private val KtSourceElement.parentAsSourceElement: KtSourceElement?
    get() = when (elementType) {
        KtNodeTypes.REFERENCE_EXPRESSION -> when (this) {
            is KtPsiSourceElement -> psi.parent.toKtPsiSourceElement(kind)
            is KtLightSourceElement -> treeStructure.getParent(lighterASTNode)?.toKtLightSourceElement(treeStructure, kind)
        }
        else -> null
    }

private val KtSourceElement.operatorSignIfBinary: KtSourceElement?
    get() = when (elementType) {
        KtNodeTypes.BINARY_EXPRESSION -> when (this) {
            is KtPsiSourceElement -> (psi as? KtBinaryExpression)?.operationReference?.toKtPsiSourceElement(kind)
            is KtLightSourceElement -> treeStructure.findChildByType(lighterASTNode, KtNodeTypes.OPERATION_REFERENCE)
                ?.toKtLightSourceElement(treeStructure, kind)
        }
        else -> null
    }

private class DebugDiagnosticConsumer(
    private val result: MutableList<KtDiagnostic>,
    private val diagnosedRangesToDiagnosticNames: Map<IntRange, Set<String>>
) {
    private companion object {
        private val allowedKindsForDebugInfo = setOf(
            KtRealSourceElementKind,
            KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess,
            KtFakeSourceElementKind.SmartCastExpression,
            KtFakeSourceElementKind.DelegatingConstructorCall,
            KtFakeSourceElementKind.ArrayAccessNameReference,
            KtFakeSourceElementKind.ArrayIndexExpressionReference,
            KtFakeSourceElementKind.DesugaredPlusAssign,
            KtFakeSourceElementKind.DesugaredMinusAssign,
            KtFakeSourceElementKind.DesugaredTimesAssign,
            KtFakeSourceElementKind.DesugaredDivAssign,
            KtFakeSourceElementKind.DesugaredRemAssign,
            KtFakeSourceElementKind.DesugaredPrefixDec,
            KtFakeSourceElementKind.DesugaredPrefixInc,
            KtFakeSourceElementKind.DesugaredPostfixDec,
            KtFakeSourceElementKind.DesugaredPostfixInc
        )
    }

    fun report(factory: KtDiagnosticFactory0, sourceElement: KtSourceElement?) {
        if (sourceElement == null || sourceElement.kind !in allowedKindsForDebugInfo) return

        // Lambda argument is always (?) duplicated by function literal
        // Block expression is always (?) duplicated by single block expression
        if (sourceElement.elementType == KtNodeTypes.LAMBDA_ARGUMENT || sourceElement.elementType == KtNodeTypes.BLOCK) return

        val availableDiagnostics = diagnosedRangesToDiagnosticNames[sourceElement.startOffset..sourceElement.endOffset]
        if (availableDiagnostics == null || factory.name !in availableDiagnostics) {
            return
        }

        val diagnostic = when (sourceElement) {
            is KtPsiSourceElement -> KtPsiSimpleDiagnostic(
                sourceElement,
                factory.severity,
                factory,
                factory.defaultPositioningStrategy
            )
            is KtLightSourceElement -> KtLightSimpleDiagnostic(
                sourceElement,
                factory.severity,
                factory,
                factory.defaultPositioningStrategy
            )
        }

        result.add(diagnostic)
    }

    fun report(factory: KtDiagnosticFactory1<String>, element: FirElement, argumentFactory: () -> String) {
        val sourceElement = element.source?.takeIf { it.kind in allowedKindsForDebugInfo } ?: return

        // Lambda argument is always (?) duplicated by function literal
        // Block expression is always (?) duplicated by single block expression
        if (sourceElement.elementType == KtNodeTypes.LAMBDA_ARGUMENT || sourceElement.elementType == KtNodeTypes.BLOCK) return

        // Unfortunately I had to repeat positioning strategy logic here
        // (we need to check diagnostic range before applying it)
        val positionedElement = factory.getPositionedElement(sourceElement)

        val availableDiagnostics = diagnosedRangesToDiagnosticNames[positionedElement.startOffset..positionedElement.endOffset]
        if (availableDiagnostics == null || factory.name !in availableDiagnostics) {
            return
        }

        val diagnostic = when (positionedElement) {
            is KtPsiSourceElement -> KtPsiDiagnosticWithParameters1(
                positionedElement,
                argumentFactory(),
                factory.severity,
                factory,
                factory.defaultPositioningStrategy
            )
            is KtLightSourceElement -> KtLightDiagnosticWithParameters1(
                positionedElement,
                argumentFactory(),
                factory.severity,
                factory,
                factory.defaultPositioningStrategy
            )
        }

        result.add(diagnostic)
    }

    private fun KtDiagnosticFactory1<String>.getPositionedElement(sourceElement: KtSourceElement): KtSourceElement {
        val elementType = sourceElement.elementType
        return if (this === KtDebugInfoDiagnostics.CALL
            && (elementType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION || elementType == KtNodeTypes.SAFE_ACCESS_EXPRESSION)
        ) {
            if (sourceElement is KtPsiSourceElement) {
                val psi = (sourceElement.psi as KtQualifiedExpression).selectorExpression
                psi?.let { KtRealPsiSourceElement(it) } ?: sourceElement
            } else {
                val tree = sourceElement.treeStructure
                val selector = tree.selector(sourceElement.lighterASTNode)
                if (selector == null) {
                    sourceElement
                } else {
                    val startDelta = tree.getStartOffset(selector) - tree.getStartOffset(sourceElement.lighterASTNode)
                    val endDelta = tree.getEndOffset(selector) - tree.getEndOffset(sourceElement.lighterASTNode)
                    KtLightSourceElement(
                        selector, sourceElement.startOffset + startDelta, sourceElement.endOffset + endDelta, tree
                    )
                }
            }
        } else {
            sourceElement
        }
    }
}

class PsiLightTreeMetaInfoProcessor(testServices: TestServices) : AbstractTwoAttributesMetaInfoProcessor(testServices) {
    companion object {
        const val PSI = "PSI"
        const val LT = "LT" // Light Tree
    }

    override val firstAttribute: String get() = PSI
    override val secondAttribute: String get() = LT

    override fun processorEnabled(module: TestModule): Boolean {
        return FirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in module.directives
    }

    override fun firstAttributeEnabled(module: TestModule): Boolean {
        return module.directives.singleValue(FirDiagnosticsDirectives.FIR_PARSER) == FirParser.Psi
    }
}

fun KtDiagnostic.toMetaInfos(
    module: TestModule,
    file: TestFile,
    globalMetadataInfoHandler: GlobalMetadataInfoHandler,
    lightTreeEnabled: Boolean,
    lightTreeComparingModeEnabled: Boolean,
    forceRenderArguments: Boolean = false,
    kmpCompilationMode: KmpCompilationMode? = null
): List<FirDiagnosticCodeMetaInfo> {
    val ranges = when (this) {
        is KtDiagnosticWithSource -> textRanges
        is KtDiagnosticWithoutSource -> listOf(firstRange)
    }
    return ranges.map { range ->
        val metaInfo = FirDiagnosticCodeMetaInfo(this, FirMetaInfoUtils.renderDiagnosticNoArgs, range)
        val shouldRenderArguments = forceRenderArguments || globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, metaInfo)
            .any { it.description != null }
        if (shouldRenderArguments) {
            metaInfo.replaceRenderConfiguration(FirMetaInfoUtils.renderDiagnosticWithArgs)
        }
        if (lightTreeComparingModeEnabled) {
            metaInfo.attributes += if (lightTreeEnabled) PsiLightTreeMetaInfoProcessor.LT else PsiLightTreeMetaInfoProcessor.PSI
        }
        if (file !in module.files) {
            val targetPlatform = module.targetPlatform(globalMetadataInfoHandler.testServices)
            metaInfo.attributes += when {
                targetPlatform.isJvm() -> "JVM"
                targetPlatform.isJs() -> "JS"
                targetPlatform.isNative() -> "NATIVE"
                targetPlatform.isCommon() -> "COMMON"
                else -> error("Should not be here")
            }
        }
        if (SEPARATE_KMP_COMPILATION in module.directives && kmpCompilationMode == KmpCompilationMode.PLATFORM) {
            metaInfo.attributes += FirDiagnosticCodeMetaRenderConfiguration.PLATFORM_TAG
        }
        if (METADATA_ONLY_COMPILATION !in module.directives && kmpCompilationMode == KmpCompilationMode.METADATA) {
            metaInfo.attributes += FirDiagnosticCodeMetaRenderConfiguration.METADATA_TAG
        }
        metaInfo
    }
}

typealias DiagnosticsMap = Multimap<FirFile, DiagnosticWithKmpCompilationMode, List<DiagnosticWithKmpCompilationMode>>

data class DiagnosticWithKmpCompilationMode(val diagnostic: KtDiagnostic, val kmpCompilationMode: KmpCompilationMode)

/**
 * There are two types of checkers (represented by [MppCheckerKind]):
 * 1. Common checker. When a common checker analyzes a code, the checker doesn't see what are the actualizations for the `expect` declarations.
 * 2. Platform checker. When a platform checker analyzes a code, the checker sees what are the actualizations for the `expect` declarations
 *    instead of the `expect` declarations themselves.
 *
 * KMP is compiled in two different modes (represented by [KmpCompilationMode]):
 * 1. Metadata compilation. Metadata compilation compiles only non-platform fragments,
 *    and it runs both common and platform checkers on those non-platform fragments.
 *    But in testData, we only check diagnostics from platform checkers in non-platform fragments.
 *    Common checkers in non-platform targets are tested by "platform compilation" anyway
 * 2. Platform compilation. Platform compilation compiles all the fragments (non-platform and platform),
 *    and it runs common checkers on non-platform fragments,
 *    and it runs platform checkers on platform fragments
 *
 * Please don't confuse "platform checker" and "platform compilation"
 */
enum class KmpCompilationMode {
    METADATA, PLATFORM, LOW_LEVEL_API
}

open class FirDiagnosticCollectorService(val testServices: TestServices) : TestService {
    val reporterForLTSyntaxErrors = SimpleDiagnosticsCollector(BaseDiagnosticsCollector.RawReporter.DO_NOTHING)

    private val cache: MutableMap<FirOutputArtifact, DiagnosticsMap> = mutableMapOf()

    open fun getFrontendDiagnosticsForModule(info: FirOutputArtifact): DiagnosticsMap {
        return cache.getOrPut(info) { computeDiagnostics(info) }
    }

    val containsErrorDiagnostics: Boolean
        get() = cache.values.any { info ->
            info.values.any { it.diagnostic.severity == Severity.ERROR }
        }

    fun containsErrors(info: FirOutputArtifact): Boolean {
        return getFrontendDiagnosticsForModule(info).values.any { it.diagnostic.severity == Severity.ERROR }
    }

    private fun computeDiagnostics(info: FirOutputArtifact): ListMultimap<FirFile, DiagnosticWithKmpCompilationMode> {
        val allFiles = info.partsForDependsOnModules.flatMap { it.firFiles.values }
        val platformPart = info.partsForDependsOnModules.last()
        val lazyDeclarationResolver = platformPart.session.lazyDeclarationResolver
        val result = listMultimapOf<FirFile, DiagnosticWithKmpCompilationMode>()

        lazyDeclarationResolver.disableLazyResolveContractChecksInside {
            val configuration =
                testServices.compilerConfigurationProvider.getCompilerConfiguration(platformPart.module, CompilationStage.FIRST)
            val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

            fun processDiagnosticsFromCliPhase(diagnosticsCollector: BaseDiagnosticsCollector, mode: KmpCompilationMode) {
                val diagnosticsPerFirFile = buildMap {
                    for ((filePath, diagnostics) in diagnosticsCollector.diagnosticsByFilePath) {
                        if (filePath == null) continue
                        val firFile = allFiles.first { it.sourceFile?.path == filePath }
                        put(firFile, diagnostics)
                    }
                }
                result += diagnosticsPerFirFile.convertToTestDiagnostics(mode)
            }

            when (info) {
                is FirCliBasedOutputArtifact<*> -> {
                    val diagnosticsCollector = info.cliArtifact.diagnosticCollector
                    val mode = if (info.cliArtifact is MetadataFrontendPipelineArtifact) {
                        KmpCompilationMode.METADATA
                    } else {
                        KmpCompilationMode.PLATFORM
                    }
                    processDiagnosticsFromCliPhase(diagnosticsCollector, mode)
                }
                else -> {
                    result += platformPart.session.runCheckers(
                        platformPart.scopeSession,
                        allFiles,
                        DiagnosticReporterFactory.createPendingReporter(messageCollector),
                        mppCheckerKind = MppCheckerKind.Platform
                    ).convertToTestDiagnostics(KmpCompilationMode.PLATFORM)

                    for (part in info.partsForDependsOnModules) {
                        result += part.session.runCheckers(
                            part.scopeSession,
                            part.firFiles.values,
                            DiagnosticReporterFactory.createPendingReporter(messageCollector),
                            mppCheckerKind = MppCheckerKind.Common
                        ).convertToTestDiagnostics(KmpCompilationMode.PLATFORM)
                    }


                    for (part in info.partsForDependsOnModules) {
                        collectSyntaxDiagnostics(part, result)
                    }
                }
            }
            if (DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS !in testServices.defaultDirectives) {
                for (part in info.partsForDependsOnModules.dropLast(1)) {
                    part.session.turnOnMetadataCompilationAnalysisFlag {
                        result += part.session.runCheckers(
                            part.scopeSession,
                            part.firFiles.values,
                            DiagnosticReporterFactory.createPendingReporter(messageCollector),
                            mppCheckerKind = MppCheckerKind.Platform
                        ).convertToTestDiagnostics(KmpCompilationMode.METADATA)
                    }
                }
            }

            val lostDiagnostics = listMultimapOf<FirFile, DiagnosticWithKmpCompilationMode>()
            for (file in allFiles) {
                val diagnostics = result[file]
                if (diagnostics.none { it.diagnostic.severity == Severity.ERROR }) {
                    platformPart.session.collectLostDiagnosticsOnFile(
                        platformPart.scopeSession,
                        file,
                        DiagnosticReporterFactory.createPendingReporter(messageCollector)
                    ).forEach { lostDiagnostics.put(file, DiagnosticWithKmpCompilationMode(it, KmpCompilationMode.PLATFORM)) }
                }
            }
            for ((file, diagnostics) in lostDiagnostics) {
                diagnostics.forEach { result.put(file, it) }
            }
        }

        return result
    }

    private fun Map<FirFile, List<KtDiagnostic>>.convertToTestDiagnostics(
        mode: KmpCompilationMode
    ): Map<FirFile, List<DiagnosticWithKmpCompilationMode>> {
        return mapValues { entry -> entry.value.mapNotNull {
                runIf(it.isValid) {
                    DiagnosticWithKmpCompilationMode(it, mode)
                }
            }
        }
    }

    protected fun collectSyntaxDiagnostics(
        part: FirOutputPartForDependsOnModule,
        destination: ListMultimap<FirFile, DiagnosticWithKmpCompilationMode>,
    ) {
        for ((testFile, firFile) in part.firFiles) {
            val syntaxErrors = if (firFile.psi != null) {
                AnalyzingUtils.getSyntaxErrorRanges(firFile.psi!!).map {
                    @OptIn(InternalDiagnosticFactoryMethod::class)
                    FirSyntaxErrors.SYNTAX.on(
                        KtRealPsiSourceElement(it),
                        it.errorDescription,
                        positioningStrategy = null,
                        LanguageVersionSettingsImpl.DEFAULT, // syntax errors couldn't be suppressed anyway
                    )!!
                }
            } else {
                reporterForLTSyntaxErrors
                    .diagnosticsByFilePath["/${testFile.toLightTreeShortName()}"]
                    .orEmpty()
            }
            destination.putAll(
                firFile,
                syntaxErrors.map { DiagnosticWithKmpCompilationMode(it, KmpCompilationMode.PLATFORM) }
            )
        }
    }
}

@OptIn(SessionConfiguration::class)
private fun FirSession.turnOnMetadataCompilationAnalysisFlag(body: () -> Unit) {
    val originalLv = languageVersionSettings
    val lv = object : LanguageVersionSettings by originalLv {
        override fun <T> getFlag(flag: AnalysisFlag<T>): T =
            @Suppress("UNCHECKED_CAST") // UNCHECKED_CAST is fine because metadataCompilation is boolean flag
            if (flag == AnalysisFlags.metadataCompilation) true as T else originalLv.getFlag(flag)
    }
    register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(lv))
    try {
        body()
    } finally {
        register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(originalLv))
    }
}

val TestServices.firDiagnosticCollectorService: FirDiagnosticCollectorService by TestServices.testServiceAccessor()

private object KtDebugInfoDiagnostics : KtDiagnosticsContainer() {
    val DYNAMIC by debugInfo0()
    val EXPRESSION_TYPE by debugInfo1()
    val CALL by debugInfo1()
    val CALLABLE_OWNER by debugInfo1()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    private object Renderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DebugInfo") {
            it.put(DYNAMIC, "")
            it.put(EXPRESSION_TYPE, "{0}", TO_STRING)
            it.put(CALL, "{0}", TO_STRING)
            it.put(CALLABLE_OWNER, "{0}", TO_STRING)
        }
    }

    private fun debugInfo0() = object {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, KtDiagnosticFactory0> {
            return DummyDelegate(
                KtDiagnosticFactory0(
                    "DEBUG_INFO_${prop.name}",
                    Severity.INFO,
                    SourceElementPositioningStrategies.DEFAULT,
                    KtElement::class,
                    this@KtDebugInfoDiagnostics.getRendererFactory()
                )
            )
        }
    }

    private fun debugInfo1() = object {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, KtDiagnosticFactory1<String>> {
            return DummyDelegate(
                KtDiagnosticFactory1(
                    "DEBUG_INFO_${prop.name}",
                    Severity.INFO,
                    SourceElementPositioningStrategies.DEFAULT,
                    KtElement::class,
                    this@KtDebugInfoDiagnostics.getRendererFactory()
                )
            )
        }
    }
}
