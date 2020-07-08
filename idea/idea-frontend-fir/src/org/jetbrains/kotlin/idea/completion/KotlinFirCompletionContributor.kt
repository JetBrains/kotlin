/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.idea.fir.getFirOfClosestParent
import org.jetbrains.kotlin.idea.fir.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelPropertyByPackageIndex
import org.jetbrains.kotlin.name.FqName
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
            val packageIndexHelper = PackageIndexHelper(parameters.position.project)

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
                val nonExtensionMembers = explicitReceiverScope.collectCallableSymbols(packageIndexHelper).filter { !it.isExtension }
                yieldAll(nonExtensionMembers)

                val allApplicableExtensions = allNonExplicitScopes
                    .flatMap { it.collectCallableSymbols(packageIndexHelper) }
                    .filter { it.isExtension && it.isExtensionThatCanBeCalledOnTypes(listOf(explicitReceiverType), typeContext) }

                yieldAll(allApplicableExtensions)
            } else {
                val allAvailableSymbols = allNonExplicitScopes
                    .flatMap { it.collectCallableSymbols(packageIndexHelper) }
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


private fun FirScope.getScopeNames(indexHelper: PackageIndexHelper): Set<Name> = when (this) {
    is FirAbstractStarImportingScope -> {
        this.starImports.flatMapTo(mutableSetOf()) { indexHelper.getPackageTopLevelNames(it.packageFqName) }
    }

    is FirAbstractSimpleImportingScope -> simpleImports.keys

    else -> getCallableNames()
}

private fun FirScope.collectCallableSymbols(packageIndexHelper: PackageIndexHelper): MutableList<FirCallableSymbol<*>> {
    val allCallableSymbols = mutableListOf<FirCallableSymbol<*>>()
    fun symbolsCollector(symbol: FirCallableSymbol<*>) {
        allCallableSymbols.add(symbol)
    }

    val scopeNames = getScopeNames(packageIndexHelper)
    for (name in scopeNames) {
        processFunctionsByName(name, ::symbolsCollector)
        processPropertiesByName(name, ::symbolsCollector)
    }

    return allCallableSymbols
}

private class PackageIndexHelper(private val project: Project) {
    private val searchScope = GlobalSearchScope.allScope(project)

    private val functionByPackageIndex = KotlinTopLevelFunctionByPackageIndex.getInstance()
    private val propertyByPackageIndex = KotlinTopLevelPropertyByPackageIndex.getInstance()

    fun getPackageTopLevelNames(packageFqName: FqName): Set<Name> {
        return getTopLevelCallables(packageFqName).mapTo(mutableSetOf()) { it.nameAsSafeName }
    }

    private fun getTopLevelCallables(packageFqName: FqName): Sequence<KtCallableDeclaration> = sequence {
        yieldAll(functionByPackageIndex.get(packageFqName.asString(), project, searchScope))
        yieldAll(propertyByPackageIndex.get(packageFqName.asString(), project, searchScope))
    }
}

private fun FirCallableSymbol<*>.isExtensionThatCanBeCalledOnTypes(
    receivers: List<ConeKotlinType>,
    session: TypeCheckerProviderContext
): Boolean {
    val expectedReceiverType = fir.receiverTypeRef?.coneTypeUnsafe<ConeKotlinType>() ?: return false
    return receivers.any { AbstractTypeChecker.isSubtypeOf(session, it, expectedReceiverType) }
}
