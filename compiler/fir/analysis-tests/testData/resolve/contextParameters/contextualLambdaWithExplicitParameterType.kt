// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-77432

fun <T> foo(a: T) {}

fun usage() {
    foo<context(Int) (Int) -> Unit> { contextOf<Int>() }
    foo<context(Int) (Int) -> Unit> { a -> contextOf<Int>() }
    foo<context(Int) (Int) -> Unit> <!ARGUMENT_TYPE_MISMATCH!>{ a: Int -> <!NO_CONTEXT_ARGUMENT!>contextOf<!><Int>() }<!>
}
