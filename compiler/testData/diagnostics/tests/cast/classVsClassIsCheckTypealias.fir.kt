// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766
// LANGUAGE: +NestedTypeAliases

open class A
open class B

typealias TypeAlias = B

fun test1(a: A) = <!IMPOSSIBLE_IS_CHECK_ERROR!>a is TypeAlias<!>

open class WithNestedTypealias {
    typealias NestedTypealias = WithNestedTypealias
}

fun test2(a: A) = <!IMPOSSIBLE_IS_CHECK_ERROR!>a is WithNestedTypealias.NestedTypealias<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, typeAliasDeclaration */
