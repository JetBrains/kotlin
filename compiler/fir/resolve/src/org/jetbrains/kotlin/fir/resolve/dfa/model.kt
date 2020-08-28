/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.modality
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// --------------------------------------- Variables ---------------------------------------

data class Identifier(
    val symbol: AbstractFirBasedSymbol<*>,
    val dispatchReceiver: DataFlowVariable?,
    val extensionReceiver: DataFlowVariable?
) {
    override fun toString(): String {
        val callableId = (symbol as? FirCallableSymbol<*>)?.callableId
        return "[$callableId, dispatchReceiver = $dispatchReceiver, extensionReceiver = $extensionReceiver]"
    }
}

sealed class DataFlowVariable(private val variableIndexForDebug: Int) {
    abstract val isStable: Boolean

    final override fun toString(): String {
        return "d$variableIndexForDebug"
    }
}

class RealVariable(
    val identifier: Identifier,
    val isThisReference: Boolean,
    val explicitReceiverVariable: DataFlowVariable?,
    variableIndexForDebug: Int
) : DataFlowVariable(variableIndexForDebug) {
    override val isStable: Boolean by lazy {
        when (val symbol = identifier.symbol) {
            is FirPropertySymbol -> {
                val property = symbol.fir
                when {
                    property.isLocal -> true
                    property.isVar -> false
                    property.modality != Modality.FINAL -> false
                    property.receiverTypeRef != null -> false
                    property.getter != null -> false
                    // TODO: getters, delegates
                    else -> true
                }
            }
            else -> true
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    private val _hashCode by lazy {
        31 * identifier.hashCode() + (explicitReceiverVariable?.hashCode() ?: 0)
    }

    override fun hashCode(): Int {
        return _hashCode
    }
}

class RealVariableAndType(val variable: RealVariable, val originalType: ConeKotlinType?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RealVariableAndType

        if (variable != other.variable) return false
        if (originalType != other.originalType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = variable.hashCode()
        result = 31 * result + originalType.hashCode()
        return result
    }
}

class SyntheticVariable(val fir: FirElement, variableIndexForDebug: Int) : DataFlowVariable(variableIndexForDebug) {
    override val isStable: Boolean get() = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SyntheticVariable

        return fir isEqualsTo other.fir
    }

    override fun hashCode(): Int {
        // hack for enums
        return if (fir is FirResolvedQualifier) {
            31 * fir.packageFqName.hashCode() + fir.classId.hashCode()
        } else {
            fir.hashCode()
        }
    }
}

private infix fun FirElement.isEqualsTo(other: FirElement): Boolean {
    if (this !is FirResolvedQualifier || other !is FirResolvedQualifier) return this == other
    if (packageFqName != other.packageFqName) return false
    if (classId != other.classId) return false
    return true
}

// --------------------------------------- Facts ---------------------------------------

sealed class Statement<T : Statement<T>> {
    abstract fun invert(): T
}

/*
 * Examples:
 * d == Null
 * d != Null
 * d == True
 * d == False
 */
data class OperationStatement(val variable: DataFlowVariable, val operation: Operation) : Statement<OperationStatement>() {
    override fun invert(): OperationStatement {
        return OperationStatement(variable, operation.invert())
    }

    override fun toString(): String {
        return "$variable $operation"
    }
}

abstract class TypeStatement : Statement<TypeStatement>() {
    abstract val variable: RealVariable
    abstract val exactType: Set<ConeKotlinType>
    abstract val exactNotType: Set<ConeKotlinType>

    abstract operator fun plus(other: TypeStatement): TypeStatement
    abstract val isEmpty: Boolean
    val isNotEmpty: Boolean get() = !isEmpty

    override fun toString(): String {
        return "$variable: $exactType, $exactNotType"
    }
}

operator fun TypeStatement.plus(other: TypeStatement?): TypeStatement = other?.let { this + other } ?: this

class MutableTypeStatement(
    override val variable: RealVariable,
    override val exactType: MutableSet<ConeKotlinType> = linkedSetOf(),
    override val exactNotType: MutableSet<ConeKotlinType> = linkedSetOf()
) : TypeStatement() {
    override fun plus(other: TypeStatement): MutableTypeStatement = MutableTypeStatement(
        variable,
        LinkedHashSet(exactType).apply { addAll(other.exactType) },
        LinkedHashSet(exactNotType).apply { addAll(other.exactNotType) }
    )

    override val isEmpty: Boolean
        get() = exactType.isEmpty() && exactType.isEmpty()

    override fun invert(): MutableTypeStatement {
        return MutableTypeStatement(
            variable,
            LinkedHashSet(exactNotType),
            LinkedHashSet(exactType)
        )
    }

    operator fun plusAssign(info: TypeStatement) {
        exactType += info.exactType
        exactNotType += info.exactNotType
    }

    fun copy(): MutableTypeStatement = MutableTypeStatement(variable, LinkedHashSet(exactType), LinkedHashSet(exactNotType))
}

class Implication(
    val condition: OperationStatement,
    val effect: Statement<*>
) {
    override fun toString(): String {
        return "$condition -> $effect"
    }
}

fun Implication.invertCondition(): Implication = Implication(condition.invert(), effect)

// --------------------------------------- Aliases ---------------------------------------

typealias TypeStatements = Map<RealVariable, TypeStatement>
typealias MutableTypeStatements = MutableMap<RealVariable, MutableTypeStatement>

fun MutableTypeStatements.addStatement(variable: RealVariable, statement: TypeStatement) {
    put(variable, statement.asMutableStatement()) { it.apply { this += statement } }
}

fun MutableTypeStatements.mergeTypeStatements(other: TypeStatements) {
    other.forEach { (variable, info) ->
        addStatement(variable, info)
    }
}

// --------------------------------------- DSL ---------------------------------------

infix fun DataFlowVariable.eq(constant: Boolean?): OperationStatement {
    val condition = when (constant) {
        true -> Operation.EqTrue
        false -> Operation.EqFalse
        null -> Operation.EqNull
    }
    return OperationStatement(this, condition)
}

infix fun DataFlowVariable.notEq(constant: Boolean?): OperationStatement {
    val condition = when (constant) {
        true -> Operation.EqFalse
        false -> Operation.EqTrue
        null -> Operation.NotEqNull
    }
    return OperationStatement(this, condition)
}

infix fun OperationStatement.implies(effect: Statement<*>): Implication = Implication(this, effect)

infix fun RealVariable.typeEq(type: ConeKotlinType): TypeStatement =
    if (type !is ConeClassErrorType) {
        MutableTypeStatement(this, linkedSetOf<ConeKotlinType>().apply { this += type }, HashSet())
    } else {
        MutableTypeStatement(this)
    }

infix fun RealVariable.typeNotEq(type: ConeKotlinType): TypeStatement =
    if (type !is ConeClassErrorType) {
        MutableTypeStatement(this, linkedSetOf(), LinkedHashSet<ConeKotlinType>().apply { this += type })
    } else {
        MutableTypeStatement(this)
    }

// --------------------------------------- Utils ---------------------------------------

@OptIn(ExperimentalContracts::class)
fun DataFlowVariable.isSynthetic(): Boolean {
    contract {
        returns(true) implies (this@isSynthetic is SyntheticVariable)
        returns(false) implies (this@isSynthetic is RealVariable)
    }
    return this is SyntheticVariable
}

@OptIn(ExperimentalContracts::class)
fun DataFlowVariable.isReal(): Boolean {
    contract {
        returns(true) implies (this@isReal is RealVariable)
        returns(false) implies (this@isReal is SyntheticVariable)
    }
    return this is RealVariable
}
