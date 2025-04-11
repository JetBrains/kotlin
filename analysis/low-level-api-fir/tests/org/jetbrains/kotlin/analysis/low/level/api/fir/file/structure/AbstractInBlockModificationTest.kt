/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KaElementModificationType
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventListener
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleOutOfBlockModificationEvent
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazyResolveRenderer
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.withResolveSession
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.testFramework.runWriteAction

abstract class AbstractInBlockModificationTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val selectedElement = testServices.expressionMarkerProvider.getBottommostSelectedElementOfTypeByDirective(
            file = mainFile,
            module = mainModule,
        )

        doTest(mainFile, selectedElement, testServices)
    }

    protected fun doTest(ktFile: KtFile, selectedElement: PsiElement, testServices: TestServices) {
        val actual = testInBlockModification(
            file = ktFile,
            elementToModify = selectedElement,
            testServices = testServices,
            dumpFirFile = Directives.DUMP_FILE in testServices.moduleStructure.allDirectives,
        )

        testServices.assertions.assertEqualsToTestOutputFile(actual)
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
): String = withResolveSession(file) { firSession ->
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
    resolutionFacade: LLResolutionFacade,
): String {
    val declaration = elementToModify.getNonLocalContainingOrThisDeclaration() ?: file
    val firDeclarationBefore = declaration.getOrBuildFirOfType<FirDeclaration>(resolutionFacade)
    val declarationToRender = if (dumpFirFile) {
        file.getOrBuildFirFile(resolutionFacade).also { it.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE) }
    } else {
        firDeclarationBefore
    }

    val textBefore = declarationToRender.render()

    val modificationService = LLFirDeclarationModificationService.getInstance(elementToModify.project)

    val isApplicable = runWriteAction {
        val isOutOfBlock = modificationService.modifyElement(elementToModify)
        if (isOutOfBlock) {
            return@runWriteAction false
        }

        elementToModify.modify()

        val textAfterPsiModification = declarationToRender.render()
        testServices.assertions.assertEquals(textBefore, textAfterPsiModification) {
            "The declaration before and after modification must be in the same state, because changes in not flushed yet"
        }

        modificationService.flushModifications()
        true
    }

    if (!isApplicable) {
        return "IN-BLOCK MODIFICATION IS NOT APPLICABLE FOR THIS PLACE"
    }

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
        declaration.getOrBuildFirOfType<FirDeclaration>(resolutionFacade)
        declarationToRender.render()
    }

    val firDeclarationAfter = declaration.getOrBuildFirOfType<FirDeclaration>(resolutionFacade)
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
    val disposable = Disposer.newDisposable("${LLFirDeclarationModificationService::class.simpleName}.disposable")
    var isOutOfBlock = false
    try {
        project.analysisMessageBus.connect(disposable).subscribe(
            KotlinModificationEvent.TOPIC,
            KotlinModificationEventListener { event ->
                if (event is KotlinModuleOutOfBlockModificationEvent) {
                    isOutOfBlock = true
                }
            }
        )

        elementModified(element, modificationType = KaElementModificationType.Unknown)
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
