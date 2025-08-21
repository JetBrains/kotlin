/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractGetOrBuildFirTest.Directives.SKIP_CONTAINMENT_CHECK
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.AnalysisInterruptedException
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.ErrorResistanceServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.FirRenderingOptions
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.firRenderingOptions
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.renderer.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.util.OperatorNameConventions

abstract class AbstractGetOrBuildFirTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        fun findElement(qualifierIndex: Int?): KtElement? {
            val qualifier = if (qualifierIndex != null) "$qualifierIndex" else ""
            val element = testServices.expressionMarkerProvider
                .getTopmostSelectedElementOfTypeByDirectiveOrNull(mainFile, mainModule, defaultType = KtElement::class, qualifier)
            return element as KtElement?
        }

        val isContainmentCheckActive = SKIP_CONTAINMENT_CHECK !in mainModule.testModule.directives

        val results = withResolutionFacade(mainFile) { resolutionFacade ->
            val elementsToAnalyze = sequence {
                val firstCandidate = findElement(qualifierIndex = null) ?: error("No selected element found")
                yield(firstCandidate)

                var index = 1
                while (true) {
                    val candidate = findElement(index) ?: break
                    yield(candidate)
                    index += 1
                }
            }.toList()

            val renderingOptions = testServices.firRenderingOptions
                .copy(renderKtText = elementsToAnalyze.size > 1)

            val firFile by lazy { resolutionFacade.getOrBuildFirFile(mainFile) }

            val results = mutableListOf<String>()

            for ((index, element) in elementsToAnalyze.withIndex()) {
                val firElement = intercept(index, mainModule.testModule) { element.getOrBuildFir(resolutionFacade) }

                if (firElement != null && isContainmentCheckActive && shouldPerformContainmentCheck(firElement)) {
                    check(isInside(firElement, firFile))
                }

                results += renderActualFir(
                    fir = firElement,
                    ktElement = element,
                    renderingOptions = renderingOptions,
                    firFile = mainFile.getOrBuildFirFile(resolutionFacade),
                )
            }

            return@withResolutionFacade results
        }

        val actual = if (results.size > 1) {
            results
                .withIndex()
                .joinToString(separator = "\n\n=====\n\n") { (index, result) -> "Analysis attempt #$index\n$result" }
        } else {
            results.single()
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    // PSI-to-FIR mapper creates some synthetic FIR expression outside the FIR tree.
    private fun shouldPerformContainmentCheck(firElement: FirElement): Boolean {
        return !firElement.isSyntheticStringPlus() && !firElement.isSyntheticBuiltInSuspendCall()
    }

    // Check `KtToFirMapping.checkStringLiteralFolderExpression`.
    private fun FirElement.isSyntheticStringPlus(): Boolean {
        return this is FirResolvedNamedReference
                && this.name == OperatorNameConventions.PLUS
                && this.psi is KtOperationReferenceExpression
    }

    // Check `KtToFirMapping.fakeCallToBuiltInSuspendOrNull`.
    private fun FirElement.isSyntheticBuiltInSuspendCall(): Boolean {
        return this is FirFunctionCall
                && calleeReference.name == StandardClassIds.Callables.suspend.callableName
                && arguments.singleOrNull() is FirAnonymousFunctionExpression
    }

    /**
     * Checks whether the found [target] element is inside the [file] that corresponds to the [KtFile].
     */
    private fun isInside(target: FirElement, file: FirFile): Boolean {
        var result = false

        file.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element == target) {
                    result = true
                } else if (!result) {
                    element.acceptChildren(this)

                    /** Delegated type references are not visited in [FirResolvedTypeRef.acceptChildren] */
                    (element as? FirResolvedTypeRef)?.delegatedTypeRef?.accept(this)
                }
            }
        })

        return result
    }

    protected open fun <T : Any> intercept(index: Int, testModule: TestModule, block: () -> T?): T? {
        return block()
    }

    private object Directives : SimpleDirectivesContainer() {
        val SKIP_CONTAINMENT_CHECK by directive("Do not check that a found child can be accessed from its containing file tree")
    }
}

internal fun renderActualFir(
    fir: FirElement?,
    ktElement: KtElement,
    renderingOptions: FirRenderingOptions,
    firFile: FirFile? = null,
): String = buildString {
    appendLine("KT element: ${ktElement::class.simpleName}")
    if (renderingOptions.renderKtText) {
        appendLine("KT element text:")
        appendLine(ktElement.text)
    }
    appendLine("FIR element: ${fir?.let { it::class.simpleName }}")
    appendLine("FIR source kind: ${fir?.source?.kind?.let { it::class.simpleName }}")
    if (renderingOptions.renderContainerSource)
        appendLine("FIR container source: ${fir.renderContainerSource()}")
    if (renderingOptions.renderKtFileName)
        appendLine("File name: ${ktElement.containingKtFile.name}")
    appendLine("\nFIR element rendered:")
    appendLine(render(fir).trimEnd())
    if (firFile != null) {
        appendLine("\nFIR FILE:")
        append(render(firFile).trimEnd())
    }
}

private fun render(firElement: FirElement?): String = when (firElement) {
    null -> "null"
    is FirImport -> "import ${firElement.importedFqName}"
    else -> FirRenderer(
        packageDirectiveRenderer = FirPackageDirectiveRenderer(),
        resolvePhaseRenderer = FirResolvePhaseRenderer(),
        declarationRenderer = FirDeclarationRendererWithFilteredAttributes(),
    ).renderElementAsString(firElement)
}

private fun FirElement?.renderContainerSource(): String =
    (this as? FirCallableDeclaration)?.containerSource?.let { "${it::class.simpleName} ${it.presentableString}" } ?: "null"

abstract class AbstractSourceGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractInterruptingGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun <T : Any> intercept(index: Int, testModule: TestModule, block: () -> T?): T? {
        if (index in testModule.directives[Directives.INTERRUPT_AT]) {
            ErrorResistanceServiceRegistrar.handleInterruption {
                try {
                    block()
                    throw IllegalStateException("Analysis should be interrupted")
                } catch (_: AnalysisInterruptedException) {
                    null
                }
            }
            return null
        } else {
            return block()
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val INTERRUPT_AT by valueDirective("DECLARATION_TYPE", parser = Integer::valueOf)
    }
}

abstract class AbstractInterruptingSourceGetOrBuildFirTest : AbstractInterruptingGetOrBuildFirTest() {
    override val configurator = object : AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false) {
        override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
            get() = super.serviceRegistrars + listOf(ErrorResistanceServiceRegistrar)
    }
}

abstract class AbstractOutOfContentRootGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractScriptGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractInterruptingScriptGetOrBuildFirTest : AbstractInterruptingGetOrBuildFirTest() {
    override val configurator = object : AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false) {
        override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
            get() = super.serviceRegistrars + listOf(ErrorResistanceServiceRegistrar)
    }
}