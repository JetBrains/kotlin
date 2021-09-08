/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirFile
import org.jetbrains.kotlin.idea.frontend.api.assertIsValidAndAccessible
import org.jetbrains.kotlin.idea.frontend.api.components.KtImportOptimizer
import org.jetbrains.kotlin.idea.frontend.api.components.KtImportOptimizerResult
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.computeImportableName
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class KtFirImportOptimizer(
    override val token: ValidityToken,
    private val firResolveState: FirModuleResolveState
) : KtImportOptimizer() {
    private val firSession: FirSession
        get() = firResolveState.rootModuleSession

    override fun analyseImports(file: KtFile): KtImportOptimizerResult {
        assertIsValidAndAccessible()

        val firFile = file.getOrBuildFirFile(firResolveState).apply { ensureResolved(FirResolvePhase.BODY_RESOLVE) }

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
                super.visitTypeRef(resolvedTypeRef)
            }

            override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
                processCallableReferenceAccess(callableReferenceAccess)
                super.visitCallableReferenceAccess(callableReferenceAccess)
            }

            override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
                processResolvedQualifier(resolvedQualifier)
                super.visitResolvedQualifier(resolvedQualifier)
            }

            private fun processFunctionCall(functionCall: FirFunctionCall) {
                if (functionCall.isFullyQualified) return

                val functionReference = functionCall.toResolvedCallableReference() ?: return
                val functionSymbol = functionReference.toResolvedCallableSymbol() ?: return

                saveCallable(functionSymbol, functionReference.name)
            }

            private fun processImplicitFunctionCall(implicitInvokeCall: FirImplicitInvokeCall) {
                val functionSymbol = implicitInvokeCall.toResolvedCallableSymbol() ?: return

                saveCallable(functionSymbol, OperatorNameConventions.INVOKE)
            }

            private fun processPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
                if (propertyAccessExpression.isFullyQualified) return

                val propertyReference = propertyAccessExpression.toResolvedCallableReference() ?: return
                val propertySymbol = propertyReference.toResolvedCallableSymbol() ?: return

                saveCallable(propertySymbol, propertyReference.name)
            }

            private fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                val wholeQualifier = TypeQualifier.createFor(resolvedTypeRef) ?: return

                processTypeQualifier(wholeQualifier)
            }

            private fun processCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
                if (callableReferenceAccess.isFullyQualified) return

                val resolvedSymbol = callableReferenceAccess.calleeReference.toResolvedCallableSymbol() ?: return

                saveCallable(resolvedSymbol, resolvedSymbol.name)
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

private val FirQualifiedAccessExpression.isFullyQualified: Boolean
    get() = explicitReceiver is FirResolvedQualifier

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

        private val typeNameReference: KtNameReferenceExpression = when (qualifier) {
            is KtDotQualifiedExpression -> qualifier.selectorExpression as? KtNameReferenceExpression
            is KtNameReferenceExpression -> qualifier
            else -> null
        } ?: error("Cannot get referenced name from '${qualifier.text}'")

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
            val wholeClassId = typeRef.type.classId ?: return null
            val psi = typeRef.psi as? KtTypeReference ?: return null

            val wholeUserType = psi.typeElement?.unwrapNullability() as? KtUserType ?: return null

            return KtUserTypeQualifier(wholeClassId, wholeUserType)
        }
    }
}
