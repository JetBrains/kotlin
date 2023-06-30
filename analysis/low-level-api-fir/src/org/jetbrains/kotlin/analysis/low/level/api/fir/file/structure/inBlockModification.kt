/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildLazyExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.psi.KtDeclaration

/**
 * Must be called in a write action.
 */
@LLFirInternals
fun invalidateAfterInBlockModification(declaration: KtDeclaration) {
    ApplicationManager.getApplication().assertIsWriteThread()

    val project = declaration.project
    val ktModule = ProjectStructureProvider.getModule(project, declaration, contextualModule = null)
    val resolveSession = ktModule.getFirResolveSession(project)
    when (val firDeclaration = declaration.resolveToFirSymbol(resolveSession).fir) {
        is FirSimpleFunction -> firDeclaration.inBodyInvalidation()
        is FirPropertyAccessor -> firDeclaration.inBodyInvalidation()
        is FirProperty -> firDeclaration.inBodyInvalidation()
        else -> errorWithFirSpecificEntries("Unknown declaration with body", fir = firDeclaration, psi = declaration)
    }
}

/**
 * Drop body and all related stuff.
 * We should drop:
 * * body
 * * control flow graph reference, because it depends on the body
 * * reduce phase if needed
 *
 * Depends on the body, but we shouldn't drop:
 * * implicit type, because the change mustn't change the resulting type
 * * contract, because a change inside a contract description is OOBM, so this function won't be called in this case
 *
 * Also, we shouldn't update somehow value parameters because they have their own "bodies" (a default value) and
 * changes in them are OOBM, so it is not our case.
 */
private fun FirSimpleFunction.inBodyInvalidation() {
    invalidateBody()
}

private fun FirFunction.invalidateBody(): FirResolvePhase? {
    val body = body ?: return null

    // the body is not yet resolved, so there is nothing to invalidate
    if (body is FirLazyBlock) return null
    val newPhase = phaseWithoutBody

    decreasePhase(newPhase)
    replaceBody(buildLazyBlock())
    replaceControlFlowGraphReference(null)

    if (this is FirContractDescriptionOwner) {
        replaceContractDescription(FirEmptyContractDescription)
    }

    return newPhase
}

/**
 * Drop body and all related stuff.
 * We should drop:
 * * initializer or delegate expression
 * * control flow graph reference, because it depends on the initializer or delegate
 * * body resolution state
 * * reduce phase if needed
 *
 * Depends on the body, but we shouldn't drop:
 * * implicit type, because the change mustn't change the resulting type
 *
 * Also, we shouldn't update the property accessors because they don't depend on the initializer or delegate.
 * So it is fine to leave the phase of setter/getter/backing field as it is.
 */
private fun FirProperty.inBodyInvalidation() {
    val blockIsInvalidated = invalidateInitializer() || invalidateDelegate()

    // the block is not invalidated, so there is nothing to reanalyze
    if (!blockIsInvalidated) return

    decreasePhase(phaseWithoutBody)
    replaceControlFlowGraphReference(null)
    replaceBodyResolveState(FirPropertyBodyResolveState.NOTHING_RESOLVED)
}

/**
 * Drop body and all related stuff.
 * We should drop:
 * * body
 * * control flow graph reference, because it depends on the body
 * * property body resolution state
 * * reduce phase if needed
 *
 * Depends on the body, but we shouldn't drop:
 * * implicit type, because the change mustn't change the resulting type
 * * contract, because a change inside a contract description is OOBM, so this function won't be called in this case
 */
private fun FirPropertyAccessor.inBodyInvalidation() {
    val newPhase = invalidateBody() ?: return

    val property = propertySymbol.fir
    property.decreasePhase(newPhase)

    val newPropertyResolveState = if (isGetter) {
        FirPropertyBodyResolveState.INITIALIZER_RESOLVED
    } else {
        FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED
    }

    property.replaceBodyResolveState(minOf(property.bodyResolveState, newPropertyResolveState))
}

private fun FirProperty.invalidateInitializer(): Boolean = replaceWithLazyExpressionIfNeeded(::initializer, ::replaceInitializer)

private fun FirProperty.invalidateDelegate(): Boolean = replaceWithLazyExpressionIfNeeded(::delegate, ::replaceDelegate)

private inline fun replaceWithLazyExpressionIfNeeded(
    expressionGetter: () -> FirExpression?,
    replaceExpression: (FirExpression) -> Unit,
): Boolean {
    val expression = expressionGetter() ?: return false

    // the expression is not yet resolved, so there is nothing to invalidate
    if (expression is FirLazyExpression) return false

    replaceExpression(buildLazyExpression { source = expression.source })
    return true
}

private val FirDeclaration.phaseWithoutBody: FirResolvePhase
    get() {
        val phaseBeforeBody = if (contractShouldBeResolved) {
            FirResolvePhase.CONTRACTS.previous
        } else {
            FirResolvePhase.BODY_RESOLVE.previous
        }

        return minOf(phaseBeforeBody, resolvePhase)
    }

private val FirDeclaration.contractShouldBeResolved: Boolean
    get() = this is FirFunction && body?.statements?.firstOrNull() is FirContractCallBlock

private fun FirDeclaration.decreasePhase(newPhase: FirResolvePhase) {
    @OptIn(ResolveStateAccess::class)
    resolveState = newPhase.asResolveState()
}
