/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.config.hmppProvidersEnabled
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCommonDeclarationsMappingCollectingSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Used only for separate compilation mode (HMPP) only!
 *
 * This function goes over all resolved references to all classes and top-level callables in the non-leaf
 * sources and references them from the leaf (platform) symbol provider, so the
 * [FirCommonDeclarationsMappingCollectingSymbolProvider] could record the mapping between the platform and
 * common dependency in case this declaration was not searched from the platform session before.
 */
fun referenceAllCommonDependencies(outputs: List<SingleModuleFrontendOutput>) {
    val platformSession = outputs.last().session
    if (!platformSession.languageVersionSettings.hmppProvidersEnabled) return
    val visitor = Visitor(platformSession)

    val dependantFragments = outputs.dropLast(1)
    for ((val _ = session, val _ = scopeSession, val files = fir) in dependantFragments) {
        for (file in files) {
            file.accept(visitor)
        }
    }

    for (id in builtinTopLevelCallables) {
        platformSession.symbolProvider.getTopLevelCallableSymbols(id.packageName, id.callableName)
    }
}

/*
 * FIR2IR could inject calls to some functions from the stdlib during conversion
 * (e.g. call to `kotlin.internal.throwNoWhenBranchMatchedException()` for when expressions).
 * And it references it using the symbol provider of the converted module. If the reference
 * doesn't happen also during conversion of the leaf module (e.g. there is no `when` expression
 * in the platform code), the corresponding `FirCommonDeclarationsMappingSymbolProvider` wouldn't
 * record this function and so IR actualizer wouldn't actualize it.
 */
private val builtinTopLevelCallables: List<CallableId> = listOf(
    StandardClassIds.Callables.throwNoWhenBranchMatchedException
)

private class Visitor(val session: FirSession) : FirDefaultVisitorVoid() {
    private val visitedClasses = mutableSetOf<FirClassLikeSymbol<*>>()

    override fun visitElement(element: FirElement) {
        if (element is FirExpression && element !is FirExpressionStub) {
            lookupInType(element.resolvedType)
        }
        element.acceptChildren(this)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        val callableId = qualifiedAccessExpression.toResolvedCallableSymbol()?.callableId
        if (callableId != null && callableId.className == null) {
            session.symbolProvider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
        }
        super.visitQualifiedAccessExpression(qualifiedAccessExpression)
    }

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        val symbol = (resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*>)?.takeUnless { it.isLocal } ?: return
        val id = symbol.callableId.takeUnless { it?.classId != null } ?: return
        session.symbolProvider.getTopLevelCallableSymbols(id.packageName, id.callableName)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        resolvedQualifier.qualifierSymbol?.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId)
        resolvedQualifier.accessedObjectSymbol?.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId)
        resolvedQualifier.acceptChildren(this)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        lookupInType(resolvedTypeRef.coneType)
    }

    private fun lookupInType(type: ConeKotlinType) {
        type.forEachType l@{
            val lookupTag = it.classLikeLookupTagIfAny ?: return@l
            if (lookupTag is ConeClassLikeLookupTagWithFixedSymbol) return@l
            val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(lookupTag.classId) ?: return@l
            if (classSymbol.origin != FirDeclarationOrigin.Source && visitedClasses.add(classSymbol)) {
                @OptIn(SymbolInternals::class)
                classSymbol.fir.accept(this)
            }
        }
    }
}
