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
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.isInvokeFunction
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirFile
import org.jetbrains.kotlin.idea.frontend.api.components.KtImportOptimizer
import org.jetbrains.kotlin.idea.frontend.api.components.KtImportOptimizerResult
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.util.OperatorNameConventions

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

                val functionReference = functionCall.toResolvedCallableReference() ?: return
                val functionSymbol = functionReference.toResolvedCallableSymbol() ?: return

                val referencedByName = if (functionCall.isInvokeOperatorImplicitCall) {
                    OperatorNameConventions.INVOKE
                } else {
                    functionReference.name
                }

                saveCallable(functionSymbol, referencedByName)
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

            private fun processResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference) {
                val referencedCallableSymbol = resolvedCallableReference.resolvedSymbol as? FirCallableSymbol<*> ?: return

                saveCallable(referencedCallableSymbol, resolvedCallableReference.name)
            }

            private fun processResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
                val wholeQualifier = TypeQualifier.createFor(resolvedQualifier) ?: return

                processTypeQualifier(wholeQualifier)
            }

            private fun processTypeQualifier(qualifier: TypeQualifier) {
                val mostOuterTypeQualifier = generateSequence(qualifier) { it.outerTypeQualifier }.last()
                if (mostOuterTypeQualifier.isQualified) return

                saveType(mostOuterTypeQualifier.classId)
            }

            private fun saveType(classId: ClassId) {
                val importableName = classId.asSingleFqName()
                val referencedByName = classId.shortClassName

                saveReferencedItem(importableName, referencedByName)
            }

            private fun saveCallable(resolvedSymbol: FirCallableSymbol<*>, referencedByName: Name) {
                val importableName = resolvedSymbol.computeImportableName() ?: return

                saveReferencedItem(importableName, referencedByName)
            }

            private fun saveReferencedItem(importableName: FqName, referencedByName: Name) {
                usedImports.getOrPut(importableName) { hashSetOf() } += referencedByName
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

private val FirQualifiedAccessExpression.isFullyQualified: Boolean
    get() = explicitReceiver is FirResolvedQualifier

/**
 * A hacky way to distinguish `foo()` call from `foo.invoke()` call.
 *
 * Need to be removed when the compiler is fixed to use `FirImplicitInvokeCall` in all appropriate situations.
 */
private val FirFunctionCall.isInvokeOperatorImplicitCall: Boolean
    get() {
        val functionSymbol = toResolvedCallableSymbol() ?: return false

        if (!functionSymbol.isInvokeFunction()) return false
        if (!functionSymbol.isOperator) return false

        val receiver = explicitReceiver ?: return false

        val callPsi = when (val psi = psi) {
            is KtQualifiedExpression -> psi.selectorExpression as? KtCallExpression
            is KtCallExpression -> psi
            else -> null
        } ?: return false

        // with invoke operator, explicit receiver does not have a source for some reason
        val receiverPsi = receiver.psi ?: return true

        return callPsi.calleeExpression == receiverPsi
    }

/**
 * Helper abstraction to navigate through qualified FIR elements - we have to match [ClassId] and PSI qualifier pair
 * to correctly reason about long qualifiers.
 */
private sealed interface TypeQualifier {
    val classId: ClassId

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
        override val classId: ClassId,
        private val qualifier: KtElement,
    ) : TypeQualifier {

        init {
            require(qualifier is KtDotQualifiedExpression || qualifier is KtNameReferenceExpression) {
                "Unexpected type of qualifier: ${qualifier::class}"
            }
        }

        override val isQualified: Boolean
            get() = qualifier is KtDotQualifiedExpression

        override val outerTypeQualifier: TypeQualifier?
            get() {
                val outerClassId = classId.outerClassId ?: return null
                val outerQualifier = (qualifier as? KtDotQualifiedExpression)?.receiverExpression ?: return null

                return KtDotExpressionTypeQualifier(outerClassId, outerQualifier)
            }
    }

    private class KtUserTypeQualifier(
        override val classId: ClassId,
        private val qualifier: KtUserType,
    ) : TypeQualifier {

        override val isQualified: Boolean
            get() = qualifier.qualifier != null

        override val outerTypeQualifier: TypeQualifier?
            get() {
                val outerClassId = classId.outerClassId ?: return null
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
