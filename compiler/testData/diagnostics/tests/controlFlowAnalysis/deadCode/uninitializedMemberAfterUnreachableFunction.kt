// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83382

fun error(): Nothing = null!!

class Foo {
    fun bar(): Int = error()
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val foo: String<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var baz: String<!>
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, propertyDeclaration */
