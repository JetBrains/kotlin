// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun test(x: Int, y: Int): Int =
    when (x) {
        1 -> when (y) {
            1 -> 11
            else -> 1
        }
        else -> 0
    }

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, whenExpression, whenWithSubject */
