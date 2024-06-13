// DIAGNOSTICS: -UNUSED_PARAMETER

interface First
interface Second

interface A : First, Second
interface B : First, Second

fun <S> intersect(vararg elements: S): S = TODO()

fun runIntersect(arg: A, arg2: A) = run {
    if (arg !is B) throw Exception()
    if (arg2 !is B) throw Exception()
    intersect(arg, arg2)
}

fun test(arg: A) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>runIntersect(arg, arg)<!>
}
