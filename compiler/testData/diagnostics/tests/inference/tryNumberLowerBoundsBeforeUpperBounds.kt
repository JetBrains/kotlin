// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
public fun <T: Any> iterate(initialValue: T, nextFunction: (T) -> T?): Iterator<T> =
        throw Exception("$initialValue $nextFunction")

fun foo() {
    iterate(3) { n -> if (n > 0) n - 1 else null }
}

/* GENERATED_FIR_TAGS: additiveExpression, comparisonExpression, functionDeclaration, functionalType, ifExpression,
integerLiteral, lambdaLiteral, nullableType, stringLiteral, typeConstraint, typeParameter */
