// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun foo(numbers: Collection<Int>) {
    for (i in numbers) {
        val b: Boolean
        if (1 < 2) {
            b = false
        }
        else {
            b = true
        }
        use(b)
        continue
    }
}

fun use(vararg a: Any?) = a

/* GENERATED_FIR_TAGS: assignment, comparisonExpression, continue, forLoop, functionDeclaration, ifExpression,
integerLiteral, localProperty, nullableType, outProjection, propertyDeclaration, vararg */
