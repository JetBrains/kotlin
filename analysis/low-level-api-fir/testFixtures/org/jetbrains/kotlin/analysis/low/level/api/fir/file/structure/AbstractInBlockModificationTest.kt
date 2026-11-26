/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id.LLRealPsiSymbolId
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.withResolutionFacade
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.fir.symbols.symbolId
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
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
        val directives = testServices.moduleStructure.allDirectives
        val localProperty = if (Directives.CHECK_LOCAL_SYMBOL_ID_RESTORATION in directives) {
            selectedElement.parentsWithSelf.filterIsInstance<KtProperty>().first().also {
                require(it.isLocal) { "Select an element in a local property" }
            }
        } else {
            null
        }
        val actual = testInBlockModification(
            file = ktFile,
            elementToModify = selectedElement,
            testServices = testServices,
            dumpFirFile = Directives.DUMP_FILE in directives,
            localPropertyToRestore = localProperty,
        )

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    private object Directives : SimpleDirectivesContainer() {
        val DUMP_FILE by directive("Dump the entire FirFile to the output")
        val CHECK_LOCAL_SYMBOL_ID_RESTORATION by directive("Check symbol ID restoration for the modified local property")
    }
}

internal fun testInBlockModification(
    file: KtFile,
    elementToModify: PsiElement,
    testServices: TestServices,
    dumpFirFile: Boolean,
    localPropertyToRestore: KtProperty? = null,
): String = withResolutionFacade(file) { resolutionFacade ->
    // We are trying to invoke a test case twice inside one session to be sure that sequent modifications are work
    val firstAttempt = doTestInBlockModification(file, elementToModify, testServices, dumpFirFile, resolutionFacade, localPropertyToRestore)
    val secondAttempt = doTestInBlockModification(file, elementToModify, testServices, dumpFirFile, resolutionFacade, localPropertyToRestore)
    testServices.assertions.assertEquals(firstAttempt, secondAttempt) { "Invocations must be the same" }

    firstAttempt
}

private fun doTestInBlockModification(
    file: KtFile,
    elementToModify: PsiElement,
    testServices: TestServices,
    dumpFirFile: Boolean,
    resolutionFacade: LLResolutionFacade,
    localPropertyToRestore: KtProperty?,
): String {
    val declaration = elementToModify.getNonLocalContainingOrThisDeclaration() ?: file
    val firDeclarationBefore = declaration.getOrBuildFirOfType<FirDeclaration>(resolutionFacade)
    val declarationToRender = if (dumpFirFile) {
        file.getOrBuildFirFile(resolutionFacade).also { it.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE) }
    } else {
        firDeclarationBefore
    }

    val textBefore = declarationToRender.render()
    val localSymbolBefore = localPropertyToRestore?.let { resolutionFacade.resolveToFirSymbol(it, FirResolvePhase.BODY_RESOLVE) }

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

        modificationService.flushDeferredModifications()
        true
    }

    if (!isApplicable) {
        return "IN-BLOCK MODIFICATION IS NOT APPLICABLE FOR THIS PLACE"
    }

    val textAfterModification = declarationToRender.render()
    testServices.assertions.assertNotEquals(textBefore, textAfterModification) {
        "The declaration before and after modification must be in different state"
    }

    if (localPropertyToRestore != null && localSymbolBefore != null) {
        val id = localSymbolBefore.symbolId as LLRealPsiSymbolId<*>
        check(localPropertyToRestore.isValid) { "The local property's PSI anchor should survive the in-block modification" }

        // Force restoration before another lookup rebuilds the body. Keep the old symbol alive to compare identities.
        id.clearSymbolReferenceForTest()
        val restored = id.symbol
        val current = resolutionFacade.resolveToFirSymbol(localPropertyToRestore, FirResolvePhase.BODY_RESOLVE)

        check(restored !== localSymbolBefore) { "Restoration should rebuild the local symbol after invalidation" }
        check(restored === current) { "Restoration should return the same symbol as a fresh lookup" }
        check(restored.moduleData.session === localSymbolBefore.moduleData.session) { "The FIR session should survive an in-block modification" }
        check(restored.fir.psi === localPropertyToRestore) { "Restoration should preserve the PSI anchor" }
        check(restored.symbolId == id) { "The rebuilt symbol should have an equal symbol ID" }
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

        handleElementModification(element, KaElementModificationType.Unknown)
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

abstract class AbstractSourceLikeInBlockModificationTest : AbstractInBlockModificationTest() {
    override val configurator = LLSourceLikeTestConfigurator()
}

abstract class AbstractOutOfContentRootInBlockModificationTest : AbstractInBlockModificationTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}
