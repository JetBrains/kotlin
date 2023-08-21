/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeContractDescriptionError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class ConeEffectExtractor(
    private val session: FirSession,
    private val owner: FirContractDescriptionOwner,
    private val valueParameters: List<FirValueParameter>
) : FirDefaultVisitor<ConeContractDescriptionElement, Nothing?>() {
    companion object {
        private val BOOLEAN_AND = FirContractsDslNames.id("kotlin", "Boolean", "and")
        private val BOOLEAN_OR = FirContractsDslNames.id("kotlin", "Boolean", "or")
        private val BOOLEAN_NOT = FirContractsDslNames.id("kotlin", "Boolean", "not")
    }

    private fun ConeContractDescriptionError.asElement(): KtErroneousContractElement<ConeKotlinType, ConeDiagnostic> {
        return KtErroneousContractElement(this)
    }

    override fun visitElement(element: FirElement, data: Nothing?): ConeContractDescriptionElement {
        return ConeContractDescriptionError.IllegalElement(element).asElement()
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Nothing?): ConeContractDescriptionElement {
        return returnExpression.result.accept(this, data)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?): ConeContractDescriptionElement {
        val resolvedId = functionCall.toResolvedCallableSymbol()?.callableId
            ?: return ConeContractDescriptionError.UnresolvedCall(functionCall.calleeReference.name).asElement()

        return when (resolvedId) {
            FirContractsDslNames.IMPLIES -> {
                val effect = functionCall.explicitReceiver?.asContractElement() as? ConeEffectDeclaration ?: noReceiver(resolvedId)
                val condition = functionCall.argument.asContractElement() as? ConeBooleanExpression ?: noArgument(resolvedId)
                ConeConditionalEffectDeclaration(effect, condition)
            }

            FirContractsDslNames.RETURNS -> {
                val argument = functionCall.arguments.firstOrNull()
                val value = if (argument == null) {
                    ConeContractConstantValues.WILDCARD
                } else {
                    when (val value = argument.asContractElement()) {
                        is ConeConstantReference -> value
                        else -> KtErroneousConstantReference(ConeContractDescriptionError.NotAConstant(value))
                    }
                }
                @Suppress("UNCHECKED_CAST")
                KtReturnsEffectDeclaration(value as ConeConstantReference)
            }

            FirContractsDslNames.RETURNS_NOT_NULL -> {
                ConeReturnsEffectDeclaration(ConeContractConstantValues.NOT_NULL)
            }

            FirContractsDslNames.CALLS_IN_PLACE -> {
                val reference = functionCall.arguments[0].asContractValueExpression()
                when (val argument = functionCall.arguments.getOrNull(1)) {
                    null -> ConeCallsEffectDeclaration(reference, EventOccurrencesRange.UNKNOWN)
                    else -> when (val kind = argument.parseInvocationKind()) {
                        null -> KtErroneousCallsEffectDeclaration(reference, ConeContractDescriptionError.UnresolvedInvocationKind(argument))
                        else -> ConeCallsEffectDeclaration(reference, kind)
                    }
                }
            }

            BOOLEAN_AND, BOOLEAN_OR -> {
                val left = functionCall.explicitReceiver?.asContractBooleanExpression() ?: noReceiver(resolvedId)
                val right = functionCall.arguments.firstOrNull()?.asContractBooleanExpression() ?: noArgument(resolvedId)
                val kind = when (resolvedId) {
                    BOOLEAN_AND -> LogicOperationKind.AND
                    BOOLEAN_OR -> LogicOperationKind.OR
                    else -> shouldNotBeCalled()
                }
                ConeBinaryLogicExpression(left, right, kind)
            }

            BOOLEAN_NOT -> {
                val arg = functionCall.explicitReceiver?.asContractBooleanExpression() ?: noReceiver(resolvedId)
                ConeLogicalNot(arg)
            }

            else -> ConeContractDescriptionError.NotContractDsl(resolvedId).asElement()
        }
    }

    override fun visitBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: Nothing?
    ): ConeContractDescriptionElement {
        val left = binaryLogicExpression.leftOperand.asContractBooleanExpression()
        val right = binaryLogicExpression.rightOperand.asContractBooleanExpression()
        return ConeBinaryLogicExpression(left, right, binaryLogicExpression.kind)
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Nothing?): ConeContractDescriptionElement {
        val isNegated = when (val operation = equalityOperatorCall.operation) {
            FirOperation.EQ -> false
            FirOperation.NOT_EQ -> true
            else -> return ConeContractDescriptionError.IllegalEqualityOperator(operation).asElement()
        }

        val argument = equalityOperatorCall.arguments[1]
        val const = argument as? FirConstExpression<*> ?: return ConeContractDescriptionError.NotAConstant(argument).asElement()
        if (const.kind != ConstantValueKind.Null) return ConeContractDescriptionError.IllegalConst(const, onlyNullAllowed = true).asElement()

        val arg = equalityOperatorCall.arguments[0].asContractValueExpression()
        return ConeIsNullPredicate(arg, isNegated)
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: Nothing?): ConeContractDescriptionElement {
        return smartCastExpression.originalExpression.accept(this, data)
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Nothing?
    ): ConeContractDescriptionElement {
        val symbol = qualifiedAccessExpression.toResolvedCallableSymbol()
            ?: run {
                val name = (qualifiedAccessExpression.calleeReference as? FirNamedReference)?.name ?: Name.special("unresolved")
                return ConeContractDescriptionError.UnresolvedCall(name).asElement()
            }
        val parameter = symbol.fir as? FirValueParameter
            ?: return KtErroneousValueParameterReference(
                ConeContractDescriptionError.IllegalParameter(symbol, "$symbol is not a value parameter")
            )
        val index = valueParameters.indexOf(parameter).takeUnless { it < 0 } ?: return KtErroneousValueParameterReference(
            ConeContractDescriptionError.IllegalParameter(symbol, "Value paramter $symbol is not found in parameters of outer function")
        )
        val type = parameter.returnTypeRef.coneType

        val name = parameter.name.asString()
        return toValueParameterReference(type, index, name)
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: Nothing?
    ): ConeContractDescriptionElement {
        return visitQualifiedAccessExpression(propertyAccessExpression, data)
    }

    private fun toValueParameterReference(
        type: ConeKotlinType,
        index: Int,
        name: String
    ): ConeValueParameterReference {
        return if (type == session.builtinTypes.booleanType.type) {
            ConeBooleanValueParameterReference(index, name)
        } else {
            ConeValueParameterReference(index, name)
        }
    }

    private fun FirContractDescriptionOwner.isAccessorOf(declaration: FirDeclaration): Boolean {
        return declaration is FirProperty && (declaration.getter == this || declaration.setter == this)
    }

    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: Nothing?
    ): ConeContractDescriptionElement {
        val declaration = thisReceiverExpression.calleeReference.boundSymbol?.fir
            ?: return ConeContractDescriptionError.UnresolvedThis(thisReceiverExpression).asElement()
        val callableOwner = owner as? FirCallableDeclaration
        val ownerHasReceiver = callableOwner?.receiverParameter != null
        val ownerIsMemberOfDeclaration = callableOwner?.getContainingClass(session) == declaration
        return if (declaration == owner || owner.isAccessorOf(declaration) || ownerIsMemberOfDeclaration && !ownerHasReceiver) {
            val type = thisReceiverExpression.resolvedType
            toValueParameterReference(type, -1, "this")
        } else {
            ConeContractDescriptionError.IllegalThis(thisReceiverExpression).asElement()
        }
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: Nothing?): ConeContractDescriptionElement {
        return when (constExpression.kind) {
            ConstantValueKind.Null -> ConeContractConstantValues.NULL
            ConstantValueKind.Boolean -> when (constExpression.value as Boolean) {
                true -> ConeContractConstantValues.TRUE
                false -> ConeContractConstantValues.FALSE
            }
            else -> ConeContractDescriptionError.IllegalConst(constExpression, onlyNullAllowed = false).asElement()
        }
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): ConeContractDescriptionElement {
        val arg = typeOperatorCall.argument.asContractValueExpression()
        val type = typeOperatorCall.conversionTypeRef.coneType.fullyExpandedType(session)
        val isNegated = typeOperatorCall.operation == FirOperation.NOT_IS
        val diagnostic = (type.toSymbol(session) as? FirTypeParameterSymbol)?.let { typeParameterSymbol ->
            val typeParametersOfOwner = (owner as? FirTypeParameterRefsOwner)?.typeParameters.orEmpty()
            if (typeParametersOfOwner.none { it is FirTypeParameter && it.symbol == typeParameterSymbol }) {
                return@let ConeContractDescriptionError.NotSelfTypeParameter(typeParameterSymbol)
            }
            runIf(!typeParameterSymbol.isReified) {
                ConeContractDescriptionError.NotReifiedTypeParameter(typeParameterSymbol)
            }
        }
        return when (diagnostic) {
            null -> ConeIsInstancePredicate(arg, type, isNegated)
            else -> KtErroneousIsInstancePredicate(arg, type, isNegated, diagnostic)
        }
    }

    private fun FirExpression.parseInvocationKind(): EventOccurrencesRange? {
        if (this !is FirQualifiedAccessExpression) return null
        val resolvedId = toResolvedCallableSymbol()?.callableId ?: return null
        return when (resolvedId) {
            FirContractsDslNames.EXACTLY_ONCE_KIND -> EventOccurrencesRange.EXACTLY_ONCE
            FirContractsDslNames.AT_LEAST_ONCE_KIND -> EventOccurrencesRange.AT_LEAST_ONCE
            FirContractsDslNames.AT_MOST_ONCE_KIND -> EventOccurrencesRange.AT_MOST_ONCE
            FirContractsDslNames.UNKNOWN_KIND -> EventOccurrencesRange.UNKNOWN
            else -> null
        }
    }

    private fun noReceiver(callableId: CallableId): KtErroneousContractElement<ConeKotlinType, ConeDiagnostic> {
        return ConeContractDescriptionError.NoReceiver(callableId.callableName).asElement()
    }

    private fun noArgument(callableId: CallableId): KtErroneousContractElement<ConeKotlinType, ConeDiagnostic> {
        return ConeContractDescriptionError.NoArgument(callableId.callableName).asElement()
    }

    private fun FirElement.asContractElement(): ConeContractDescriptionElement {
        return accept(this@ConeEffectExtractor, null)
    }

    private fun FirExpression.asContractBooleanExpression(): ConeBooleanExpression {
        return when (val element = asContractElement()) {
            is ConeBooleanExpression -> element
            else -> ConeContractDescriptionError.NotABooleanExpression(element).asElement()
        }
    }

    private fun FirExpression.asContractValueExpression(): ConeValueParameterReference {
        return when (val element = asContractElement()) {
            is ConeValueParameterReference -> element
            else -> KtErroneousValueParameterReference(ConeContractDescriptionError.NotAParameterReference(element))
        }
    }
}
