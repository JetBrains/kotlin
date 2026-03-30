// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT

fun test(f: (String) -> Unit) {
    f(<!NAMED_ARGUMENTS_NOT_ALLOWED!>p1<!> = "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, stringLiteral */
