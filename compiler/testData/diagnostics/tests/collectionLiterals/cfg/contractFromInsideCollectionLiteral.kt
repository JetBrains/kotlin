// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun checkList(x: Any?) {
    contract {
        returns() implies (x is List<*>)
    }
    x as List<*>
}

fun callWithTwoArgs(a: Any, b: Any) { }

fun testReturns(x: Any, y: Any) {
    [[checkList(x)]]
    x.size
    callWithTwoArgs([checkList(y)], y.size)
}

fun testCallsInPlace() {
    val x: Int
    val s = [[run { x = 42 }]]
    x.toString()
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration */
