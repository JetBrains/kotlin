/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

abstract class IrStatementOriginImpl(val debugName: String) : IrStatementOrigin {
    override fun toString(): String = debugName
}

interface IrStatementOrigin {
    object SAFE_CALL : IrStatementOriginImpl("SAFE_CALL")

    object UMINUS : IrStatementOriginImpl("UMINUS")
    object UPLUS : IrStatementOriginImpl("UPLUS")
    object EXCL : IrStatementOriginImpl("EXCL")
    object EXCLEXCL : IrStatementOriginImpl("EXCLEXCL")

    object ELVIS : IrStatementOriginImpl("ELVIS")

    object LT : IrStatementOriginImpl("LT")
    object GT : IrStatementOriginImpl("GT")
    object LTEQ : IrStatementOriginImpl("LTEQ")
    object GTEQ : IrStatementOriginImpl("GTEQ")

    object EQEQ : IrStatementOriginImpl("EQEQ")
    object EQEQEQ : IrStatementOriginImpl("EQEQEQ")
    object EXCLEQ : IrStatementOriginImpl("EXCLEQ")
    object EXCLEQEQ : IrStatementOriginImpl("EXCLEQEQ")

    object IN : IrStatementOriginImpl("IN")
    object NOT_IN : IrStatementOriginImpl("NOT_IN")
    object ANDAND : IrStatementOriginImpl("ANDAND")
    object OROR : IrStatementOriginImpl("OROR")

    object PLUS : IrStatementOriginImpl("PLUS")
    object MINUS : IrStatementOriginImpl("MINUS")
    object MUL : IrStatementOriginImpl("MUL")
    object DIV : IrStatementOriginImpl("DIV")
    object PERC : IrStatementOriginImpl("PERC")
    object RANGE : IrStatementOriginImpl("RANGE")
    object RANGE_UNTIL : IrStatementOriginImpl("RANGE_UNTIL")

    object INVOKE : IrStatementOriginImpl("INVOKE")
    object VARIABLE_AS_FUNCTION : IrStatementOriginImpl("VARIABLE_AS_FUNCTION")
    object GET_ARRAY_ELEMENT : IrStatementOriginImpl("GET_ARRAY_ELEMENT")

    object PREFIX_INCR : IrStatementOriginImpl("PREFIX_INCR")
    object PREFIX_DECR : IrStatementOriginImpl("PREFIX_DECR")
    object POSTFIX_INCR : IrStatementOriginImpl("POSTFIX_INCR")
    object POSTFIX_DECR : IrStatementOriginImpl("POSTFIX_DECR")

    object EQ : IrStatementOriginImpl("EQ")
    object PLUSEQ : IrStatementOriginImpl("PLUSEQ")
    object MINUSEQ : IrStatementOriginImpl("MINUSEQ")
    object MULTEQ : IrStatementOriginImpl("MULTEQ")
    object DIVEQ : IrStatementOriginImpl("DIVEQ")
    object PERCEQ : IrStatementOriginImpl("PERCEQ")

    object ARGUMENTS_REORDERING_FOR_CALL : IrStatementOriginImpl("ARGUMENTS_REORDERING_FOR_CALL")
    object DESTRUCTURING_DECLARATION : IrStatementOriginImpl("DESTRUCTURING_DECLARATION")

    object GET_PROPERTY : IrStatementOriginImpl("GET_PROPERTY")
    object GET_LOCAL_PROPERTY : IrStatementOriginImpl("GET_LOCAL_PROPERTY")

    object IF : IrStatementOriginImpl("IF")
    object WHEN : IrStatementOriginImpl("WHEN")
    object WHEN_COMMA : IrStatementOriginImpl("WHEN_COMMA")
    object WHILE_LOOP : IrStatementOriginImpl("WHILE_LOOP")
    object DO_WHILE_LOOP : IrStatementOriginImpl("DO_WHILE_LOOP")
    object FOR_LOOP : IrStatementOriginImpl("FOR_LOOP")
    object FOR_LOOP_ITERATOR : IrStatementOriginImpl("FOR_LOOP_ITERATOR")
    object FOR_LOOP_INNER_WHILE : IrStatementOriginImpl("FOR_LOOP_INNER_WHILE")
    object FOR_LOOP_HAS_NEXT : IrStatementOriginImpl("FOR_LOOP_HAS_NEXT")
    object FOR_LOOP_NEXT : IrStatementOriginImpl("FOR_LOOP_NEXT")

    object LAMBDA : IrStatementOriginImpl("LAMBDA")
    object DEFAULT_VALUE : IrStatementOriginImpl("DEFAULT_VALUE")
    object ANONYMOUS_FUNCTION : IrStatementOriginImpl("ANONYMOUS_FUNCTION")
    object OBJECT_LITERAL : IrStatementOriginImpl("OBJECT_LITERAL")
    object ADAPTED_FUNCTION_REFERENCE : IrStatementOriginImpl("ADAPTED_FUNCTION_REFERENCE")
    object SUSPEND_CONVERSION : IrStatementOriginImpl("SUSPEND_CONVERSION")
    object FUN_INTERFACE_CONSTRUCTOR_REFERENCE : IrStatementOriginImpl("FUN_INTERFACE_CONSTRUCTOR_REFERENCE")

    object INITIALIZE_PROPERTY_FROM_PARAMETER : IrStatementOriginImpl("INITIALIZE_PROPERTY_FROM_PARAMETER")
    object INITIALIZE_FIELD : IrStatementOriginImpl("INITIALIZE_FIELD")

    object PROPERTY_REFERENCE_FOR_DELEGATE : IrStatementOriginImpl("PROPERTY_REFERENCE_FOR_DELEGATE")

    object BRIDGE_DELEGATION : IrStatementOriginImpl("BRIDGE_DELEGATION")

    object SYNTHETIC_NOT_AUTOBOXED_CHECK : IrStatementOriginImpl("SYNTHETIC_NOT_AUTOBOXED_CHECK")

    object PARTIAL_LINKAGE_RUNTIME_ERROR : IrStatementOriginImpl("PARTIAL_LINKAGE_RUNTIME_ERROR")

    data class COMPONENT_N private constructor(val index: Int) : IrStatementOriginImpl("COMPONENT_$index") {
        companion object {
            private val precreatedComponents = Array(32) { i -> COMPONENT_N(i + 1) }

            fun withIndex(index: Int) =
                if (index < precreatedComponents.size)
                    precreatedComponents[index - 1]
                else
                    COMPONENT_N(index)
        }
    }

}

fun IrStatementOrigin.isAssignmentOperatorWithResult() =
    when (this) {
        IrStatementOrigin.PREFIX_INCR, IrStatementOrigin.PREFIX_DECR,
        IrStatementOrigin.POSTFIX_INCR, IrStatementOrigin.POSTFIX_DECR ->
            true
        else ->
            false
    }

fun IrStatementOrigin.isAssignmentOperator(): Boolean =
    when (this) {
        IrStatementOrigin.EQ,
        IrStatementOrigin.PLUSEQ,
        IrStatementOrigin.MINUSEQ,
        IrStatementOrigin.MULTEQ,
        IrStatementOrigin.DIVEQ,
        IrStatementOrigin.PERCEQ -> true
        else -> isAssignmentOperatorWithResult()
    }
