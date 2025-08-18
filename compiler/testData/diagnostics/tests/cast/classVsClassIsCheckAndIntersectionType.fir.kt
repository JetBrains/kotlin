// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766

open class A
open class B
open class C

fun test1(a: A) {
    <!IMPOSSIBLE_IS_CHECK_ERROR!>a is B<!> && <!IMPOSSIBLE_IS_CHECK_ERROR!>a is C<!>
}

fun test2(a: A) {
    <!IMPOSSIBLE_IS_CHECK_ERROR!>a !is B<!> && return
    <!IMPOSSIBLE_IS_CHECK_ERROR!>a is C<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
