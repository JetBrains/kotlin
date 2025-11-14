// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun sumUntilZero(list: List<Int>): Int {
    var sum = 0
    for (x in list) {
        when {
            x > 0 -> sum += x
            x == 0 -> break
            else -> continue
        }
    }
    return sum
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, break, comparisonExpression, continue, equalityExpression,
forLoop, functionDeclaration, integerLiteral, localProperty, propertyDeclaration, whenExpression */
