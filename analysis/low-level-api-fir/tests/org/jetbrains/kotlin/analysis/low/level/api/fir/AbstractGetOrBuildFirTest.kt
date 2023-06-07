/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.renderer.FirFileAnnotationsContainerRenderer
import org.jetbrains.kotlin.fir.renderer.FirPackageDirectiveRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractGetOrBuildFirTest : AbstractLowLevelApiSingleFileTest() {

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val module = moduleStructure.modules.single()
        val selectedElement = testServices.expressionMarkerProvider.getSelectedElementOfTypeByDirective(ktFile, module) as KtElement

        val actual = resolveWithClearCaches(ktFile) { session ->
            renderActualFir(selectedElement.getOrBuildFir(session), selectedElement)
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}

fun renderActualFir(fir: FirElement?, ktElement: KtElement, renderKtText: Boolean = false): String {
    return """|KT element: ${ktElement::class.simpleName}${if (renderKtText) "\nKT element text:\n" + ktElement.text else ""}
               |FIR element: ${fir?.let { it::class.simpleName }}
               |FIR source kind: ${fir?.source?.kind?.let { it::class.simpleName }}
               |
               |FIR element rendered:
               |${render(fir).trimEnd()}""".trimMargin()
}

private fun render(firElement: FirElement?): String = when (firElement) {
    null -> "null"
    is FirImport -> "import ${firElement.importedFqName}"
    else -> FirRenderer(
        fileAnnotationsContainerRenderer = FirFileAnnotationsContainerRenderer(),
        packageDirectiveRenderer = FirPackageDirectiveRenderer(),
        resolvePhaseRenderer = FirResolvePhaseRenderer(),
    ).renderElementAsString(firElement)
}

abstract class AbstractSourceGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractOutOfContentRootGetOrBuildFirTest : AbstractGetOrBuildFirTest() {
    override val configurator = AnalysisApiFirOutOfContentRootTestConfigurator
}