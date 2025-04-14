/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
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
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithFilteredAttributes
import org.jetbrains.kotlin.fir.renderer.FirPackageDirectiveRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractGetOrBuildFirTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val selectedElement = testServices.expressionMarkerProvider
            .getTopmostSelectedElementOfTypeByDirective(mainFile, mainModule) as KtElement

        val actual = withResolutionFacade(mainFile) { resolutionFacade ->
            renderActualFir(
                fir = selectedElement.getOrBuildFir(resolutionFacade),
                ktElement = selectedElement,
                renderingOptions = testServices.firRenderingOptions,
                firFile = mainFile.getOrBuildFirFile(resolutionFacade),
            )
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
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

abstract class AbstractOutOfContentRootGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractScriptGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
