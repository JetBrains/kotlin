// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

class A
class B

data class Args(val a: A)

fun foo(a: A, b: B = B()) {}

fun test(args: Args, b: B) {
    foo(...args, <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>b<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor, propertyDeclaration */
