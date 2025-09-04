/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid

fun referenceAllCommonDependencies(outputs: List<ModuleCompilerAnalyzedOutput>) {
    val platformSession = outputs.last().session
    if (!platformSession.languageVersionSettings.getFlag(AnalysisFlags.hierarchicalMultiplatformCompilation)) return
    val visitor = Visitor(platformSession)

    val dependantFragments = outputs.dropLast(1)
    for ((_, _, files) in dependantFragments) {
        for (file in files) {
            file.accept(visitor)
        }
    }
}

private class Visitor(val session: FirSession) : FirDefaultVisitorVoid() {
    override fun visitElement(element: FirElement) {
        if (element is FirExpression) {
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
        val symbol = resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*> ?: return
        val id = symbol.callableId.takeIf { symbol.rawStatus.visibility != Visibilities.Local && it != null && it.classId == null } ?: return
        session.symbolProvider.getTopLevelCallableSymbols(id.packageName, id.callableName)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        lookupInType(resolvedQualifier.resolvedType)
        visitElement(resolvedQualifier)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        lookupInType(resolvedTypeRef.coneType)
    }

    private fun lookupInType(type: ConeKotlinType) {
        type.forEachType l@{
            val lookupTag = it.classLikeLookupTagIfAny ?: return@l
            if (lookupTag is ConeClassLikeLookupTagWithFixedSymbol) return@l
            session.symbolProvider.getClassLikeSymbolByClassId(lookupTag.classId)
        }
    }
}
