/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory1
import org.jetbrains.kotlin.checkers.utils.TypeOfCall
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.AbstractTwoAttributesMetaInfoProcessor
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull

class FirDiagnosticsHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    companion object {
        private val allowedKindsForDebugInfo = setOf(
            FirRealSourceElementKind,
            FirFakeSourceElementKind.DesugaredCompoundAssignment,
        )
    }

    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    private val diagnosticsService: DiagnosticsService
        get() = testServices.diagnosticsService

    override val directivesContainers: List<DirectivesContainer> =
        listOf(DiagnosticsDirectives)

    override val additionalServices: List<ServiceRegistrationData> =
        listOf(service(::DiagnosticsService))

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        val diagnosticsPerFile = info.firAnalyzerFacade.runCheckers()
        val lightTreeComparingModeEnabled = FirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in module.directives
        val lightTreeEnabled = FirDiagnosticsDirectives.USE_LIGHT_TREE in module.directives

        for (file in module.files) {
            val firFile = info.firFiles[file] ?: continue
            val diagnostics = diagnosticsPerFile[firFile] ?: continue
            val diagnosticsMetadataInfos = diagnostics.mapNotNull { diagnostic ->
                if (!diagnosticsService.shouldRenderDiagnostic(module, diagnostic.factory.name)) return@mapNotNull null
                // SYNTAX errors will be reported later
                if (diagnostic.factory == FirErrors.SYNTAX) return@mapNotNull null
                if (!diagnostic.isValid) return@mapNotNull null
                diagnostic.toMetaInfo(file, lightTreeEnabled, lightTreeComparingModeEnabled)
            }
            globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnosticsMetadataInfos)
            collectSyntaxDiagnostics(file, firFile, lightTreeEnabled, lightTreeComparingModeEnabled)
            collectDebugInfoDiagnostics(file, firFile, lightTreeEnabled, lightTreeComparingModeEnabled)
        }
    }

    private fun FirDiagnostic<*>.toMetaInfo(
        file: TestFile,
        lightTreeEnabled: Boolean,
        lightTreeComparingModeEnabled: Boolean,
        forceRenderArguments: Boolean = false
    ): FirDiagnosticCodeMetaInfo {
        val metaInfo = FirDiagnosticCodeMetaInfo(this, FirMetaInfoUtils.renderDiagnosticNoArgs)
        val shouldRenderArguments = forceRenderArguments || globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, metaInfo)
            .any { it.description != null }
        if (shouldRenderArguments) {
            metaInfo.replaceRenderConfiguration(FirMetaInfoUtils.renderDiagnosticWithArgs)
        }
        if (lightTreeComparingModeEnabled) {
            metaInfo.attributes += if (lightTreeEnabled) PsiLightTreeMetaInfoProcessor.LT else PsiLightTreeMetaInfoProcessor.PSI
        }
        return metaInfo
    }

    private fun collectSyntaxDiagnostics(
        testFile: TestFile,
        firFile: FirFile,
        lightTreeEnabled: Boolean,
        lightTreeComparingModeEnabled: Boolean
    ) {
        val metaInfos = if (firFile.psi != null) {
            AnalyzingUtils.getSyntaxErrorRanges(firFile.psi!!).map {
                FirErrors.SYNTAX.on(FirRealPsiSourceElement(it)).toMetaInfo(testFile, lightTreeEnabled, lightTreeComparingModeEnabled)
            }
        } else {
            collectLightTreeSyntaxErrors(firFile).map { node ->
                FirErrors.SYNTAX.on(node).toMetaInfo(testFile, lightTreeEnabled, lightTreeComparingModeEnabled)
            }
        }

        globalMetadataInfoHandler.addMetadataInfosForFile(testFile, metaInfos)
    }

    private fun collectDebugInfoDiagnostics(
        testFile: TestFile,
        firFile: FirFile,
        lightTreeEnabled: Boolean,
        lightTreeComparingModeEnabled: Boolean
    ) {
        val result = mutableListOf<FirDiagnostic<*>>()
        val diagnosedRangesToDiagnosticNames = globalMetadataInfoHandler.getExistingMetaInfosForFile(testFile).groupBy(
            keySelector = { it.start..it.end },
            valueTransform = { it.tag }
        ).mapValues { (_, it) -> it.toSet() }
        object : FirDefaultVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element is FirExpression) {
                    result.addIfNotNull(
                        createExpressionTypeDiagnosticIfExpected(
                            element, diagnosedRangesToDiagnosticNames
                        )
                    )
                }

                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: FirFunctionCall) {
                result.addIfNotNull(
                    createCallDiagnosticIfExpected(functionCall, functionCall.calleeReference, diagnosedRangesToDiagnosticNames)
                )

                super.visitFunctionCall(functionCall)
            }
        }.let(firFile::accept)
        globalMetadataInfoHandler.addMetadataInfosForFile(
            testFile,
            result.map { it.toMetaInfo(testFile, lightTreeEnabled, lightTreeComparingModeEnabled, forceRenderArguments = true) }
        )
    }

    fun createExpressionTypeDiagnosticIfExpected(
        element: FirExpression,
        diagnosedRangesToDiagnosticNames: Map<IntRange, Set<String>>
    ): FirDiagnosticWithParameters1<FirSourceElement, String>? =
        DebugInfoDiagnosticFactory1.EXPRESSION_TYPE.createDebugInfoDiagnostic(element, diagnosedRangesToDiagnosticNames) {
            element.typeRef.renderAsString((element as? FirExpressionWithSmartcast)?.originalType)
        }

    private fun FirTypeRef.renderAsString(originalTypeRef: FirTypeRef?): String {
        val type = coneTypeSafe<ConeKotlinType>() ?: return "Type is unknown"
        val rendered = type.renderForDebugInfo()
        val originalTypeRendered = originalTypeRef?.coneTypeSafe<ConeKotlinType>()?.renderForDebugInfo() ?: return rendered

        return "$originalTypeRendered & $rendered"
    }

    private fun createCallDiagnosticIfExpected(
        element: FirElement,
        reference: FirNamedReference,
        diagnosedRangesToDiagnosticNames: Map<IntRange, Set<String>>
    ): FirDiagnosticWithParameters1<FirSourceElement, String>? =
        DebugInfoDiagnosticFactory1.CALL.createDebugInfoDiagnostic(element, diagnosedRangesToDiagnosticNames) {
            val resolvedSymbol = (reference as? FirResolvedNamedReference)?.resolvedSymbol
            val fqName = resolvedSymbol?.fqNameUnsafe()
            Renderers.renderCallInfo(fqName, getTypeOfCall(reference, resolvedSymbol))
        }

    private fun DebugInfoDiagnosticFactory1.getPositionedElement(sourceElement: FirSourceElement): FirSourceElement {
        return if (this === DebugInfoDiagnosticFactory1.CALL
            && sourceElement.elementType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION
        ) {
            if (sourceElement is FirPsiSourceElement<*>) {
                val psi = (sourceElement.psi as KtDotQualifiedExpression).selectorExpression
                psi?.let { FirRealPsiSourceElement(it) } ?: sourceElement
            } else {
                val tree = sourceElement.treeStructure
                val selector = tree.selector(sourceElement.lighterASTNode)
                if (selector == null) {
                    sourceElement
                } else {
                    val startDelta = tree.getStartOffset(selector) - tree.getStartOffset(sourceElement.lighterASTNode)
                    val endDelta = tree.getEndOffset(selector) - tree.getEndOffset(sourceElement.lighterASTNode)
                    FirLightSourceElement(
                        selector, sourceElement.startOffset + startDelta, sourceElement.endOffset + endDelta, tree
                    )
                }
            }
        } else {
            sourceElement
        }
    }

    private inline fun DebugInfoDiagnosticFactory1.createDebugInfoDiagnostic(
        element: FirElement,
        diagnosedRangesToDiagnosticNames: Map<IntRange, Set<String>>,
        argument: () -> String,
    ): FirDiagnosticWithParameters1<FirSourceElement, String>? {
        val sourceElement = element.source ?: return null
        if (sourceElement.kind !in allowedKindsForDebugInfo) return null

        // Lambda argument is always (?) duplicated by function literal
        // Block expression is always (?) duplicated by single block expression
        if (sourceElement.elementType == KtNodeTypes.LAMBDA_ARGUMENT || sourceElement.elementType == KtNodeTypes.BLOCK) return null
        // Unfortunately I had to repeat positioning strategy logic here
        // (we need to check diagnostic range before applying it)
        val positionedElement = getPositionedElement(sourceElement)
        if (diagnosedRangesToDiagnosticNames[positionedElement.startOffset..positionedElement.endOffset]?.contains(this.name) != true) {
            return null
        }

        val argumentText = argument()
        val factory = FirDiagnosticFactory1<PsiElement, String>(name, severity)
        return when (positionedElement) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters1(positionedElement, argumentText, severity, factory)
            is FirLightSourceElement -> FirLightDiagnosticWithParameters1(positionedElement, argumentText, severity, factory)
        }
    }

    private fun getTypeOfCall(
        reference: FirNamedReference,
        resolvedSymbol: AbstractFirBasedSymbol<*>?
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
            is FirFunction<*> -> buildString {
                if (fir is FirCallableMemberDeclaration<*>) {
                    if (fir.status.isInline) append("inline ")
                    if (fir.status.isInfix) append("infix ")
                    if (fir.status.isOperator) append("operator ")
                    if (fir.receiverTypeRef != null) append("extension ")
                }
                append(TypeOfCall.FUNCTION.nameToRender)
            }
            else -> TypeOfCall.OTHER.nameToRender
        }
    }

    private fun AbstractFirBasedSymbol<*>.fqNameUnsafe(): FqNameUnsafe? = when (this) {
        is FirClassLikeSymbol<*> -> classId.asSingleFqName().toUnsafe()
        is FirCallableSymbol<*> -> callableId.asFqNameForDebugInfo().toUnsafe()
        else -> null
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
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
        return FirDiagnosticsDirectives.USE_LIGHT_TREE !in module.directives
    }
}

