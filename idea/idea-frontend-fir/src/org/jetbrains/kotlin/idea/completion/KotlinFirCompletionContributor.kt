/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.getSymbolByTypeRef
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.idea.fir.getFirOfClosestParent
import org.jetbrains.kotlin.idea.fir.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

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

        val firFunction = LowLevelFirApiFacade.buildFunctionWithResolvedBody(originalFileFir, parentFunction)

        val element = nameExpression.getFirOfClosestParent() as? FirQualifiedAccessExpression ?: return

        for (scope in getScopes(element, firFunction.session)) {
            for (name in scope.availableNames()) {
                result.addElement(LookupElementBuilder.create(name.asString()))
            }
        }

//        val receiver = element.explicitReceiver
//        if (receiver != null) {
//
//            val firScope =
//                receiver.typeRef.coneTypeUnsafe<ConeKotlinType>().scope(firFunction.session, ScopeSession()) ?: return
//                // (symbolProvider.getSymbolByTypeRef<FirClassSymbol<*>>(receiver.typeRef))?.buildUseSiteMemberScope(firFunction.session, ScopeSession()) ?: return
//
//            for (name in firScope.getCallableNames()) {
//                firScope.processFunctionsByName(name) {
//                    it.fir
//                    result.addElement(LookupElementBuilder.create(name.asString()))
//                }
//
//                firScope.processPropertiesByName(name) {
//                    result.addElement(LookupElementBuilder.create(name.asString()))
//                }
//            }
//        }

//        val symbolProvider = originalFileFir.session.firSymbolProvider
//
//        val allCallableNamesInPackage =
////            symbolProvider.getTopLevelCallableSymbols(originalFile.packageFqName, Name.identifier("bar"))
//            symbolProvider.getAllCallableNamesInPackage(originalFileFir.packageFqName)
//
//        for (it in allCallableNamesInPackage) {
//            result.addElement(LookupElementBuilder.create(it.asString()))
//        }
//
//        val scope = ((nameExpression.getOrBuildFir() as? FirQualifiedAccessExpression)?.explicitReceiver as? FirFunctionCall)?.typeRef?.let {
//            (symbolProvider.getSymbolByTypeRef<AbstractFirBasedSymbol<*>>(it) as? FirClassSymbol<*>)?.classId?.let {
//                symbolProvider.getAllCallableNamesInClass(it)
//            }
//        }
//
////        val type = (((nameExpression.getOrBuildFir() as FirQualifiedAccessExpression).explicitReceiver as FirFunctionCall).typeRef as FirResolvedTypeRef).type as ConeClassLikeType
////        originalFileFir.session.declaredMemberScopeProvider.declaredMemberScope(type.lookupTag.toSymbol(originalFileFir.session).fir as FirRegularClass)
//
        return
    }

    interface ScopeWrapper {
        fun availableNames(): Set<Name>
    }

    fun getScopes(element: FirQualifiedAccessExpression, session: FirSession): Sequence<ScopeWrapper> = sequence {
        val receiver = element.explicitReceiver
        if (receiver != null) {

            val firScope = receiver.typeRef.coneTypeUnsafe<ConeKotlinType>().scope(session, ScopeSession())
            // (symbolProvider.getSymbolByTypeRef<FirClassSymbol<*>>(receiver.typeRef))?.buildUseSiteMemberScope(firFunction.session, ScopeSession()) ?: return

            if (firScope != null) {
                yield(object : ScopeWrapper {
                    override fun availableNames(): Set<Name> {
                        return firScope.getCallableNames()
                    }
                })
            }
        }
    }
}
