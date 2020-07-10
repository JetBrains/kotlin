/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralTypeImpl
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirIntegerLiteralTypeScope(private val session: FirSession, val isUnsigned: Boolean) : FirTypeScope() {
    sealed class ILTKey {
        object Signed : ILTKey()
        object Unsigned : ILTKey()
    }

    companion object {
        val BINARY_OPERATOR_NAMES = FirIntegerOperator.Kind.values().filterNot { it.unary }.map { it.operatorName }
        val UNARY_OPERATOR_NAMES = FirIntegerOperator.Kind.values().filter { it.unary }.map { it.operatorName }
        private val ALL_OPERATORS = FirIntegerOperator.Kind.values().map { it.operatorName to it }.toMap()

        val SCOPE_SESSION_KEY = scopeSessionKey<ILTKey, FirTypeScope>()
    }

    @Suppress("PrivatePropertyName")
    private val BINARY_OPERATOR_SYMBOLS = BINARY_OPERATOR_NAMES.map { name ->
        name to FirNamedFunctionSymbol(CallableId(name)).apply {
            createFirFunction(name, this).apply {
                val valueParameterName = Name.identifier("arg")
                valueParameters += buildValueParameter {
                    source = null
                    origin = FirDeclarationOrigin.Synthetic
                    session = this@FirIntegerLiteralTypeScope.session
                    returnTypeRef = FirILTTypeRefPlaceHolder(isUnsigned)
                    this.name = valueParameterName
                    symbol = FirVariableSymbol(valueParameterName)
                    defaultValue = null
                    isCrossinline = false
                    isNoinline = false
                    isVararg = false
                }
            }
        }
    }.toMap()

    @Suppress("PrivatePropertyName")
    private val UNARY_OPERATOR_SYMBOLS = UNARY_OPERATOR_NAMES.map { name ->
        name to FirNamedFunctionSymbol(CallableId(name)).apply { createFirFunction(name, this) }
    }.toMap()

    @OptIn(FirImplementationDetail::class)
    private fun createFirFunction(name: Name, symbol: FirNamedFunctionSymbol): FirSimpleFunctionImpl = FirIntegerOperator(
        source = null,
        session,
        FirILTTypeRefPlaceHolder(isUnsigned),
        receiverTypeRef = null,
        ALL_OPERATORS.getValue(name),
        FirResolvedDeclarationStatusImpl(Visibilities.PUBLIC, FirEffectiveVisibilityImpl.Public, Modality.FINAL),
        symbol
    ).apply {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        val symbol = BINARY_OPERATOR_SYMBOLS[name]
            ?: UNARY_OPERATOR_SYMBOLS[name]
            ?: return
        processor(symbol)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
    }

    override fun processOverriddenFunctionsWithDepth(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, Int) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NEXT
}

@OptIn(FirImplementationDetail::class)
class FirIntegerOperator @FirImplementationDetail constructor(
    source: FirSourceElement?,
    session: FirSession,
    returnTypeRef: FirTypeRef,
    receiverTypeRef: FirTypeRef?,
    val kind: Kind,
    status: FirDeclarationStatus,
    symbol: FirFunctionSymbol<FirSimpleFunction>
) : FirSimpleFunctionImpl(
    source,
    session,
    resolvePhase = FirResolvePhase.BODY_RESOLVE,
    FirDeclarationOrigin.Synthetic,
    returnTypeRef,
    receiverTypeRef,
    typeParameters = mutableListOf(),
    valueParameters = mutableListOf(),
    body = null,
    status,
    containerSource = null,
    contractDescription = FirEmptyContractDescription,
    kind.operatorName,
    symbol,
    annotations = mutableListOf(),
) {
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

class FirILTTypeRefPlaceHolder(
    isUnsigned: Boolean,
    override val isSuspend: Boolean = false
) : FirResolvedTypeRef() {
    override val source: FirSourceElement? get() = null
    override val annotations: List<FirAnnotationCall> get() = emptyList()
    override var type: ConeIntegerLiteralType = ConeIntegerLiteralTypeImpl(0, isUnsigned)
    override val delegatedTypeRef: FirTypeRef? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirResolvedTypeRef {
        return this
    }
}
