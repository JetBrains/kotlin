/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
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
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
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
        val FLOATING_BINARY_OPERATOR_NAMES = FirIntegerOperator.Kind.values().filter { it.withFloatingRhs }.map { it.operatorName }
        val UNARY_OPERATOR_NAMES = FirIntegerOperator.Kind.values().filter { it.unary }.map { it.operatorName }
        private val ALL_OPERATORS = FirIntegerOperator.Kind.values().map { it.operatorName to it }.toMap()

        val SCOPE_SESSION_KEY = scopeSessionKey<ILTKey, FirTypeScope>()
    }

    @Suppress("PrivatePropertyName")
    private val BINARY_OPERATOR_SYMBOLS = BINARY_OPERATOR_NAMES.map { name ->
        name to FirNamedFunctionSymbol(CallableId(name)).apply {
            createFirFunction(name, this).apply {
                valueParameters += createValueParameter(FirILTTypeRefPlaceHolder(isUnsigned))
            }
        }
    }.toMap()

    private val FLOATING_BINARY_OPERATOR_SYMBOLS = FLOATING_BINARY_OPERATOR_NAMES.map { name ->
        name to listOf(session.builtinTypes.floatType, session.builtinTypes.doubleType).map { typeRef ->
            FirNamedFunctionSymbol(CallableId(name)).apply {
                createFirFunction(name, this, typeRef).apply {
                    valueParameters += createValueParameter(typeRef)
                }
            }
        }
    }.toMap()

    @Suppress("PrivatePropertyName")
    private val UNARY_OPERATOR_SYMBOLS = UNARY_OPERATOR_NAMES.map { name ->
        name to FirNamedFunctionSymbol(CallableId(name)).apply { createFirFunction(name, this) }
    }.toMap()

    @OptIn(FirImplementationDetail::class)
    private fun createFirFunction(
        name: Name,
        symbol: FirNamedFunctionSymbol,
        returnTypeRef: FirResolvedTypeRef = FirILTTypeRefPlaceHolder(isUnsigned)
    ): FirSimpleFunctionImpl = FirIntegerOperator(
        source = null,
        session,
        returnTypeRef,
        receiverTypeRef = null,
        ALL_OPERATORS.getValue(name),
        FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL),
        symbol
    ).apply {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
    }

    private fun createValueParameter(returnTypeRef: FirResolvedTypeRef): FirValueParameter {
        return buildValueParameter {
            source = null
            origin = FirDeclarationOrigin.Synthetic
            session = this@FirIntegerLiteralTypeScope.session
            this.returnTypeRef = returnTypeRef
            name = Name.identifier("arg")
            symbol = FirVariableSymbol(name)
            defaultValue = null
            isCrossinline = false
            isNoinline = false
            isVararg = false
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        UNARY_OPERATOR_SYMBOLS[name]?.let {
            processor(it)
            return
        }
        val symbol = BINARY_OPERATOR_SYMBOLS[name] ?: return
        processor(symbol)
        FLOATING_BINARY_OPERATOR_SYMBOLS[name]?.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NEXT

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NEXT

    override fun getCallableNames(): Set<Name> = ALL_OPERATORS.keys

    override fun getClassifierNames(): Set<Name> = emptySet()
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
    valueParameters = mutableListOf(),
    body = null,
    status,
    containerSource = null,
    contractDescription = FirEmptyContractDescription,
    kind.operatorName,
    symbol,
    annotations = mutableListOf(),
    typeParameters = mutableListOf(),
) {
    enum class Kind(val operatorName: Name, val unary: Boolean, val withFloatingRhs: Boolean) {
        PLUS(OperatorNameConventions.PLUS, unary = false, withFloatingRhs = true),
        MINUS(OperatorNameConventions.MINUS, unary = false, withFloatingRhs = true),
        TIMES(OperatorNameConventions.TIMES, unary = false, withFloatingRhs = true),
        DIV(OperatorNameConventions.DIV, unary = false, withFloatingRhs = true),
        REM(OperatorNameConventions.REM, unary = false, withFloatingRhs = true),
        SHL(Name.identifier("shl"), unary = false, withFloatingRhs = false),
        SHR(Name.identifier("shr"), unary = false, withFloatingRhs = false),
        USHR(Name.identifier("ushr"), unary = false, withFloatingRhs = false),
        XOR(Name.identifier("xor"), unary = false, withFloatingRhs = false),
        AND(Name.identifier("and"), unary = false, withFloatingRhs = false),
        OR(Name.identifier("or"), unary = false, withFloatingRhs = false),
        UNARY_PLUS(OperatorNameConventions.UNARY_PLUS, unary = true, withFloatingRhs = false),
        UNARY_MINUS(OperatorNameConventions.UNARY_MINUS, unary = true, withFloatingRhs = false),
        INV(Name.identifier("inv"), unary = true, withFloatingRhs = false),
    }
}

class FirILTTypeRefPlaceHolder(isUnsigned: Boolean) : FirResolvedTypeRef() {
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
