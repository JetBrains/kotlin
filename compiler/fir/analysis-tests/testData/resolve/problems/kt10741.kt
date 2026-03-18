// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-10741

// KT-10741: Intersection types in type-inference

interface A {
    fun foo(): String
}

interface B {
    fun bar(): Int
}

fun <T : A> process(t: T): T = t

fun test(x: A) {
    if (x is B) {
        // x has intersection type A & B here
        val result = process(x)
        result.foo() // OK
        result.bar() // should work if T is inferred as A & B
    }
}

fun <T> identity(x: T): T = x

fun test2(x: A) {
    if (x is B) {
        // x has intersection type A & B here
        val result = identity(x)
        result.foo() // should work if T is inferred as A & B
        result.bar() // should work if T is inferred as A & B
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, interfaceDeclaration, intersectionType, isExpression,
localProperty, nullableType, propertyDeclaration, smartcast, typeConstraint, typeParameter */
