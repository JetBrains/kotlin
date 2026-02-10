// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-80947

import kotlin.contracts.*

fun test(x: Any) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!><!CANNOT_INFER_PARAMETER_TYPE!>callsInPlace<!><!NO_VALUE_FOR_PARAMETER!>()<!><!>
        <!NO_VALUE_FOR_PARAMETER!>true.<!ERROR_IN_CONTRACT_DESCRIPTION!><!CANNOT_INFER_PARAMETER_TYPE, OPT_IN_USAGE_ERROR!>holdsIn<!>()<!><!>
        true.implies(<!UNRESOLVED_REFERENCE!>a<!>==<!SYNTAX!><!>)
        true.implies(<!SYNTAX!><!SYNTAX!><!>is<!> <!UNRESOLVED_REFERENCE!>Int<!><!SYNTAX!><!SYNTAX!><!>)<!>
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contractHoldsInEffect, contractImpliesReturnEffect, contracts,
equalityExpression, functionDeclaration, lambdaLiteral */
