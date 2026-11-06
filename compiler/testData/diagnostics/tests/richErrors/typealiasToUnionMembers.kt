// RUN_PIPELINE_TILL: FRONTEND
error class Foo
error class Bar

typealias R = String | Foo
typealias Errs = Foo | Bar

fun test(x: R, e: Errs) {
    x|.length
    x<!UNSAFE_CALL!>.<!>length
    x.toString()
    x<!UNSAFE_CALL!>.<!>hashCode()
    x<!UNSAFE_CALL!>.<!>foo()
    e.toString()
    e|.toString()
}

fun String.foo() {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, safeCall, typeAliasDeclaration */
