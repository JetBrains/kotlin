// RUN_PIPELINE_TILL: FRONTEND
// SKIP_FIR_DUMP
// ISSUE: KT-67869
//// LANGUAGE: +ResolveTopLevelLambdasAsSyntheticCallArgument

fun interface MyFun {
    fun foo(x: String): Int
}

val topLevel: MyFun = <!INITIALIZER_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!>.length }<!>

fun baz(x: MyFun = <!INITIALIZER_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!>.length }<!>): MyFun = x

class A(
    val classMember: MyFun = <!INITIALIZER_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!>.length }<!>
)

fun returnExpr(): MyFun = <!RETURN_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!>.length }<!>

fun returnExplicit(): MyFun {
    return <!RETURN_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!>.length }<!>
}

val withGetter: MyFun
    get() = <!RETURN_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!>.length }<!>

fun main() {
    var local: MyFun = <!INITIALIZER_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!>.length }<!>
    local = <!ASSIGNMENT_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!>.length + 1 }<!>
}
