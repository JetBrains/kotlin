/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.renderer.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractGetOrBuildFirTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val selectedElement = testServices.expressionMarkerProvider.getSelectedElementOfTypeByDirective(mainFile, mainModule) as KtElement

        val actual = resolveWithClearCaches(mainFile) { session ->
            renderActualFir(
                fir = selectedElement.getOrBuildFir(session),
                ktElement = selectedElement,
                firFile = mainFile.getOrBuildFirFile(session),
            )
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}

fun renderActualFir(
    fir: FirElement?,
    ktElement: KtElement,
    renderKtText: Boolean = false,
    firFile: FirFile? = null,
): String = """
       |KT element: ${ktElement::class.simpleName}${if (renderKtText) "\nKT element text:\n" + ktElement.text else ""}
       |FIR element: ${fir?.let { it::class.simpleName }}
       |FIR source kind: ${fir?.source?.kind?.let { it::class.simpleName }}
       |
       |FIR element rendered:
       |${render(fir).trimEnd()}${if (firFile != null) "\n\nFIR FILE:\n${render(firFile).trimEnd()}" else ""}""".trimMargin()

private fun render(firElement: FirElement?): String = when (firElement) {
    null -> "null"
    is FirImport -> "import ${firElement.importedFqName}"
    else -> FirRenderer(
        fileAnnotationsContainerRenderer = FirFileAnnotationsContainerRenderer(),
        packageDirectiveRenderer = FirPackageDirectiveRenderer(),
        resolvePhaseRenderer = FirResolvePhaseRenderer(),
        declarationRenderer = FirDeclarationRendererWithFilteredAttributes(),
    ).renderElementAsString(firElement)
}

abstract class AbstractSourceGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractOutOfContentRootGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractScriptGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
