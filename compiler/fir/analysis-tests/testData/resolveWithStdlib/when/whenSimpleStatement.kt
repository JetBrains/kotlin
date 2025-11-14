// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun foo(x: Int) {
    when (x) {
        1 -> println("one")
        2 -> println("two")
        // no else since statement (no return)
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
