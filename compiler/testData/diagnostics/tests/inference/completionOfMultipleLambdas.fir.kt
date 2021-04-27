// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER

fun <K> select(x: K, y: K): K = x
fun <K> select3(x: K, y: K, z: K): K = x

interface A
interface B

fun test() {
    select3(
        { a: A, b: A -> Unit },
        { b, a -> Unit },
        <!TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!>; Unit }<!>
    )
}

// ISSUE: KT-27999
// ISSUE: KT-30244
fun test_1() {
    select(
        { 1 },
        { "" }
    )
}

// ISSUE: KT-31102
fun bar(): Int = 1
fun test_2(x: Int) {
    val f1: () -> Int = select({ bar() }, ::bar) // TYPE_MISMATCH on lambda
    val f2 = select({ bar() }, ::bar) // Same
}
