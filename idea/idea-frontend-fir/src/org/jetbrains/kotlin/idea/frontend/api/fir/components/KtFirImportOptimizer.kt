/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirFile
import org.jetbrains.kotlin.idea.frontend.api.components.KtImportOptimizer
import org.jetbrains.kotlin.idea.frontend.api.components.KtImportOptimizerResult
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.resolve.ImportPath

internal class KtFirImportOptimizer(
    private val firResolveState: FirModuleResolveState
) : KtImportOptimizer() {
    private val firSession: FirSession
        get() = firResolveState.rootModuleSession

    override fun analyseImports(file: KtFile): KtImportOptimizerResult {
        val firFile = file.getOrBuildFirFile(firResolveState).apply { ensureResolved(FirResolvePhase.BODY_RESOLVE) }

        val existingImports = file.importDirectives

        val explicitlyImportedFqNames = existingImports
            .asSequence()
            .mapNotNull { it.importPath }
            .filter { !it.isAllUnder && !it.hasAlias() }
            .map { it.fqName }
            .toSet()

        val referencesEntities = collectReferencedEntities(firFile)

        val requiredStarImports = referencesEntities
            .asSequence()
            .filterNot { it in explicitlyImportedFqNames }
            .mapNotNull { it.parentOrNull() }
            .filterNot { it.isRoot }
            .toSet()

        val unusedImports = mutableSetOf<KtImportDirective>()
        val alreadySeenImports = mutableSetOf<ImportPath>()

        for (import in existingImports) {
            val importPath = import.importPath ?: continue

            val isUsed = when {
                !alreadySeenImports.add(importPath) -> false
                importPath.isAllUnder -> importPath.fqName in requiredStarImports
                importPath.fqName in referencesEntities -> true
                else -> false
            }

            if (!isUsed) {
                unusedImports += import
            }
        }

        return KtImportOptimizerResult(unusedImports)
    }

    private fun collectReferencedEntities(firFile: FirFile): Set<FqName> {
        val usedImports: MutableSet<FqName> = mutableSetOf()

        firFile.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: FirFunctionCall) {
                processFunctionCall(functionCall)
                super.visitFunctionCall(functionCall)
            }

            override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
                processPropertyAccessExpression(propertyAccessExpression)
                super.visitPropertyAccessExpression(propertyAccessExpression)
            }

            override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                processTypeRef(resolvedTypeRef)
                super.visitTypeRef(resolvedTypeRef)
            }

            override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference) {
                processResolvedCallableReference(resolvedCallableReference)
                super.visitResolvedCallableReference(resolvedCallableReference)
            }

            override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
                processResolvedQualifier(resolvedQualifier)
                super.visitResolvedQualifier(resolvedQualifier)
            }

            private fun processFunctionCall(functionCall: FirFunctionCall) {
                if (functionCall.isFullyQualified) return

                val functionSymbol = functionCall.toResolvedCallableSymbol() ?: return
                saveCallable(functionSymbol)
            }

            private fun processPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
                if (propertyAccessExpression.isFullyQualified) return

                val propertySymbol = propertyAccessExpression.toResolvedCallableSymbol() ?: return
                saveCallable(propertySymbol)
            }

            private fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                if (!resolvedTypeRef.canBeReferencedByImport()) return

                val classSymbol = resolvedTypeRef.toClassLikeSymbol(firSession) ?: return
                saveType(classSymbol)
            }

            private fun processResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference) {
                val referencedCallableSymbol = resolvedCallableReference.resolvedSymbol as? FirCallableSymbol<*> ?: return

                saveCallable(referencedCallableSymbol)
            }

            private fun processResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
                val qualifierSymbol = resolvedQualifier.symbol ?: return

                saveType(qualifierSymbol)
            }

            private fun saveType(symbol: FirClassLikeSymbol<*>) {
                usedImports += symbol.classId.asSingleFqName()
            }

            private fun saveCallable(symbol: FirCallableSymbol<*>) {
                val importableName = symbol.computeImportableName() ?: return
                usedImports += importableName
            }
        })

        return usedImports
    }

    // DUP in KtFirReferenceShortener
    private fun FirCallableSymbol<*>.computeImportableName(): FqName? {
        // if classId == null, callable is topLevel
        val classId = callableId.classId
            ?: return callableId.packageName.child(callableId.callableName)

        if (this is FirConstructorSymbol) return classId.asSingleFqName()

        val containingClass = getContainingClassSymbol(firSession) ?: return null

        // Java static members, enums, and object members can be imported
        return if (containingClass.origin == FirDeclarationOrigin.Java ||
            containingClass.classKind == ClassKind.ENUM_CLASS ||
            containingClass.classKind == ClassKind.OBJECT
        ) {
            classId.asSingleFqName()
        } else {
            null
        }
    }
}

private fun FirResolvedTypeRef.canBeReferencedByImport(): Boolean {
    val wholeTypeReference = psi as? KtTypeReference ?: return false

    val wholeTypeElement = wholeTypeReference.typeElement?.unwrapNullability() as? KtUserType ?: return false
    return wholeTypeElement.qualifier == null
}

private val FirQualifiedAccessExpression.isFullyQualified: Boolean
    get() = explicitReceiver is FirResolvedQualifier
