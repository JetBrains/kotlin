// RUN_PIPELINE_TILL: FRONTEND

fun foo(vararg x: Int) {}

fun test() {
    foo(<!UNRESOLVED_REFERENCE!><!SPREAD_OF_NULLABLE!>*<!><!UNRESOLVED_REFERENCE!>Unresolved<!><!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, vararg */
