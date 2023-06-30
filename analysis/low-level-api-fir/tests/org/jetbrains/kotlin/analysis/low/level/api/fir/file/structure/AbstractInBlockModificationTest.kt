/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.extapi.psi.ASTDelegatePsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazyResolveRenderer
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractInBlockModificationTest : AbstractLowLevelApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)
        val isSupposedToBeModified = declaration.isReanalyzableContainer()
        val actual = if (isSupposedToBeModified) {
            resolveWithCaches(ktFile) { firSession ->
                val firDeclarationBefore = declaration.getOrBuildFirOfType<FirDeclaration>(firSession)
                val declarationTextBefore = firDeclarationBefore.render()

                declaration.modifyBody()
                invalidateAfterInBlockModification(declaration)

                val declarationTextAfterModification = firDeclarationBefore.render()
                testServices.assertions.assertNotEquals(declarationTextBefore, declarationTextAfterModification) {
                    "The declaration before and after modification must be in different state"
                }

                val firDeclarationAfter = declaration.getOrBuildFirOfType<FirDeclaration>(firSession)
                testServices.assertions.assertEquals(firDeclarationBefore, firDeclarationAfter) {
                    "The declaration before and after must be the same"
                }

                val declarationTextAfter = firDeclarationAfter.render()
                testServices.assertions.assertEquals(declarationTextBefore, declarationTextAfter) {
                    "The declaration must have the same in the resolved state"
                }

                "BEFORE MODIFICATION:\n$declarationTextBefore\nAFTER MODIFICATION:\n$declarationTextAfterModification"
            }
        } else {
            "IN-BLOCK MODIFICATION IS NOT APPLICABLE FOR THIS ${declaration::class.simpleName} DECLARATION"
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    /**
     * Emulate modification inside the body
     */
    private fun KtDeclaration.modifyBody() {
        parentsWithSelf.filterIsInstance<ASTDelegatePsiElement>().forEach {
            it.subtreeChanged()
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
    override val configurator = AnalysisApiFirOutOfContentRootTestConfigurator
}