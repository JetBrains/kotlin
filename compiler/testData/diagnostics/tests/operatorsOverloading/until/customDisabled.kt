// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -RangeUntilOperator

class A {
    <!UNSUPPORTED_FEATURE("The feature \"range until operator\" is disabled")!>operator<!> fun rangeUntil(other: A): Iterable<A> = TODO()
}

fun main(n: A, f: A) {
    for (i in f<!UNSUPPORTED_FEATURE("The feature \"range until operator\" is disabled")!>..<<!>n) {

    }
}

/* GENERATED_FIR_TAGS: classDeclaration, forLoop, functionDeclaration, localProperty, operator, propertyDeclaration,
rangeExpression */
