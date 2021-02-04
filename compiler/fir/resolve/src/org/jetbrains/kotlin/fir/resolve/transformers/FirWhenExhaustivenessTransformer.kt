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
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.runIf

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

        val typeRef = whenExpression.subjectVariable?.returnTypeRef
            ?: whenExpression.subject?.typeRef
            ?: return null

        // TODO: add some report logic about flexible type (see WHEN_ENUM_CAN_BE_NULL_IN_JAVA diagnostic in old frontend)
        val type = typeRef.coneType.lowerBoundIfFlexible()
        val lookupTag = (type as? ConeLookupTagBasedType)?.lookupTag ?: return null
        val nullable = type.nullability == ConeNullability.NULLABLE
        val isExhaustive = when {
            ((lookupTag as? ConeClassLikeLookupTag)?.classId == bodyResolveComponents.session.builtinTypes.booleanType.id) -> {
                checkBooleanExhaustiveness(whenExpression, nullable)
            }

            whenExpression.branches.isEmpty() -> false

            else -> {
                val klass = lookupTag.toSymbol(bodyResolveComponents.session)?.fir as? FirRegularClass ?: return null
                when {
                    klass.classKind == ClassKind.ENUM_CLASS -> checkEnumExhaustiveness(whenExpression, klass, nullable)
                    klass.modality == Modality.SEALED -> checkSealedClassExhaustiveness(whenExpression, klass, nullable)
                    else -> return null
                }
            }
        }

        return runIf(isExhaustive) {
            whenExpression.replaceIsExhaustive(true)
            whenExpression
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

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: EnumExhaustivenessData) {
            if (equalityOperatorCall.operation.let { it == FirOperation.EQ || it == FirOperation.IDENTITY }) {
                when (val argument = equalityOperatorCall.arguments[1]) {
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

    private fun checkSealedClassExhaustiveness(
        whenExpression: FirWhenExpression,
        sealedClass: FirRegularClass,
        nullable: Boolean
    ): Boolean {
        if (sealedClass.sealedInheritors.isNullOrEmpty()) return true

        val data = SealedExhaustivenessData(sealedClass, !nullable)
        for (branch in whenExpression.branches) {
            branch.condition.accept(SealedExhaustivenessVisitor, data)
        }
        return data.isExhaustive()
    }

    private inner class SealedExhaustivenessData(regularClass: FirRegularClass, var containsNull: Boolean) {
        val symbolProvider = bodyResolveComponents.symbolProvider
        private val rootNode = SealedClassInheritors(regularClass.classId, regularClass.sealedInheritors.mapToSealedInheritors())

        private fun List<ClassId>?.mapToSealedInheritors(): MutableSet<SealedClassInheritors>? {
            if (this.isNullOrEmpty()) return null

            return this.mapNotNull {
                val inheritor = symbolProvider.getClassLikeSymbolByFqName(it)?.fir as? FirRegularClass ?: return@mapNotNull null
                SealedClassInheritors(inheritor.classId, inheritor.sealedInheritors.mapToSealedInheritors())
            }.takeIf { it.isNotEmpty() }?.toMutableSet()
        }

        fun removeInheritor(classId: ClassId) {
            if (rootNode.classId == classId) {
                rootNode.inheritors?.clear()
                return
            }

            rootNode.removeInheritor(classId)
        }

        fun isExhaustive() = containsNull && rootNode.isEmpty()
    }

    private data class SealedClassInheritors(val classId: ClassId, val inheritors: MutableSet<SealedClassInheritors>? = null) {
        fun removeInheritor(classId: ClassId): Boolean {
            return inheritors != null && (inheritors.removeIf { it.classId == classId } || inheritors.any { it.removeInheritor(classId) })
        }

        fun isEmpty(): Boolean {
            return inheritors != null && inheritors.all { it.isEmpty() }
        }
    }

    private object SealedExhaustivenessVisitor : FirDefaultVisitor<Unit, SealedExhaustivenessData>() {
        override fun visitElement(element: FirElement, data: SealedExhaustivenessData) {}

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: SealedExhaustivenessData) {
            if (typeOperatorCall.operation == FirOperation.IS) {
                typeOperatorCall.conversionTypeRef.accept(this, data)
            }
        }

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: SealedExhaustivenessData) {
            if (equalityOperatorCall.operation.let { it == FirOperation.EQ || it == FirOperation.IDENTITY }) {
                when (val argument = equalityOperatorCall.arguments[1]) {
                    is FirConstExpression<*> -> {
                        if (argument.value == null) {
                            data.containsNull = true
                        }
                    }

                    is FirResolvedQualifier -> {
                        argument.typeRef.accept(this, data)
                    }
                }
            }
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: SealedExhaustivenessData) {
            val lookupTag = (resolvedTypeRef.type as? ConeLookupTagBasedType)?.lookupTag ?: return
            val symbol = data.symbolProvider.getSymbolByLookupTag(lookupTag) as? FirClassSymbol ?: return
            data.removeInheritor(symbol.classId)
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

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: BooleanExhaustivenessFlags) {
            if (equalityOperatorCall.operation.let { it == FirOperation.EQ || it == FirOperation.IDENTITY }) {
                val argument = equalityOperatorCall.arguments[1]
                if (argument is FirConstExpression<*>) {
                    when (argument.value) {
                        true -> data.containsTrue = true
                        false -> data.containsFalse = true
                        null -> data.containsNull = true
                    }
                }
            }
        }

        override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: BooleanExhaustivenessFlags) {
            if (binaryLogicExpression.kind == LogicOperationKind.OR) {
                binaryLogicExpression.acceptChildren(this, data)
            }
        }
    }
}
