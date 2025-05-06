// RUN_PIPELINE_TILL: FRONTEND
// SKIP_FIR_DUMP
// ISSUE: KT-67869
// LANGUAGE: -ResolveTopLevelLambdasAsSyntheticCallArgument

fun expectAny(a: Any) {}

var b: Boolean = false

fun <E> myEmptyList(): List<E> = TODO()

fun main() {
    expectAny(x@{
        if (b) return@x myEmptyList()

        myEmptyList<String>()
    })
}

val x: Any = x@{
    if (b) return@x <!CANNOT_INFER_PARAMETER_TYPE!>myEmptyList<!>()

    myEmptyList<String>()
}
