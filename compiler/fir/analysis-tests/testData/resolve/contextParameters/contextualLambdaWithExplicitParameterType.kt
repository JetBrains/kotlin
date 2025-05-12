// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-77432

fun <T> foo(a: T) {}

fun usage() {
    foo<context(Int) (Int) -> Unit> { contextOf<Int>() }
    foo<context(Int) (Int) -> Unit> { a -> contextOf<Int>() }
    foo<context(Int) (Int) -> Unit> { a: Int -> contextOf<Int>() }
}

fun <T> id(x: T) = x
fun <T> select(vararg x: T) = x[0]

val x1 = select(id { contextOf<Int>() }, context(c: Int) fun () = c)