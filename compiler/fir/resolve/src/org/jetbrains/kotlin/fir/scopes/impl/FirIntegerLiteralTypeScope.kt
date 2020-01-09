/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralTypeImpl
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

private object FirIntegerLiteralTypeClassifierSymbol : FirClassifierSymbol<FirIntegerLiteralTypeClassifier>() {
    override fun toLookupTag(): ConeClassifierLookupTag {
        throw IllegalStateException("Should not be called")
    }
}

private object FirIntegerLiteralTypeClassifier : FirDeclaration, FirSymbolOwner<FirIntegerLiteralTypeClassifier> {
    override val symbol: AbstractFirBasedSymbol<FirIntegerLiteralTypeClassifier>
        get() = FirIntegerLiteralTypeClassifierSymbol

    override val source: FirSourceElement? get() = throw IllegalStateException("Should not be called")
    override val session: FirSession get() = throw IllegalStateException("Should not be called")
    override val resolvePhase: FirResolvePhase get() = throw IllegalStateException("Should not be called")

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        throw IllegalStateException("Should not be called")
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        throw IllegalStateException("Should not be called")
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        throw IllegalStateException("Should not be called")
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        throw IllegalStateException("Should not be called")
    }
}

class FirIntegerLiteralTypeScope(private val session: FirSession) : FirScope() {
    companion object {
        val BINARY_OPERATOR_NAMES = FirIntegerOperator.Kind.values().filterNot { it.unary }.map { it.operatorName }
        val UNARY_OPERATOR_NAMES = FirIntegerOperator.Kind.values().filter { it.unary }.map { it.operatorName }
        private val ALL_OPERATORS = FirIntegerOperator.Kind.values().map { it.operatorName to it }.toMap()

        val ILT_SYMBOL: FirClassifierSymbol<*> = FirIntegerLiteralTypeClassifierSymbol
        val SCOPE_SESSION_KEY = scopeSessionKey<FirClassifierSymbol<*>, FirIntegerLiteralTypeScope>()
    }

    private val BINARY_OPERATOR_SYMBOLS = BINARY_OPERATOR_NAMES.map { name ->
        name to FirNamedFunctionSymbol(CallableId(name)).apply {
            createFirFunction(name, this).apply {
                val valueParameterName = Name.identifier("arg")
                valueParameters += FirValueParameterImpl(
                    source = null,
                    session,
                    FirILTTypeRefPlaceHolder(),
                    valueParameterName,
                    FirVariableSymbol(name),
                    defaultValue = null,
                    isCrossinline = false,
                    isNoinline = false,
                    isVararg = false
                )
            }
        }
    }.toMap()

    private val UNARY_OPERATOR_SYMBOLS = UNARY_OPERATOR_NAMES.map { name ->
        name to FirNamedFunctionSymbol(CallableId(name)).apply { createFirFunction(name, this) }
    }.toMap()

    private fun createFirFunction(name: Name, symbol: FirNamedFunctionSymbol): FirSimpleFunctionImpl = FirIntegerOperator(
        source = null,
        session,
        FirILTTypeRefPlaceHolder(),
        receiverTypeRef = null,
        ALL_OPERATORS.getValue(name),
        FirResolvedDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL),
        symbol
    ).apply {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
    }

    override fun processClassifiersByName(name: Name, processor: (FirClassifierSymbol<*>) -> Unit) {

    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        val symbol = BINARY_OPERATOR_SYMBOLS[name]
            ?: UNARY_OPERATOR_SYMBOLS[name]
            ?: return
        processor(symbol)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> Unit) {
    }
}

class FirIntegerOperator(
    source: FirSourceElement?,
    session: FirSession,
    returnTypeRef: FirTypeRef,
    receiverTypeRef: FirTypeRef?,
    val kind: Kind,
    status: FirDeclarationStatus,
    symbol: FirFunctionSymbol<FirSimpleFunction>
) : FirSimpleFunctionImpl(source, session, returnTypeRef, receiverTypeRef, kind.operatorName, status, symbol) {
    enum class Kind(val unary: Boolean, val operatorName: Name) {
        PLUS(false, OperatorNameConventions.PLUS),
        MINUS(false, OperatorNameConventions.MINUS),
        TIMES(false, OperatorNameConventions.TIMES),
        DIV(false, OperatorNameConventions.DIV),
        REM(false, OperatorNameConventions.REM),
        SHL(false, Name.identifier("shl")),
        SHR(false, Name.identifier("shr")),
        USHR(false, Name.identifier("ushr")),
        XOR(false, Name.identifier("xor")),
        AND(false, Name.identifier("and")),
        OR(false, Name.identifier("or")),
        UNARY_PLUS(true, OperatorNameConventions.UNARY_PLUS),
        UNARY_MINUS(true, OperatorNameConventions.UNARY_MINUS),
        INV(true, Name.identifier("inv"))
    }
}

class FirILTTypeRefPlaceHolder : FirResolvedTypeRef() {
    override val source: FirSourceElement? get() = null
    override val annotations: List<FirAnnotationCall> get() = emptyList()
    override var type: ConeIntegerLiteralType = ConeIntegerLiteralTypeImpl(0)
    override val delegatedTypeRef: FirTypeRef? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }
}

class FirIntegerOperatorCall(source: FirSourceElement?) : FirFunctionCallImpl(source)