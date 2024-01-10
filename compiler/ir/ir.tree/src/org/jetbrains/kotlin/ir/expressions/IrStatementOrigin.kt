/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class IrStatementOriginImpl(override val debugName: String) : IrStatementOrigin, ReadOnlyProperty<Any?, IrStatementOriginImpl> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): IrStatementOriginImpl = this

    override fun toString(): String = debugName

    companion object : PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, IrStatementOriginImpl>> {
        override fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, IrStatementOriginImpl> =
            IrStatementOriginImpl(property.name)
    }
}

interface IrStatementOrigin {
    val debugName: String

    companion object {
        val SAFE_CALL by IrStatementOriginImpl

        val UMINUS by IrStatementOriginImpl
        val UPLUS by IrStatementOriginImpl
        val EXCL by IrStatementOriginImpl
        val EXCLEXCL by IrStatementOriginImpl

        val ELVIS by IrStatementOriginImpl

        val LT by IrStatementOriginImpl
        val GT by IrStatementOriginImpl
        val LTEQ by IrStatementOriginImpl
        val GTEQ by IrStatementOriginImpl

        val EQEQ by IrStatementOriginImpl
        val EQEQEQ by IrStatementOriginImpl
        val EXCLEQ by IrStatementOriginImpl
        val EXCLEQEQ by IrStatementOriginImpl

        val IN by IrStatementOriginImpl
        val NOT_IN by IrStatementOriginImpl
        val ANDAND by IrStatementOriginImpl
        val OROR by IrStatementOriginImpl

        val PLUS by IrStatementOriginImpl
        val MINUS by IrStatementOriginImpl
        val MUL by IrStatementOriginImpl
        val DIV by IrStatementOriginImpl
        val PERC by IrStatementOriginImpl
        val RANGE by IrStatementOriginImpl
        val RANGE_UNTIL by IrStatementOriginImpl

        val INVOKE by IrStatementOriginImpl
        val VARIABLE_AS_FUNCTION by IrStatementOriginImpl
        val GET_ARRAY_ELEMENT by IrStatementOriginImpl

        val PREFIX_INCR by IrStatementOriginImpl
        val PREFIX_DECR by IrStatementOriginImpl
        val POSTFIX_INCR by IrStatementOriginImpl
        val POSTFIX_DECR by IrStatementOriginImpl

        val EQ by IrStatementOriginImpl
        val PLUSEQ by IrStatementOriginImpl
        val MINUSEQ by IrStatementOriginImpl
        val MULTEQ by IrStatementOriginImpl
        val DIVEQ by IrStatementOriginImpl
        val PERCEQ by IrStatementOriginImpl

        val ARGUMENTS_REORDERING_FOR_CALL by IrStatementOriginImpl
        val DESTRUCTURING_DECLARATION by IrStatementOriginImpl

        /**
         * Used on an [IrCall] expression to indicate that it's a call to a property getter.
         */
        val GET_PROPERTY by IrStatementOriginImpl

        val GET_LOCAL_PROPERTY by IrStatementOriginImpl

        val IF by IrStatementOriginImpl
        val WHEN by IrStatementOriginImpl
        val WHEN_COMMA by IrStatementOriginImpl
        val WHILE_LOOP by IrStatementOriginImpl
        val DO_WHILE_LOOP by IrStatementOriginImpl
        val FOR_LOOP by IrStatementOriginImpl
        val FOR_LOOP_ITERATOR by IrStatementOriginImpl
        val FOR_LOOP_INNER_WHILE by IrStatementOriginImpl
        val FOR_LOOP_HAS_NEXT by IrStatementOriginImpl
        val FOR_LOOP_NEXT by IrStatementOriginImpl

        val LAMBDA by IrStatementOriginImpl
        val DEFAULT_VALUE by IrStatementOriginImpl
        val ANONYMOUS_FUNCTION by IrStatementOriginImpl
        val OBJECT_LITERAL by IrStatementOriginImpl
        val ADAPTED_FUNCTION_REFERENCE by IrStatementOriginImpl
        val SUSPEND_CONVERSION by IrStatementOriginImpl
        val FUN_INTERFACE_CONSTRUCTOR_REFERENCE by IrStatementOriginImpl

        val INITIALIZE_PROPERTY_FROM_PARAMETER by IrStatementOriginImpl
        val INITIALIZE_FIELD by IrStatementOriginImpl

        val PROPERTY_REFERENCE_FOR_DELEGATE by IrStatementOriginImpl

        val BRIDGE_DELEGATION by IrStatementOriginImpl

        val SYNTHETIC_NOT_AUTOBOXED_CHECK by IrStatementOriginImpl

        val PARTIAL_LINKAGE_RUNTIME_ERROR by IrStatementOriginImpl
    }

    data class COMPONENT_N private constructor(val index: Int) : IrStatementOrigin {
        override val debugName: String = "COMPONENT_$index"

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
