// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76766

open class A
open class B

fun test(a: A?) = <!USELESS_IS_CHECK!>a is B<!>

fun test2(a: A?) = <!USELESS_IS_CHECK!>a is B?<!>

fun test3(a: A) = <!USELESS_IS_CHECK!>a is B?<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType */
