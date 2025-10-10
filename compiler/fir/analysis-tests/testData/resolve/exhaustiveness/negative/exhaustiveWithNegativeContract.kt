// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +DataFlowBasedExhaustiveness
// WITH_STDLIB

import kotlin.contracts.*

sealed interface MySealedInterface {
    object A : MySealedInterface
    object B : MySealedInterface
    object C : MySealedInterface
}

sealed interface Variants {
    data object A : Variants
    data object B : Variants
}

fun ensureNotA(v: Variants) {
    contract {
        returns() implies (v !is Variants.A)
    }
    if (v is Variants.A) throw Exception("Forbidden")
}

fun customContract(v: Variants): String {
    ensureNotA(v)

    return when (v) {
        is Variants.B -> "B"
    }
}

fun requireContracts(x: MySealedInterface): Int {
    require(x !is MySealedInterface.A)
    if (x is MySealedInterface.C) return 1
    return when (x) {
        MySealedInterface.B -> 3
    }
}


fun checkContracts(x: MySealedInterface): Int {
    check(x !is MySealedInterface.A) { "x must not be A" }
    if (x is MySealedInterface.C) return 1
    return when (x) {
        MySealedInterface.B -> 3
    }
}

fun notNullContracts(x: MySealedInterface?): Int {
    requireNotNull(x)
    if (x is MySealedInterface.C) return 1
    return when (x) {
        MySealedInterface.A -> 2
        MySealedInterface.B -> 3
    }
}

fun checkNotNullWhen(x: MySealedInterface?): Int {
    checkNotNull(x)
    if (x is MySealedInterface.C) return 1
    return when (x) {
        MySealedInterface.A -> 2
        MySealedInterface.B -> 3
    }
}

fun assertBool(x: Boolean): Int {
    assert(x)
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        true -> 2
    }
}

@OptIn(ExperimentalContracts::class)
fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
        returns(false) implies (x !is String)
    }
    return x is String
}

fun customContract1(x: Any?) {
    !isString(x)
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        !is String -> 1
    }
}

fun takeIfSample(x: String): Int {
    val nonEmpty = x.takeIf { it.isNotEmpty() }
    if (nonEmpty == null) return 1
    return when (x) {
        <!USELESS_IS_CHECK!>is String<!> -> 2
    }
}

fun letContract(x: String?) {
    x?.let {
        when (x) {
            <!USELESS_IS_CHECK!>is String<!> -> 1
        }
    }
}

lateinit var x: MySealedInterface
fun lateinitCheck(): Int {
    if (::x.isInitialized and (x is MySealedInterface.C)) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is MySealedInterface.B -> 2
        is MySealedInterface.A -> 3
    }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, data, functionDeclaration, ifExpression,
interfaceDeclaration, isExpression, lambdaLiteral, nestedClass, objectDeclaration, sealed, smartcast, stringLiteral,
whenExpression, whenWithSubject */
