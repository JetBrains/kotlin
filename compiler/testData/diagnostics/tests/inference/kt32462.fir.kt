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
    select(
        { a: A -> a.toB() },
        { a: A -> a.toC() }
    )
}

fun test_2() {
    select(
        A::toB,
        <!UNRESOLVED_REFERENCE!>A::toC<!>
    )
}
