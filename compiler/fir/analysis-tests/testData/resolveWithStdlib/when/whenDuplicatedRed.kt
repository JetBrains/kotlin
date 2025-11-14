// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun test(x: Int) =
    when (x) {
        1 -> "one"
        <!DUPLICATE_BRANCH_CONDITION_IN_WHEN!>1<!> -> "one dupe"
        else -> "other"
    }

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
