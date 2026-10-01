// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
error class Foo
error class Bar
typealias TA = Foo

fun test(
    a: TA | Bar,
) {
    test2(a)
    test2(Foo() <!USELESS_CAST!>as TA<!>)
}

fun test2(
    a: Foo | Bar,
){
    test(a)
    test(Foo())
    test(Bar())
    test(Foo() <!USELESS_CAST!>as TA<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, typeAliasDeclaration */
