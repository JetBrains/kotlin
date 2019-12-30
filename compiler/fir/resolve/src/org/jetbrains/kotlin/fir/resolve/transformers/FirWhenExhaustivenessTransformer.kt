/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.ClassId

class FirWhenExhaustivenessTransformer(private val bodyResolveComponents: BodyResolveComponents) : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        throw IllegalArgumentException("Should not be there")
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Nothing?): CompositeTransformResult<FirStatement> {
        val resultExpression = processExhaustivenessCheck(whenExpression) ?: whenExpression
        return resultExpression.compose()
    }

    private fun processExhaustivenessCheck(whenExpression: FirWhenExpression): FirWhenExpression? {
        if (whenExpression.branches.any { it.condition is FirElseIfTrueCondition }) {
            whenExpression.replaceIsExhaustive(true)
            return whenExpression
        }

        val typeRef = (whenExpression.subjectVariable?.returnTypeRef
            ?: (whenExpression.subject as? FirQualifiedAccessExpression)?.typeRef) as? FirResolvedTypeRef
            ?: return null

        val lookupTag = (typeRef.type as? ConeLookupTagBasedType)?.lookupTag ?: return null
        val nullable = typeRef.type.nullability == ConeNullability.NULLABLE
        val isExhaustive = when {
            ((lookupTag as? ConeClassLikeLookupTag)?.classId == bodyResolveComponents.session.builtinTypes.booleanType.id) -> {
                checkBooleanExhaustiveness(whenExpression, nullable)
            }

            else -> {
                val klass = lookupTag.toSymbol(bodyResolveComponents.session)?.fir as? FirRegularClass ?: return null
                when {
                    klass.classKind == ClassKind.ENUM_CLASS -> checkEnumExhaustiveness(whenExpression, klass, nullable)
                    klass.modality == Modality.SEALED -> checkSealedClassExhaustiveness(whenExpression, klass as FirSealedClass, nullable)
                    else -> return null
                }
            }
        }

        return if (isExhaustive) {
            whenExpression.replaceIsExhaustive(true)
            whenExpression
        } else {
            null
        }
    }

    // ------------------------ Enum exhaustiveness ------------------------

    private fun checkEnumExhaustiveness(whenExpression: FirWhenExpression, enum: FirRegularClass, nullable: Boolean): Boolean {
        val data = EnumExhaustivenessData(
            enum.collectEnumEntries().map { it.symbol }.toMutableSet(),
            !nullable
        )
        for (branch in whenExpression.branches) {
            branch.condition.accept(EnumExhaustivenessVisitor, data)
        }
        return data.containsNull && data.remainingEntries.isEmpty()
    }

    private class EnumExhaustivenessData(val remainingEntries: MutableSet<FirVariableSymbol<FirEnumEntry>>, var containsNull: Boolean)

    private object EnumExhaustivenessVisitor : FirVisitor<Unit, EnumExhaustivenessData>() {
        override fun visitElement(element: FirElement, data: EnumExhaustivenessData) {}

        override fun visitOperatorCall(operatorCall: FirOperatorCall, data: EnumExhaustivenessData) {
            if (operatorCall.operation == FirOperation.EQ) {
                when (val argument = operatorCall.arguments[1]) {
                    is FirConstExpression<*> -> {
                        if (argument.value == null) {
                            data.containsNull = true
                        }
                    }
                    is FirQualifiedAccessExpression -> {
                        val reference = argument.calleeReference as? FirResolvedNamedReference ?: return
                        val symbol = (reference.resolvedSymbol.fir as? FirEnumEntry)?.symbol ?: return

                        data.remainingEntries.remove(symbol)
                    }
                }
            }
        }

        override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: EnumExhaustivenessData) {
            if (binaryLogicExpression.kind == LogicOperationKind.OR) {
                binaryLogicExpression.acceptChildren(this, data)
            }
        }
    }

    // ------------------------ Sealed class exhaustiveness ------------------------

    private fun checkSealedClassExhaustiveness(whenExpression: FirWhenExpression, sealedClass: FirSealedClass, nullable: Boolean): Boolean {
        if (sealedClass.inheritors.isEmpty()) return true
        val data = SealedExhaustivenessData(
            sealedClass.session.firSymbolProvider,
            sealedClass.inheritors.toMutableSet(),
            !nullable
        )
        for (branch in whenExpression.branches) {
            branch.condition.accept(SealedExhaustivenessVisitor, data)
        }
        return data.containsNull && data.remainingInheritors.isEmpty()
    }

    private class SealedExhaustivenessData(
        val symbolProvider: FirSymbolProvider,
        val remainingInheritors: MutableSet<ClassId>,
        var containsNull: Boolean
    )

    private object SealedExhaustivenessVisitor : FirDefaultVisitor<Unit, SealedExhaustivenessData>() {
        override fun visitElement(element: FirElement, data: SealedExhaustivenessData) {}

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: SealedExhaustivenessData) {
            if (typeOperatorCall.operation == FirOperation.IS) {
                typeOperatorCall.conversionTypeRef.accept(this, data)
            }
        }

        override fun visitOperatorCall(operatorCall: FirOperatorCall, data: SealedExhaustivenessData) {
            if (operatorCall.operation == FirOperation.EQ) {
                val argument = operatorCall.arguments[1]
                if (argument is FirConstExpression<*> && argument.value == null) {
                    data.containsNull = true
                }
            }
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: SealedExhaustivenessData) {
            val lookupTag = (resolvedTypeRef.type as? ConeLookupTagBasedType)?.lookupTag ?: return
            val symbol = data.symbolProvider.getSymbolByLookupTag(lookupTag) as? FirClassSymbol ?: return
            data.remainingInheritors.remove(symbol.classId)
        }

        override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: SealedExhaustivenessData) {
            if (binaryLogicExpression.kind == LogicOperationKind.OR) {
                binaryLogicExpression.acceptChildren(this, data)
            }
        }
    }

    // ------------------------ Boolean exhaustiveness ------------------------

    private fun checkBooleanExhaustiveness(whenExpression: FirWhenExpression, nullable: Boolean): Boolean {
        val flags = BooleanExhaustivenessFlags(!nullable)
        for (branch in whenExpression.branches) {
            branch.condition.accept(BooleanExhaustivenessVisitor, flags)
        }
        return flags.containsTrue && flags.containsFalse && flags.containsNull
    }

    private class BooleanExhaustivenessFlags(var containsNull: Boolean) {
        var containsTrue = false
        var containsFalse = false
    }

    private object BooleanExhaustivenessVisitor : FirVisitor<Unit, BooleanExhaustivenessFlags>() {
        override fun visitElement(element: FirElement, data: BooleanExhaustivenessFlags) {}

        override fun visitOperatorCall(operatorCall: FirOperatorCall, data: BooleanExhaustivenessFlags) {
            if (operatorCall.operation == FirOperation.EQ) {
                val argument = operatorCall.arguments[1]
                if (argument is FirConstExpression<*>) {
                    when (argument.value) {
                        true -> data.containsTrue = true
                        false -> data.containsFalse = true
                        null -> data.containsNull = true
                    }
                }
            }
        }
    }
}