// RUN_PIPELINE_TILL: BACKEND
fun foo(x: (Int) -> Unit) {}

class A {
    fun bar() {}
}

fun main(a: A?, y: String) {
    foo {
        a?.bar() ?: y.get(0)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, nullableType, safeCall */
