// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76766

open class A
open class B
open class C

fun test1(a: A) {
    <!USELESS_IS_CHECK!>a is B<!> && a is C
}

fun test2(a: A) {
    <!USELESS_IS_CHECK!>a !is B<!> && return
    a is C
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
