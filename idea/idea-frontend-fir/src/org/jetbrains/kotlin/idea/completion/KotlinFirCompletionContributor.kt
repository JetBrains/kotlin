/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.idea.fir.getFirOfClosestParent
import org.jetbrains.kotlin.idea.fir.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext

class KotlinFirCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), KotlinFirCompletionProvider)
    }
}

/**
 * 1. Collect all receivers for the expression (implicit and explicit)
 */
private object KotlinFirCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val originalFile = parameters.originalFile as? KtFile ?: return

        val originalFileFir = originalFile.getOrBuildFirSafe<FirFile>(LowLevelFirApiFacade.getResolveStateFor(originalFile)) ?: return

        val reference = (parameters.position.parent as? KtSimpleNameExpression)?.mainReference ?: return
        val nameExpression = reference.expression.takeIf { it !is KtLabelReferenceExpression } ?: return

        val parentFunction = nameExpression.getNonStrictParentOfType<KtNamedFunction>() ?: return

        val completionContext = LowLevelFirApiFacade.buildCompletionContextForFunction(originalFileFir, parentFunction)

        val element = nameExpression.getFirOfClosestParent() as? FirQualifiedAccessExpression ?: return
        val towerDataContext = completionContext.getTowerDataContext(nameExpression)

        val symbols: Sequence<FirCallableSymbol<*>> = sequence {
            val explicitReceiver = element.explicitReceiver
            val explicitReceiverType = explicitReceiver?.typeRef?.coneTypeUnsafe<ConeKotlinType>()
            val explicitReceiverScope = explicitReceiverType?.scope(completionContext.session, ScopeSession())

            val implicitReceivers = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.implicitReceiver }
            val implicitReceiversTypes = implicitReceivers.map { it.type }

            val localScopes = towerDataContext.localScopes
            val nonLocalScopes = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope }
            val implicitReceiversScopes = implicitReceivers.mapNotNull { it.implicitScope }

            val allNonExplicitScopes = localScopes.asSequence() + nonLocalScopes + implicitReceiversScopes

            val typeContext = completionContext.session.typeContext

            if (explicitReceiverScope != null) {
                yieldAll(explicitReceiverScope.collectCallableSymbols())

                val allApplicableExtensions = allNonExplicitScopes
                    .flatMap { it.collectCallableSymbols() }
                    .filter { it.isExtension && it.isExtensionThatCanBeCalledOnTypes(listOf(explicitReceiverType), typeContext) }

                yieldAll(allApplicableExtensions)
            } else {
                val allAvailableSymbols = allNonExplicitScopes
                    .flatMap { it.collectCallableSymbols() }
                    .filter { !it.isExtension || it.isExtensionThatCanBeCalledOnTypes(implicitReceiversTypes, typeContext) }

                yieldAll(allAvailableSymbols)
            }
        }

        for (symbol in symbols) {
            if (symbol is FirConstructorSymbol) continue
            val symbolName = symbol.callableId.callableName

            result.addElement(LookupElementBuilder.create(symbolName.toString()))
        }
    }

}

private val FirCallableSymbol<*>.isExtension get() = fir.receiverTypeRef != null

private fun FirScope.collectCallableSymbols(): MutableList<FirCallableSymbol<*>> {
    val allCallableSymbols = mutableListOf<FirCallableSymbol<*>>()
    fun symbolsCollector(symbol: FirCallableSymbol<*>) {
        allCallableSymbols.add(symbol)
    }

    for (name in getCallableNames()) {
        processFunctionsByName(name, ::symbolsCollector)
        processPropertiesByName(name, ::symbolsCollector)
    }

    return allCallableSymbols
}

private fun FirCallableSymbol<*>.isExtensionThatCanBeCalledOnTypes(
    receivers: List<ConeKotlinType>,
    session: TypeCheckerProviderContext
): Boolean {
    val expectedReceiverType = fir.receiverTypeRef?.coneTypeUnsafe<ConeKotlinType>() ?: return false
    return receivers.any { AbstractTypeChecker.isSubtypeOf(session, it, expectedReceiverType) }
}
