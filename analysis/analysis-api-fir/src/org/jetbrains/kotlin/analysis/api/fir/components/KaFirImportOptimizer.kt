/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.analysis.api.components.KaImportOptimizer
import org.jetbrains.kotlin.analysis.api.components.KaImportOptimizerResult
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.isImplicitDispatchReceiver
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver
import org.jetbrains.kotlin.analysis.api.fir.utils.computeImportableName
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirImportOptimizer(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaImportOptimizer, KaFirSessionComponent {
    override fun analyzeImportsToOptimize(file: KtFile): KaImportOptimizerResult = withPsiValidityAssertion(file) {
        val existingImports = file.importDirectives
        if (existingImports.isEmpty()) return KaImportOptimizerResult()

        val (usedDeclarations, unresolvedNames) = collectReferencedEntities(file)

        return KaImportOptimizerResult(usedDeclarations, unresolvedNames)
    }

    override val KaSymbol.importableFqName: FqName?
        get() = withValidityAssertion {
            when (this) {
                is KaClassLikeSymbol -> classId?.asSingleFqName()
                is KaCallableSymbol -> firSymbol.computeImportableName()
                else -> null
            }
        }

    private data class ReferencedEntitiesResult(
        val usedImports: Map<FqName, Set<Name>>,
        val unresolvedNames: Set<Name>,
    )

    private fun collectReferencedEntities(file: KtFile): ReferencedEntitiesResult {
        val firFile = file.getOrBuildFirFile(resolutionFacade).apply { lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE) }
        val usedImports = mutableMapOf<FqName, MutableSet<Name>>()
        val unresolvedNames = mutableSetOf<Name>()

        fun saveReferencedItem(importableName: FqName, referencedByName: Name) {
            usedImports.getOrPut(importableName) { hashSetOf() } += referencedByName
        }

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

            override fun visitComponentCall(componentCall: FirComponentCall) {
                processFunctionCall(componentCall)
                super.visitComponentCall(componentCall)
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
                processErrorTypeRef(errorTypeRef)

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

            private fun processErrorNameReference(resolvable: FirResolvable) {
                val nameReference = resolvable.calleeReference as? FirErrorNamedReference ?: return
                val name = nameReference.unresolvedName ?: return
                unresolvedNames += name
            }

            private fun processErrorTypeRef(typeRef: FirErrorTypeRef) {
                val diagnostic = typeRef.diagnostic as? ConeUnresolvedError ?: return
                val name = diagnostic.unresolvedName ?: return
                unresolvedNames += name
            }

            private fun processFunctionCall(functionCall: FirFunctionCall) {
                if (functionCall.dispatchedWithoutImport) return
                processErrorNameReference(functionCall)

                val referencesByName = functionCall.functionReferenceName ?: return
                saveCallable(functionCall, referencesByName)
            }

            private fun processImplicitFunctionCall(implicitInvokeCall: FirImplicitInvokeCall) {
                processErrorNameReference(implicitInvokeCall)

                saveCallable(implicitInvokeCall, OperatorNameConventions.INVOKE)
            }

            private fun processPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
                if (propertyAccessExpression.dispatchedWithoutImport) return
                processErrorNameReference(propertyAccessExpression)

                val referencedByName = propertyAccessExpression.propertyReferenceName ?: return

                saveCallable(propertyAccessExpression, referencedByName)
            }

            private fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                val wholeQualifier = TypeQualifier.createFor(resolvedTypeRef) ?: return

                processTypeQualifier(wholeQualifier)
            }

            private fun processCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
                if (callableReferenceAccess.dispatchedWithoutImport) return
                processErrorNameReference(callableReferenceAccess)

                val referencedByName = callableReferenceAccess.callableReferenceName ?: return
                saveCallable(callableReferenceAccess, referencedByName)
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

            private fun saveCallable(qualifiedCall: FirQualifiedAccessExpression, referencedByName: Name) {
                val importableName = importableNameForReferencedSymbol(qualifiedCall) ?: return

                saveReferencedItem(importableName, referencedByName)
            }

            private fun importableNameForReferencedSymbol(qualifiedCall: FirQualifiedAccessExpression): FqName? {
                return qualifiedCall.importableNameForImplicitlyDispatchedCallable()
                    ?: qualifiedCall.referencedCallableSymbol?.computeImportableName()
            }

            /**
             * Returns correct importable name for implicitly dispatched callable - that is, a callable
             * which has a dispatch receiver, but whose dispatch receiver is present implicitly. The most
             * important case for that is the following:
             *
             * ```kt
             * import MyObject.bar
             *
             * open class Base { fun bar() {} }
             *
             * object MyObject : Base()
             *
             * fun test() {
             *   bar()
             * }
             * ```
             *
             * For the `bar()` call, `MyObject` instance is an implicit dispatch receiver.
             *
             * In such case, [FirQualifiedAccessExpression] representing the call references
             * the original `Base.bar` callable symbol instead of `MyObject.bar`, because there are
             * no separate symbol for that case.
             *
             * Java statics present a similar case - they can be imported not only from the declaring class,
             * but also from any subclass.
             */
            private fun FirQualifiedAccessExpression.importableNameForImplicitlyDispatchedCallable(): FqName? {
                val dispatchReceiver = dispatchReceiver
                if (
                    dispatchReceiver !is FirResolvedQualifier ||
                    !dispatchReceiver.isImplicitDispatchReceiver
                ) {
                    return null
                }

                val dispatcherClass = dispatchReceiver.classId ?: return null
                val referencedSymbolName = referencedCallableSymbol?.name ?: return null

                return CallableId(dispatcherClass, referencedSymbolName).asSingleFqName()
            }
        })

        file.accept(object : KtTreeVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is KDocLink) {
                    visitKDocLink(element)
                }
            }

            private fun visitKDocLink(docLink: KDocLink) {
                val docName = docLink.getChildOfType<KDocName>() ?: return
                val qualifiedNameAsFqName = docName.getQualifiedNameAsFqName()
                val importableNames = with(analysisSession) {
                    val resolvedSymbols = KDocReferenceResolver
                        .resolveKdocFqName(useSiteSession, qualifiedNameAsFqName, qualifiedNameAsFqName, docLink)
                    if (resolvedSymbols.isEmpty()) {
                        unresolvedNames += qualifiedNameAsFqName.shortName()
                        emptyList()
                    } else {
                        resolvedSymbols.flatMap { toImportableFqNames(it, qualifiedNameAsFqName) }
                    }
                }
                importableNames.forEach { importableName ->
                    saveReferencedItem(importableName, importableName.shortName())
                }
            }

            private fun toImportableFqNames(symbol: KaSymbol, qualifiedNameAsFqName: FqName): List<FqName> =
                buildList {
                    when (symbol) {
                        is KaCallableSymbol -> {
                            val callableId = symbol.callableId ?: return emptyList()
                            val fqName = callableId.asSingleFqName()
                            val classFqName = callableId.classId?.asSingleFqName()
                            // either it is a member declaration
                            if (classFqName != null) {
                                if (classFqName != qualifiedNameAsFqName) {
                                    this += classFqName
                                }
                            } else if (fqName != qualifiedNameAsFqName) {
                                // or some kind of top level declaration with potential receiver
                                this += fqName
                                val receiverClassType = symbol.receiverParameter?.returnType as? KaClassType
                                val receiverFqName = receiverClassType?.classId?.asSingleFqName()
                                // import has no receiver for receiver kdoc declaration:
                                // for receiver case kdoc like `[Foo.bar]`
                                // import looks like `import a.b.bar`
                                if (receiverFqName != null && qualifiedNameAsFqName.pathSegments().size > 1) {
                                    this += receiverFqName
                                }
                            }
                        }
                        is KaClassLikeSymbol -> {
                            val fqName = symbol.classId?.asSingleFqName()
                            if (fqName != null && fqName != qualifiedNameAsFqName) {
                                this += fqName
                            }
                        }
                    }
                }
        })

        return ReferencedEntitiesResult(usedImports, unresolvedNames)
    }
}

private val FirErrorNamedReference.unresolvedName: Name?
    get() = when (val diagnostic = diagnostic) {
        is ConeUnresolvedError -> diagnostic.unresolvedName
        is ConeAmbiguityError -> diagnostic.name
        else -> null
    }

private val ConeUnresolvedError.unresolvedName: Name?
    get() = when (this) {
        is ConeUnresolvedNameError -> name
        is ConeUnresolvedReferenceError -> name
        is ConeUnresolvedSymbolError -> classId.shortClassName
        is ConeUnresolvedTypeQualifierError -> {
            // we take the first qualifier part because only the first one can be imported
            qualifiers.firstOrNull()?.name
        }
    }


/**
 * An actual name by which this callable reference was used.
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
    }

/**
 * Referenced [ClassId], even if it is not completely correctly resolved.
 */
private val FirResolvedTypeRef.resolvedClassId: ClassId?
    get() {
        if (this !is FirErrorTypeRef) {
            // When you call a method from Java with type arguments, in FIR they are currently represented as flexible types.
            // TODO Consider handling other non-ConeLookupTagBasedType types here (see KT-66418)
            return coneType.abbreviatedTypeOrSelf.lowerBoundIfFlexible().classId
        }

        val candidateSymbols = diagnostic.getCandidateSymbols()
        val singleClassSymbol = candidateSymbols.singleOrNull() as? FirClassLikeSymbol

        return singleClassSymbol?.classId
    }

private val FirQualifiedAccessExpression.dispatchedWithoutImport: Boolean
    get() = when {
        isQualifiedWithPackage -> true
        dispatchReceiver is FirThisReceiverExpression -> true
        explicitReceiver != null && dispatchReceiver == explicitReceiver -> true
        else -> false
    }


/**
 * Returns `true` if [this] expression is fully-qualified with package name.
 * Such expressions definitely do not need any kind of imports.
 *
 * Examples:
 * - `pkg.foo()` - `true`
 * - `foo()` - `false`
 * - `Obj.foo()` - `false`
 * - `pkg.Obj.foo()` - `false`
 */
private val FirQualifiedAccessExpression.isQualifiedWithPackage: Boolean
    get() {
        val receiver = explicitReceiver
        return receiver is FirResolvedQualifier && receiver.relativeClassFqName == null
    }

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
                ?: errorWithAttachment("Cannot get referenced name from '${qualifier::class}'") {
                    withPsiEntry("qualifier", qualifier)
                }
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
                ?: errorWithAttachment("Cannot get referenced name from '${qualifier::class}'") {
                    withPsiEntry("qualifier", qualifier)
                }

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
        val FirResolvedQualifier.isPresentInSource: Boolean
            get() = when (source?.kind) {
                is KtRealSourceElementKind -> true
                is KtFakeSourceElementKind.ImplicitInvokeCall -> true

                else -> false
            }

        fun createFor(qualifier: FirResolvedQualifier): TypeQualifier? {
            if (!qualifier.isPresentInSource) return null

            val wholeClassId = qualifier.classId ?: return null
            val psi = qualifier.psi as? KtExpression ?: return null

            val wholeQualifier = when (psi) {
                is KtDotQualifiedExpression -> psi
                is KtNameReferenceExpression -> psi.getDotQualifiedExpressionForSelector() ?: psi
                else -> psi
            }

            return KtDotExpressionTypeQualifier(wholeClassId, wholeQualifier)
        }

        private val FirResolvedTypeRef.isPresentInSource: Boolean
            get() = when (source?.kind) {
                is KtRealSourceElementKind -> {
                    val isArrayFromVararg = delegatedTypeRef?.source?.kind is KtFakeSourceElementKind.ArrayTypeFromVarargParameter;

                    // type ref with delegated type ref with such source kind is NOT directly present in the source, so we ignore it
                    !isArrayFromVararg
                }

                // type ref with such source kind is explicitly present in the source, so we want to see it
                is KtFakeSourceElementKind.ArrayTypeFromVarargParameter -> true

                else -> false
            }

        fun createFor(typeRef: FirResolvedTypeRef): TypeQualifier? {
            if (!typeRef.isPresentInSource) return null

            val wholeClassId = typeRef.resolvedClassId ?: return null
            val psi = typeRef.psi as? KtTypeReference ?: return null

            val wholeUserType = psi.typeElement?.unwrapNullability() as? KtUserType ?: return null

            return KtUserTypeQualifier(wholeClassId, wholeUserType)
        }
    }
}
