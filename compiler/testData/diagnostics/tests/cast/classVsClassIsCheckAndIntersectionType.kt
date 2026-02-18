// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766

open class A
open class B
open class C

fun test1(a: A) {
    <!USELESS_IS_CHECK!>a is B<!> && <!USELESS_IS_CHECK!>a is C<!>
}

fun test2(a: A) {
    <!USELESS_IS_CHECK!>a !is B<!> && return
    <!USELESS_IS_CHECK!>a is C<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
