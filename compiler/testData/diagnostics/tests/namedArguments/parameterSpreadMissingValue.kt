// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

class A
class B

data class Args(val a: A)

fun foo(a: A, b: B) {}

fun test(args: Args) {
    foo(<!NO_VALUE_FOR_PARAMETER!>...args)<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor, propertyDeclaration */
