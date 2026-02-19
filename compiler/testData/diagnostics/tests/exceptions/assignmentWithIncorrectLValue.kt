// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-65241

object A

fun test() {
    A.<!SYNTAX!>else<!> = 42
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, integerLiteral, objectDeclaration */
