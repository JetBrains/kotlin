/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtImportOptimizer
import org.jetbrains.kotlin.analysis.api.components.KtImportOptimizerResult
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.utils.computeImportableName
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class KtFirImportOptimizer(
    override val token: KtLifetimeToken,
    private val firResolveSession: LLFirResolveSession
) : KtImportOptimizer() {
    private val firSession: FirSession
        get() = firResolveSession.useSiteFirSession

    override fun analyseImports(file: KtFile): KtImportOptimizerResult {
        assertIsValidAndAccessible()

        val firFile = file.getOrBuildFirFile(firResolveSession).apply { lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE) }

        val existingImports = file.importDirectives

        val explicitlyImportedFqNames = existingImports
            .asSequence()
            .mapNotNull { it.importPath }
            .filter { !it.isAllUnder && !it.hasAlias() }
            .map { it.fqName }
            .toSet()

        val referencesEntities = collectReferencedEntities(firFile)
            .filterNot { (fqName, referencedByNames) ->
                // when referenced by more than one name, we need to keep the imports with same package
                fqName.parentOrNull() == file.packageFqName && referencedByNames.size == 1
            }

        val requiredStarImports = referencesEntities.keys
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
                importPath.fqName in referencesEntities -> importPath.importedName in referencesEntities.getValue(importPath.fqName)
                else -> false
            }

            if (!isUsed) {
                unusedImports += import
            }
        }

        return KtImportOptimizerResult(unusedImports)
    }

    private fun collectReferencedEntities(firFile: FirFile): Map<FqName, Set<Name>> {
        val usedImports = mutableMapOf<FqName, MutableSet<Name>>()

        firFile.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: FirFunctionCall) {
                processFunctionCall(functionCall)
                super.visitFunctionCall(functionCall)
            }

            override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall) {
                processImplicitFunctionCall(implicitInvokeCall)
                super.visitImplicitInvokeCall(implicitInvokeCall)
            }

            override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
                processPropertyAccessExpression(propertyAccessExpression)
                super.visitPropertyAccessExpression(propertyAccessExpression)
            }

            override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                processTypeRef(resolvedTypeRef)

                resolvedTypeRef.delegatedTypeRef?.accept(this)
                super.visitTypeRef(resolvedTypeRef)
            }

            override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
                processTypeRef(errorTypeRef)

                errorTypeRef.delegatedTypeRef?.accept(this)
                super.visitErrorTypeRef(errorTypeRef)
            }

            override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
                processCallableReferenceAccess(callableReferenceAccess)
                super.visitCallableReferenceAccess(callableReferenceAccess)
            }

            override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
                processResolvedQualifier(resolvedQualifier)
                super.visitResolvedQualifier(resolvedQualifier)
            }

            override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier) {
                processResolvedQualifier(errorResolvedQualifier)
                super.visitErrorResolvedQualifier(errorResolvedQualifier)
            }

            private fun processFunctionCall(functionCall: FirFunctionCall) {
                if (functionCall.isFullyQualified) return

                val referencesByName = functionCall.functionReferenceName ?: return
                val functionSymbol = functionCall.referencedCallableSymbol ?: return

                saveCallable(functionSymbol, referencesByName)
            }

            private fun processImplicitFunctionCall(implicitInvokeCall: FirImplicitInvokeCall) {
                val functionSymbol = implicitInvokeCall.referencedCallableSymbol ?: return

                saveCallable(functionSymbol, OperatorNameConventions.INVOKE)
            }

            private fun processPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
                if (propertyAccessExpression.isFullyQualified) return

                val referencedByName = propertyAccessExpression.propertyReferenceName ?: return
                val propertySymbol = propertyAccessExpression.referencedCallableSymbol ?: return

                saveCallable(propertySymbol, referencedByName)
            }

            private fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                val wholeQualifier = TypeQualifier.createFor(resolvedTypeRef) ?: return

                processTypeQualifier(wholeQualifier)
            }

            private fun processCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
                if (callableReferenceAccess.isFullyQualified) return

                val referencedByName = callableReferenceAccess.callableReferenceName ?: return
                val resolvedSymbol = callableReferenceAccess.referencedCallableSymbol ?: return

                saveCallable(resolvedSymbol, referencedByName)
            }

            private fun processResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
                val wholeQualifier = TypeQualifier.createFor(resolvedQualifier) ?: return

                processTypeQualifier(wholeQualifier)
            }

            private fun processTypeQualifier(qualifier: TypeQualifier) {
                val mostOuterTypeQualifier = generateSequence(qualifier) { it.outerTypeQualifier }.last()
                if (mostOuterTypeQualifier.isQualified) return

                saveType(mostOuterTypeQualifier)
            }

            private fun saveType(qualifier: TypeQualifier) {
                val importableName = qualifier.referencedClassId.asSingleFqName()
                val referencedByName = qualifier.referencedByName

                saveReferencedItem(importableName, referencedByName)
            }

            private fun saveCallable(resolvedSymbol: FirCallableSymbol<*>, referencedByName: Name) {
                val importableName = resolvedSymbol.computeImportableName(firSession) ?: return

                saveReferencedItem(importableName, referencedByName)
            }

            private fun saveReferencedItem(importableName: FqName, referencedByName: Name) {
                usedImports.getOrPut(importableName) { hashSetOf() } += referencedByName
            }
        })

        return usedImports
    }
}

/**
 * An actual name by which this callable reference were used.
 */
private val FirCallableReferenceAccess.callableReferenceName: Name?
    get() {
        toResolvedCallableReference()?.let { return it.name }

        val wholeCallableReferenceExpression = realPsi as? KtCallableReferenceExpression

        return wholeCallableReferenceExpression?.callableReference?.getReferencedNameAsName()
    }

/**
 * A name by which referenced functions was called.
 */
private val FirFunctionCall.functionReferenceName: Name?
    get() {
        toResolvedCallableReference()?.let { return it.name }

        // unresolved reference has incorrect name, so we have to retrieve it by PSI
        val wholeCallExpression = realPsi as? KtExpression
        val callExpression = wholeCallExpression?.getPossiblyQualifiedCallExpression()

        return callExpression?.getCallNameExpression()?.getReferencedNameAsName()
    }

/**
 * A name by which referenced property is used.
 */
private val FirPropertyAccessExpression.propertyReferenceName: Name?
    get() {
        toResolvedCallableReference()?.let { return it.name }

        // unresolved reference has incorrect name, so we have to retrieve it by PSI
        val wholePropertyAccessExpression = realPsi as? KtExpression
        val propertyNameExpression = wholePropertyAccessExpression?.getPossiblyQualifiedSimpleNameExpression()

        return propertyNameExpression?.getReferencedNameAsName()
    }

/**
 * Referenced callable symbol, even if it not completely correctly resolved.
 */
private val FirQualifiedAccessExpression.referencedCallableSymbol: FirCallableSymbol<*>?
    get() {
        return toResolvedCallableSymbol()
            ?: (calleeReference as? FirNamedReference)?.candidateSymbol as? FirCallableSymbol<*>
    }

/**
 * Referenced [ClassId], even if it is not completely correctly resolved.
 */
private val FirResolvedTypeRef.resolvedClassId: ClassId?
    get() {
        if (this !is FirErrorTypeRef) return type.classId

        val candidateSymbols = diagnostic.getCandidateSymbols()
        val singleClassSymbol = candidateSymbols.singleOrNull() as? FirClassLikeSymbol

        return singleClassSymbol?.classId
    }

private val FirQualifiedAccessExpression.isFullyQualified: Boolean
    get() = explicitReceiver is FirResolvedQualifier

private fun KtExpression.getPossiblyQualifiedSimpleNameExpression(): KtSimpleNameExpression? {
    return ((this as? KtQualifiedExpression)?.selectorExpression ?: this) as? KtSimpleNameExpression?
}

/**
 * Helper abstraction to navigate through qualified FIR elements - we have to match [ClassId] and PSI qualifier pair
 * to correctly reason about long qualifiers.
 */
private sealed interface TypeQualifier {
    val referencedClassId: ClassId

    /**
     * Type can be imported with alias, and thus can be referenced by the name different from its actual name.
     *
     * We cannot use [ClassId.getShortClassName] for this, since it is not affected by the alias.
     */
    val referencedByName: Name

    /**
     * Must be `true` if the PSI qualifier is itself qualified with the package or some other type, and `false` otherwise.
     *
     * ```
     * foo.bar.Baz -> true
     * Baz.Type -> true
     * Baz -> false
     * ```
     */
    val isQualified: Boolean

    val outerTypeQualifier: TypeQualifier?

    private class KtDotExpressionTypeQualifier(
        override val referencedClassId: ClassId,
        qualifier: KtElement,
    ) : TypeQualifier {

        private val dotQualifier: KtDotQualifiedExpression? = qualifier as? KtDotQualifiedExpression

        private val typeNameReference: KtNameReferenceExpression = run {
            require(qualifier is KtNameReferenceExpression || qualifier is KtDotQualifiedExpression || qualifier is KtCallExpression) {
                "Unexpected qualifier '${qualifier.text}' of type '${qualifier::class}'"
            }

            qualifier.getCalleeExpressionIfAny() as? KtNameReferenceExpression
                ?: error("Cannot get referenced name from '${qualifier.text}'")
        }

        override val referencedByName: Name
            get() = typeNameReference.getReferencedNameAsName()

        override val isQualified: Boolean
            get() = dotQualifier != null

        override val outerTypeQualifier: TypeQualifier?
            get() {
                val outerClassId = referencedClassId.outerClassId ?: return null
                val outerQualifier = dotQualifier?.receiverExpression ?: return null

                return KtDotExpressionTypeQualifier(outerClassId, outerQualifier)
            }
    }

    private class KtUserTypeQualifier(
        override val referencedClassId: ClassId,
        private val qualifier: KtUserType,
    ) : TypeQualifier {

        override val referencedByName: Name
            get() = qualifier.referenceExpression?.getReferencedNameAsName()
                ?: error("Cannot get referenced name from '${qualifier.text}'")

        override val isQualified: Boolean
            get() = qualifier.qualifier != null

        override val outerTypeQualifier: TypeQualifier?
            get() {
                val outerClassId = referencedClassId.outerClassId ?: return null
                val outerQualifier = qualifier.qualifier ?: return null

                return KtUserTypeQualifier(outerClassId, outerQualifier)
            }
    }

    companion object {
        fun createFor(qualifier: FirResolvedQualifier): TypeQualifier? {
            val wholeClassId = qualifier.classId ?: return null
            val psi = qualifier.psi as? KtExpression ?: return null

            val wholeQualifier = when (psi) {
                is KtDotQualifiedExpression -> psi
                is KtNameReferenceExpression -> psi.getDotQualifiedExpressionForSelector() ?: psi
                else -> psi
            }

            return KtDotExpressionTypeQualifier(wholeClassId, wholeQualifier)
        }

        fun createFor(typeRef: FirResolvedTypeRef): TypeQualifier? {
            val wholeClassId = typeRef.resolvedClassId ?: return null
            val psi = typeRef.psi as? KtTypeReference ?: return null

            val wholeUserType = psi.typeElement?.unwrapNullability() as? KtUserType ?: return null

            return KtUserTypeQualifier(wholeClassId, wholeUserType)
        }
    }
}
