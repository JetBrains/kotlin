/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.extapi.psi.ASTDelegatePsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazyResolveRenderer
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractInBlockModificationTest : AbstractLowLevelApiSingleFileTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val selectedElement = testServices.expressionMarkerProvider.getSelectedElementOfTypeByDirective(
            ktFile = ktFile,
            module = moduleStructure.modules.last(),
        )

        val declaration = selectedElement.getNonLocalReanalyzableContainingDeclaration()
        val actual = if (declaration != null) {
            val (before, after) = testInBlockModification(
                file = ktFile,
                declaration = declaration,
                testServices = testServices,
                dumpFirFile = Directives.DUMP_FILE in moduleStructure.allDirectives,
            )

            "BEFORE MODIFICATION:\n$before\nAFTER MODIFICATION:\n$after"
        } else {
            "IN-BLOCK MODIFICATION IS NOT APPLICABLE FOR THIS PLACE"
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private object Directives : SimpleDirectivesContainer() {
        val DUMP_FILE by directive("Dump the entire FirFile to the output")
    }
}

internal fun testInBlockModification(
    file: KtFile,
    declaration: KtAnnotated,
    testServices: TestServices,
    dumpFirFile: Boolean,
): Pair<String, String> = resolveWithCaches(file) { firSession ->
    val firDeclarationBefore = declaration.getOrBuildFirOfType<FirDeclaration>(firSession)
    val declarationToRender = if (dumpFirFile) {
        file.getOrBuildFirFile(firSession).also { it.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE) }
    } else {
        firDeclarationBefore
    }

    val textBefore = declarationToRender.render()

    declaration.modifyBody()
    invalidateAfterInBlockModification(declaration)

    val textAfterModification = declarationToRender.render()
    testServices.assertions.assertNotEquals(textBefore, textAfterModification) {
        "The declaration before and after modification must be in different state"
    }

    val textAfter = if (dumpFirFile) {
        // we should resolve the entire file instead of the declaration to be sure that this declaration will be
        // resolved by file resolution as well
        declarationToRender.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        declarationToRender.render()
    } else {
        declaration.getOrBuildFirOfType<FirDeclaration>(firSession)
        declarationToRender.render()
    }

    val firDeclarationAfter = declaration.getOrBuildFirOfType<FirDeclaration>(firSession)
    testServices.assertions.assertEquals(firDeclarationBefore, firDeclarationAfter) {
        "The declaration before and after must be the same"
    }

    testServices.assertions.assertEquals(textBefore, textAfter) {
        "The declaration must have the same in the resolved state"
    }

    Pair(textBefore, textAfterModification)
}

/**
 * Emulate modification inside the body
 */
private fun KtAnnotated.modifyBody() {
    for (parent in parentsWithSelf) {
        when (parent) {
            is ASTDelegatePsiElement -> parent.subtreeChanged()
            is KtCodeFragment -> parent.subtreeChanged()
        }
    }
}

private fun FirDeclaration.render(): String {
    val declarationToRender = if (this is FirPropertyAccessor) propertySymbol.fir else this
    return lazyResolveRenderer(StringBuilder()).renderElementAsString(declarationToRender)
}

abstract class AbstractSourceInBlockModificationTest : AbstractInBlockModificationTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractOutOfContentRootInBlockModificationTest : AbstractInBlockModificationTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractScriptInBlockModificationTest : AbstractInBlockModificationTest() {
    override val configurator get() = AnalysisApiFirScriptTestConfigurator
}
