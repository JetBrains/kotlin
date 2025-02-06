// RUN_PIPELINE_TILL: BACKEND
// SKIP_FIR_DUMP
// ISSUE: KT-67869
// LANGUAGE: +ResolveTopLevelLambdasAsSyntheticCallArgument

fun interface MyFun {
    fun foo(x: String): Int
}

val topLevel: MyFun = { it.length }

fun baz(x: MyFun = { it.length }): MyFun = x

class A(
    val classMember: MyFun = { it.length }
)

fun returnExpr(): MyFun = { it.length }

fun returnExplicit(): MyFun {
    return { it.length }
}

val withGetter: MyFun
    get() = { it.length }

fun main() {
    var local: MyFun = { it.length }
    local = { it.length + 1 }
}
