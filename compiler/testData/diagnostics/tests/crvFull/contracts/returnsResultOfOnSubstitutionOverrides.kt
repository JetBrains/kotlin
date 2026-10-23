// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// ISSUE: KT-90664
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun compute(): Int = 42

open class Box<T>(val value: T) {
    inline fun <R> fold(block: (T) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            returnsResultOf(block)
        }
        return block(value)
    }

    fun <S> id(x: S): S {
        contract {
            returnsParameter(x)
        }
        return x
    }

    fun unsubstituted(sb: StringBuilder) {
        fold { sb.append(it) }
        <!RETURN_VALUE_NOT_USED!>fold<!> { compute() }
        id(sb.append(""))
    }
}

class Derived : Box<String>("")

fun substituted(box: Box<String>, sb: StringBuilder) {
    box.fold { sb.append(it) }
    box.<!RETURN_VALUE_NOT_USED!>fold<!> { compute() }
    box.id(sb.append(""))
}

fun inherited(derived: Derived, sb: StringBuilder) {
    derived.fold { sb.append(it) }
    derived.<!RETURN_VALUE_NOT_USED!>fold<!> { compute() }
    derived.id(sb.append(""))
}

/* GENERATED_FIR_TAGS: classDeclaration, contractCallsEffect, contracts, flexibleType, functionDeclaration,
functionalType, inline, integerLiteral, javaFunction, lambdaLiteral, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, typeParameter */
