// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// SKIP_TXT

class A
class B
class C

data class Tail(val c: C)

fun foo(a: A, b: B, c: C) {}

fun test(a: A, tail: Tail) {
    foo(a, <!NO_VALUE_FOR_PARAMETER!>...tail)<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor, propertyDeclaration */
