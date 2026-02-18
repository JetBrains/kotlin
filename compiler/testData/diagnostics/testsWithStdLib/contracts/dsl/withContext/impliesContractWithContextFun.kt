// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

class MyEvent {
    fun foo() {}
}

context(event: Any?)
fun returnsContract(): Boolean {
    contract {
        returns() implies (event is MyEvent)
    }
    return event is MyEvent
}

context(event: Boolean)
fun contextInReturn(s: String?): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(event) implies (s is String)<!>
    }
    return s is String
}

context(s: String?, s2: Int?)
fun aFewContexts(): Boolean {
    contract {
        returns(true) implies (s is String && s2 is Int)
    }
    return (s is String && s2 is Int)
}

fun test1(event: Any?) {
    with(event) {
        returnsContract()
        event.<!UNRESOLVED_REFERENCE!>foo<!>()
        this.foo()
    }
}

context(event: Any?)
fun test2() {
    returnsContract()
    event.foo()
}

context(string1: String?, s2: Int?)
fun test3() {
    if(aFewContexts()) {
        string1.length
        s2.inc()
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, contractConditionalEffect, contracts, functionDeclaration,
functionDeclarationWithContext, ifExpression, isExpression, lambdaLiteral, nullableType, smartcast, thisExpression */
