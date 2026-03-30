// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

class A
class B
class C

data class Head(val a: A)
data class Tail(val b: B, val c: C)

fun foo(a: A, b: B, c: C) {}

fun test(head: Head, tail: Tail, b: B) {
    foo(...head, ...tail, b = b)
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor, propertyDeclaration */
