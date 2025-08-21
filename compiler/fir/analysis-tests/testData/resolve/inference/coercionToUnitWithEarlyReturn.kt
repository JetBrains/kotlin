// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-39075

class A {
    fun unit() {}
}

fun foo(x: () -> Unit) {}

fun main(x: A?) {

    val lambda = l@{
        if (x?.hashCode() == 0) return@l

        x?.unit()
    }

    foo(lambda)
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, functionalType, ifExpression,
integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall */
