// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766

open class A
open class B

fun test(a: A?) = <!USELESS_IS_CHECK!>a is B<!>

fun test2(a: A?) = a is B?

fun test3(a: A) = <!USELESS_IS_CHECK!>a is B<!USELESS_NULLABLE_CHECK!>?<!><!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType */
