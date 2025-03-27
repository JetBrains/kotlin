/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirMppDeduplicatingSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.processAllCallables
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun referenceAllCommonDependencies(outputs: List<ModuleCompilerAnalyzedOutput>) {
    val (platformSession, scopeSession, _) = outputs.last()
    val deduplicatingProvider = (platformSession.symbolProvider as FirCachingCompositeSymbolProvider)
        .providers
        .firstIsInstanceOrNull<FirMppDeduplicatingSymbolProvider>()
        ?: return
    val visitor = Visitor(platformSession, scopeSession)
    val platformClassesReferencedDuringResolution = deduplicatingProvider.classMapping.values.map { it.platformClass }
    for (platformClass in platformClassesReferencedDuringResolution) {
        visitor.lookupEverythingInClass(platformClass)
    }

    val dependantFragments = outputs.dropLast(1)
    for ((_, _, files) in dependantFragments) {
        for (file in files) {
            file.accept(visitor)
        }
    }
}

private class Visitor(val session: FirSession, val scopeSession: ScopeSession) : FirDefaultVisitorVoid() {
    private val visited = mutableSetOf<ClassId>()

    override fun visitElement(element: FirElement) {
        if (element is FirExpression) {
            lookupInType(element.resolvedType)
        }
        element.acceptChildren(this)
    }

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        val symbol = resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*> ?: return
        val id = symbol.callableId.takeUnless { it.isLocal || it.classId != null } ?: return
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
            val classId = lookupTag.classId
            val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return@l
            lookupEverythingInClass(symbol)
        }
    }

    fun lookupEverythingInClass(symbol: FirClassLikeSymbol<*>) {
        if (!visited.add(symbol.classId)) return
        for (supertype in symbol.getSuperTypes(session, recursive = true)) {
            lookupInType(supertype)
        }
        lookupInTypeParameters(symbol.typeParameterSymbols)
        symbol.getContainingClassSymbol()?.let { lookupEverythingInClass(it) }
        @OptIn(SymbolInternals::class)
        lookupInAnnotations(symbol.annotations)
        val scope = symbol.defaultType().scope(
            session,
            scopeSession,
            CallableCopyTypeCalculator.CalculateDeferredForceLazyResolution,
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        ) ?: return
        scope.processDeclaredConstructors {
            lookupInMember(it)
        }
        scope.processAllCallables {
            lookupInMember(it)
        }
    }

    private fun lookupInMember(memberSymbol: FirCallableSymbol<*>) {
        @OptIn(SymbolInternals::class)
        lookupInAnnotations(memberSymbol.annotations)
        lookupInTypeParameters(memberSymbol.typeParameterSymbols)
        lookupInParameters(memberSymbol.contextParameterSymbols)
        memberSymbol.receiverParameterSymbol?.let { parameterSymbol ->
            lookupInType(parameterSymbol.resolvedType)
            @OptIn(SymbolInternals::class)
            lookupInAnnotations(parameterSymbol.annotations)
        }
        lookupInType(memberSymbol.resolvedReturnType)
        if (memberSymbol is FirFunctionSymbol<*>) {
            lookupInParameters(memberSymbol.valueParameterSymbols)
        }
    }

    private fun lookupInTypeParameters(typeParameterSymbols: List<FirTypeParameterSymbol>) {
        for (typeParameterSymbol in typeParameterSymbols) {
            @OptIn(SymbolInternals::class)
            lookupInAnnotations(typeParameterSymbol.annotations)
            for (typeRef in typeParameterSymbol.resolvedBounds) {
                lookupInType(typeRef.coneType)
            }
        }
    }

    private fun lookupInParameters(parameterSymbols: List<FirValueParameterSymbol>) {
        for (parameterSymbol in parameterSymbols) {
            lookupInType(parameterSymbol.resolvedReturnType)
            @OptIn(SymbolInternals::class)
            lookupInAnnotations(parameterSymbol.annotations)
        }
    }

    private fun lookupInAnnotations(annotations: List<FirAnnotation>) {
        for (annotation in annotations) {
            annotation.resolvedType.toClassLikeSymbol(session)?.let { lookupEverythingInClass(it) }
        }
    }
}
