// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A {
    operator fun rangeUntil(other: A): Iterable<A> = TODO()
}

fun main(n: A, f: A) {
    for (i in f..<n) {

    }
}

/* GENERATED_FIR_TAGS: classDeclaration, forLoop, functionDeclaration, localProperty, operator, propertyDeclaration,
rangeExpression */
