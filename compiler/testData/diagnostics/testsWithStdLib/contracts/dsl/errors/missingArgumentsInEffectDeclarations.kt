// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-80947

import kotlin.contracts.*

fun test(x: Any) {
    contract {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>callsInPlace<!><!NO_VALUE_FOR_PARAMETER!>()<!>
        <!ERROR_IN_CONTRACT_DESCRIPTION!>true.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OPT_IN_USAGE_ERROR!>holdsIn<!><!NO_VALUE_FOR_PARAMETER!>()<!><!>
        true.<!OPT_IN_USAGE_ERROR!>implies<!>(<!UNRESOLVED_REFERENCE!>a<!><!DEBUG_INFO_MISSING_UNRESOLVED!>==<!><!SYNTAX!><!>)
        true.<!OPT_IN_USAGE_ERROR!>implies<!>(<!SYNTAX!><!SYNTAX!><!>is<!> <!UNRESOLVED_REFERENCE!>Int<!><!SYNTAX!><!SYNTAX!><!>)<!>
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contractHoldsInEffect, contractImpliesReturnEffect, contracts,
equalityExpression, functionDeclaration, lambdaLiteral */
