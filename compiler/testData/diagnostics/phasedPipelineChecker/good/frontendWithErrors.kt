// RUN_PIPELINE_TILL: FRONTEND

fun test() {
    <!UNRESOLVED_REFERENCE!>nonExistingFunction<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration */
