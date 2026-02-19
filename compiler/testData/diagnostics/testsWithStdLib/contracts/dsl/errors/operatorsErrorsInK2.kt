// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +AllowContractsOnSomeOperators

import kotlin.contracts.*

class A

operator fun A.invoke(): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (this@invoke == A())<!>
    }
    return true
}

class B

operator fun B.invoke(): Boolean {
    contract {
        <!ANNOTATION_IN_CONTRACT_ERROR!>@Suppress("SENSELESS_COMPARISON")<!>
        returns(true) implies (this@invoke != null)
    }
    return true
}

fun test() {
    operator fun Int.invoke(x: Boolean) {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (x) }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, equalityExpression,
funWithExtensionReceiver, functionDeclaration, lambdaLiteral, localFunction, operator, thisExpression */
