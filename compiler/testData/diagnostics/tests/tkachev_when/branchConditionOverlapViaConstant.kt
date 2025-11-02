// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
const val x = 1

fun foo(y: Int) {
    when (y) {
        1 -> {}
        2 -> {}
        x -> {}
        else -> {}
    }
}

/* GENERATED_FIR_TAGS: const, equalityExpression, functionDeclaration, integerLiteral, propertyDeclaration,
whenExpression, whenWithSubject */
