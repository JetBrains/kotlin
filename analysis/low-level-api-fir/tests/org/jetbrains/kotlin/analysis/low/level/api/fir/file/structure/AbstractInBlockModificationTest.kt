/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazyResolveRenderer
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus
import org.jetbrains.kotlin.analysis.providers.topics.KotlinModuleOutOfBlockModificationListener
import org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
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

        val actual = testInBlockModification(
            file = ktFile,
            elementToModify = selectedElement,
            testServices = testServices,
            dumpFirFile = Directives.DUMP_FILE in moduleStructure.allDirectives,
        )

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private object Directives : SimpleDirectivesContainer() {
        val DUMP_FILE by directive("Dump the entire FirFile to the output")
    }
}

internal fun testInBlockModification(
    file: KtFile,
    elementToModify: PsiElement,
    testServices: TestServices,
    dumpFirFile: Boolean,
): String = resolveWithCaches(file) { firSession ->
    // We are trying to invoke a test case twice inside one session to be sure that sequent modifications are work
    val firstAttempt = doTestInBlockModification(file, elementToModify, testServices, dumpFirFile, firSession)
    val secondAttempt = doTestInBlockModification(file, elementToModify, testServices, dumpFirFile, firSession)
    testServices.assertions.assertEquals(firstAttempt, secondAttempt) { "Invocations must be the same" }

    firstAttempt
}

private fun doTestInBlockModification(
    file: KtFile,
    elementToModify: PsiElement,
    testServices: TestServices,
    dumpFirFile: Boolean,
    firSession: LLFirResolveSession,
): String {
    val declaration = elementToModify.getNonLocalContainingOrThisDeclaration() ?: file
    val firDeclarationBefore = declaration.getOrBuildFirOfType<FirDeclaration>(firSession)
    val declarationToRender = if (dumpFirFile) {
        file.getOrBuildFirFile(firSession).also { it.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE) }
    } else {
        firDeclarationBefore
    }

    val textBefore = declarationToRender.render()

    val modificationService = LLFirDeclarationModificationService.getInstance(elementToModify.project)
    val isOutOfBlock = modificationService.modifyElement(elementToModify)
    if (isOutOfBlock) {
        return "IN-BLOCK MODIFICATION IS NOT APPLICABLE FOR THIS PLACE"
    }

    elementToModify.modify()

    val textAfterPsiModification = declarationToRender.render()
    testServices.assertions.assertEquals(textBefore, textAfterPsiModification) {
        "The declaration before and after modification must be in the same state, because changes in not flushed yet"
    }

    modificationService.flushModifications()

    val textAfterModification = declarationToRender.render()
    testServices.assertions.assertNotEquals(textBefore, textAfterModification) {
        "The declaration before and after modification must be in different state"
    }

    val textAfter = if (dumpFirFile) {
        // we should resolve the entire file instead of the declaration to be sure that this declaration will be
        // resolved by file resolution as well
        declarationToRender.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)
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

    return "BEFORE MODIFICATION:\n$textBefore\nAFTER MODIFICATION:\n$textAfterModification"
}

/**
 * @return **true** if out-of-block happens
 */
private fun LLFirDeclarationModificationService.modifyElement(element: PsiElement): Boolean {
    val disposable = Disposer.newDisposable()
    var isOutOfBlock = false
    try {
        project.analysisMessageBus.connect(disposable).subscribe(
            KotlinTopics.MODULE_OUT_OF_BLOCK_MODIFICATION,
            KotlinModuleOutOfBlockModificationListener { isOutOfBlock = true },
        )

        elementModified(element)
    } finally {
        Disposer.dispose(disposable)
    }

    return isOutOfBlock
}

/**
 * Emulate modification inside the body
 */
private fun PsiElement.modify() {
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
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
