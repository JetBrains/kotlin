// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766

open class A
open class B

fun test(a: A?) = <!IMPOSSIBLE_IS_CHECK_ERROR!>a is B<!>

fun test2(a: A?) = <!IMPOSSIBLE_IS_CHECK_ERROR!>a is B?<!>

fun test3(a: A) = <!IMPOSSIBLE_IS_CHECK_ERROR!>a is B?<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType */
