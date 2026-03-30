// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

class A
class B
class C

data class Tail(val b: B, val c: C)

fun foo(a: A, b: B, c: C) {}

fun test(a: A, tail: Tail) {
    foo(a = a, ...tail)
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor, propertyDeclaration */
