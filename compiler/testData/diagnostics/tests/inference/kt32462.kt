// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-32462

fun <K> select(x: K, y: K): K = x

interface A {
    fun toB(): B
    fun toC(): C
    fun toC(x: Int): C
}
interface B
interface C

fun test_1() {
    <!DEBUG_INFO_EXPRESSION_TYPE("(A) -> kotlin.Any")!>select(
        { a: A -> a.toB() },
        { a: A -> a.toC() }
    )<!>
}

fun test_2() {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<A, kotlin.Any>")!>select(
        A::toB,
        A::toC
    )<!>
}
