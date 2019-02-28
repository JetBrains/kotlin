/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.utils.addIfNotNull

class FirBodyResolveTransformer(val session: FirSession) : FirTransformer<Any?>() {

    val symbolProvider = session.service<FirSymbolProvider>()

    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Any?): CompositeTransformResult<FirTypeRef> {
        if (data == null)
            return implicitTypeRef.compose()
        require(data is FirTypeRef)
        return data.compose()
    }


    private fun ConeClassLikeType.buildSubstitutionScope(
        useSiteSession: FirSession,
        unsubstituted: FirScope,
        regularClass: FirRegularClass
    ): FirClassSubstitutionScope? {
        if (this.typeArguments.isEmpty()) return null

        val substitution = regularClass.typeParameters.zip(this.typeArguments) { typeParameter, typeArgument ->
            typeParameter.symbol to (typeArgument as? ConeTypedProjection)?.type
        }.filter { (_, type) -> type != null }.toMap() as Map<ConeTypeParameterSymbol, ConeKotlinType>

        return FirClassSubstitutionScope(useSiteSession, unsubstituted, substitution, true)
    }

    private fun FirRegularClass.buildUseSiteScope(useSiteSession: FirSession = session): FirClassUseSiteScope {
        val superTypeScope = FirCompositeScope(mutableListOf())
        val declaredScope = FirClassDeclaredMemberScope(this, useSiteSession)
        lookupSuperTypes(this, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession)
            .mapNotNullTo(superTypeScope.scopes) { useSiteSuperType ->
                if (useSiteSuperType is ConeClassErrorType) return@mapNotNullTo null
                val symbol = useSiteSuperType.symbol
                if (symbol is FirClassSymbol) {
                    val scope = symbol.fir.buildUseSiteScope(useSiteSession)
                    useSiteSuperType.buildSubstitutionScope(useSiteSession, scope, symbol.fir) ?: scope
                } else {
                    null
                }
            }
        return FirClassUseSiteScope(useSiteSession, superTypeScope, declaredScope, true)
    }

    fun ConeKotlinType.scope(useSiteSession: FirSession): FirScope {
        when (this) {
            is ConeClassTypeImpl -> return (this.symbol as FirBasedSymbol<FirRegularClass>).fir.buildUseSiteScope(useSiteSession)
            else -> error("Failed type ${this}")
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            scopes += regularClass.buildUseSiteScope()
            super.transformRegularClass(regularClass, data)
        }
    }


    protected inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val sizeBefore = scopes.size
        val result = l()
        val size = scopes.size
        assert(size >= sizeBefore)
        repeat(size - sizeBefore) {
            scopes.let { it.removeAt(it.size - 1) }
        }
        return result
    }

    val scopes = mutableListOf<FirScope>()

    enum class CandidateApplicability {
        HIDDEN,
        PARAMETER_MAPPING_ERROR,
        SYNTHETIC_RESOLVED,
        RESOLVED
    }

    inner class ApplicabilityChecker {

        var groupCounter = 0
        val groupNumbers = mutableListOf<Int>()
        val candidates = mutableListOf<ConeCallableSymbol>()


        var currentApplicability = CandidateApplicability.HIDDEN


        var expectedType: FirTypeRef? = null
        var explicitReceiverType: FirTypeRef? = null

        fun newDataSet() {
            groupNumbers.clear()
            candidates.clear()
            groupCounter = 0
            expectedType = null
            currentApplicability = CandidateApplicability.HIDDEN
        }

        fun newGroup() {
            groupCounter++
        }


        fun isSubtypeOf(superType: FirTypeRef?, subType: FirTypeRef?): Boolean {
            if (superType == null && subType == null) return true
            if (superType != null && subType != null) return true
            return false
        }


        private fun getApplicability(symbol: ConeCallableSymbol): CandidateApplicability {
            val declaration = (symbol as? FirBasedSymbol<FirCallableMember>)?.fir
                ?: return CandidateApplicability.HIDDEN

            if (!isSubtypeOf(declaration.receiverTypeRef, explicitReceiverType)) return CandidateApplicability.PARAMETER_MAPPING_ERROR
            return CandidateApplicability.RESOLVED
        }

        fun consumeCandidate(symbol: ConeCallableSymbol) {
            val applicability = getApplicability(symbol)

            if (applicability > currentApplicability) {
                groupNumbers.clear()
                candidates.clear()
                currentApplicability = applicability
            }


            if (applicability == currentApplicability) {
                candidates.add(symbol)
                groupNumbers.add(groupCounter)
            }
        }
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        qualifiedAccessExpression.explicitReceiver?.visitNoTransform(this, null)
        val callee = qualifiedAccessExpression.calleeReference as? FirNamedReference ?: return qualifiedAccessExpression.compose()

        with(ApplicabilityChecker()) {
            expectedType = data as FirTypeRef?
            explicitReceiverType = qualifiedAccessExpression.explicitReceiver?.resultType
            newDataSet()

            for (scope in scopes.asReversed()) {
                newGroup()
                val name = callee.name
                scope.processPropertiesByName(name) {
                    consumeCandidate(symbol = it)
                    ProcessorAction.NEXT
                }
                if (currentApplicability == CandidateApplicability.RESOLVED) {
                    break
                }
            }

            val result = candidates
            qualifiedAccessExpression.transformCalleeReference(this@FirBodyResolveTransformer, result)

            bindingContext[qualifiedAccessExpression] =
                when (val newCallee = qualifiedAccessExpression.calleeReference) {
                    is FirErrorNamedReference ->
                        FirErrorTypeRefImpl(session, qualifiedAccessExpression.psi, newCallee.errorReason)
                    is FirResolvedCallableReference ->
                        (newCallee.callableSymbol as FirBasedSymbol<FirCallableMember>).fir.returnTypeRef
                    else -> error("WTF!")
                }
        }
        return qualifiedAccessExpression.compose()
    }

    override fun transformNamedReference(namedReference: FirNamedReference, data: Any?): CompositeTransformResult<FirNamedReference> {
        if (namedReference is FirResolvedCallableReference) return namedReference.compose()
        val name = namedReference.name
        val referents = data as? List<ConeCallableSymbol> ?: return namedReference.compose()
        return when (referents.size) {
            0 -> FirErrorNamedReference(
                namedReference.session, namedReference.psi, "Unresolved name: $name"
            ).compose()
            1 -> FirResolvedCallableReferenceImpl(
                namedReference.session, namedReference.psi,
                name, referents.single()
            ).compose()
            else -> FirErrorNamedReference(
                namedReference.session, namedReference.psi, "Ambiguity: $name, ${referents.map { it.callableId }}"
            ).compose()
        }
    }

//
//    override fun transformAssignment(assignment: FirAssignment, data: Any?): CompositeTransformResult<FirStatement> {
//        return withNewSettings {
//            lookupProperties = true
//            lookupFunctions = false
//            super.transformAssignment(assignment, data)
//        }
//    }

//
//    override fun transformNamedReference(namedReference: FirNamedReference, data: Any?): CompositeTransformResult<FirNamedReference> {
//        if (namedReference is FirResolvedCallableReference) return namedReference.compose()
//        val name = namedReference.name
//        val referents = mutableListOf<ConeCallableSymbol>()
//        fun collect(it: ConeCallableSymbol): ProcessorAction {
//            referents.add(it)
//            return ProcessorAction.NEXT
//        }
//
//        if (lookupFunctions)
//            towerScope.processFunctionsByName(name, ::collect)
//        if (lookupProperties)
//            towerScope.processPropertiesByName(name, ::collect)
//
//        return when (referents.size) {
//            0 -> FirErrorNamedReference(
//                namedReference.session, namedReference.psi, "Unresolved name: $name"
//            ).compose()
//            1 -> FirResolvedCallableReferenceImpl(
//                namedReference.session, namedReference.psi,
//                name, referents.single()
//            ).compose()
//            else -> FirErrorNamedReference(
//                namedReference.session, namedReference.psi, "Ambiguity: $name, ${referents.map { it.callableId }}"
//            ).compose()
//        }
//
//    }
//
//    override fun transformQualifiedAccessExpression(
//        qualifiedAccessExpression: FirQualifiedAccessExpression,
//        data: Any?
//    ): CompositeTransformResult<FirStatement> {
//
//
//
//        return super.transformQualifiedAccessExpression(qualifiedAccessExpression, data)
//    }


    override fun transformBlock(block: FirBlock, data: Any?): CompositeTransformResult<FirStatement> {

        block.transformChildren(this, data)
        val statement = block.statements.lastOrNull()

        val resultExpression = when (statement) {
            is FirReturnExpression -> statement.result
            is FirExpression -> statement
            else -> null
        }
        resultExpression?.resultType?.let { bindingContext[block] = it }
        return super.transformBlock(block, data)
    }

    private fun commonSuperType(types: List<FirTypeRef>): FirTypeRef {
        return types.first()
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Any?): CompositeTransformResult<FirStatement> {

        val type = commonSuperType(whenExpression.branches.mapNotNull {
            it.result.visitNoTransform(this, data)
            it.result.resultType
        })
        bindingContext[whenExpression] = type
        return super.transformWhenExpression(whenExpression, data)
    }

    override fun <T> transformConstExpression(constExpression: FirConstExpression<T>, data: Any?): CompositeTransformResult<FirStatement> {
        if (data == null) return constExpression.compose()
        val expectedType = data as FirTypeRef

        if (expectedType is FirImplicitTypeRef) {

            val symbol = when (constExpression.kind) {
                IrConstKind.Null -> StandardClassIds.Nothing(symbolProvider)
                IrConstKind.Boolean -> StandardClassIds.Boolean(symbolProvider)
                IrConstKind.Char -> StandardClassIds.Char(symbolProvider)
                IrConstKind.Byte -> StandardClassIds.Byte(symbolProvider)
                IrConstKind.Short -> StandardClassIds.Short(symbolProvider)
                IrConstKind.Int -> StandardClassIds.Int(symbolProvider)
                IrConstKind.Long -> StandardClassIds.Long(symbolProvider)
                IrConstKind.String -> StandardClassIds.String(symbolProvider)
                IrConstKind.Float -> StandardClassIds.Float(symbolProvider)
                IrConstKind.Double -> StandardClassIds.Double(symbolProvider)
            }

            val type = ConeClassTypeImpl(symbol, emptyArray(), isNullable = constExpression.kind == IrConstKind.Null)

            bindingContext[constExpression] = FirResolvedTypeRefImpl(session, null, type, false, emptyList())
        } else {
            bindingContext[constExpression] = expectedType
        }


        return super.transformConstExpression(constExpression, data)
    }

    @Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
    val bindingContext = mutableMapOf<FirExpression, FirTypeRef>()

    val FirExpression.resultType: FirTypeRef? get() = bindingContext[this]


    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Any?): CompositeTransformResult<FirDeclaration> {

        return withScopeCleanup {
            scopes.addIfNotNull(namedFunction.receiverTypeRef?.coneTypeSafe()?.scope(session))
            val body = namedFunction.body
            if (namedFunction.returnTypeRef is FirImplicitTypeRef && body != null) {
                body.visitNoTransform(this, namedFunction.returnTypeRef)
                namedFunction.transformReturnTypeRef(this, body.resultType)
            }

            super.transformNamedFunction(namedFunction, data)
        }

    }

    override fun transformVariable(variable: FirVariable, data: Any?): CompositeTransformResult<FirDeclaration> {
        val initializer = variable.initializer
        initializer?.visitNoTransform(this, variable.returnTypeRef)
        if (variable.returnTypeRef is FirImplicitTypeRef) {
            when {
                variable.delegate != null -> TODO("!?")
                initializer != null -> {
                    variable.transformReturnTypeRef(this, initializer.resultType)
                }
            }
        }
        return super.transformVariable(variable, data)
    }

    override fun transformProperty(property: FirProperty, data: Any?): CompositeTransformResult<FirDeclaration> {
        return transformVariable(property, data)
    }

    private fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
        val result = this.transform(transformer, data)
        require(result.single === this)
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val transformer = FirBodyResolveTransformer(file.session)
        return file.transform(transformer, null)
    }
}
