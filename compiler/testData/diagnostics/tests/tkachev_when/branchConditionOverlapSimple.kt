// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
fun foo(x: Int): String {
    return when (x) {
        1 -> "1"
        2 -> "2"
        <!DUPLICATE_BRANCH_CONDITION_IN_WHEN!>1<!> -> "3"
        else -> "0"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
