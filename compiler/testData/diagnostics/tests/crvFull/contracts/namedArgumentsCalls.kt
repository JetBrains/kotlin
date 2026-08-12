// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun <A, B, R> A.twoLambdasOneResult(first: (A) -> B, second: (B) -> R): R {
    contract {
        returnsResultOf(second)
    }
    return second(first(this))
}

fun <A, B> twoParams(a: A, b: B): B {
    contract {
        returnsParameter(b)
    }
    println(a)
    return b
}

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testReturnsResultOf() {
    "x".<!RETURN_VALUE_NOT_USED!>twoLambdasOneResult<!>({ ign() }, { fooS() })
    "x".twoLambdasOneResult({ fooS() }, { ign() })
    "x".twoLambdasOneResult(second = { ign() }, first = { fooS() })
    "x".<!RETURN_VALUE_NOT_USED!>twoLambdasOneResult<!>(second = { fooS() }, first = { ign() })
}

fun testReturnsParameter() {
    twoParams(fooS(), ign())
    twoParams(b = <!RETURN_VALUE_NOT_USED!>fooS<!>(), a = ign())
    twoParams(ign(), <!RETURN_VALUE_NOT_USED!>fooS<!>())
    twoParams(b = ign(), a = fooS())
}

/* GENERATED_FIR_TAGS: contracts, funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral,
nullableType, stringLiteral, thisExpression, typeParameter */
