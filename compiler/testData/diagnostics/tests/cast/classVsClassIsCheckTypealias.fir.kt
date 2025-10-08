// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76766
// LANGUAGE: +NestedTypeAliases

open class A
open class B

typealias TypeAlias = B

fun test1(a: A) = <!IMPOSSIBLE_IS_CHECK_WARNING!>a is TypeAlias<!>

open class WithNestedTypealias {
    typealias NestedTypealias = WithNestedTypealias
}

fun test2(a: A) = <!IMPOSSIBLE_IS_CHECK_WARNING!>a is WithNestedTypealias.NestedTypealias<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, typeAliasDeclaration */
