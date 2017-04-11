// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

protocol interface FooBar
class X

fun test1() {
    val x: X = X()
    val y: FooBar = X()

    x is FooBar
    y is FooBar
}

fun test2() {
    val x: X = X()
    val y: FooBar = X()

    x is X
    y is X
}

fun test3() {
    val x: X = X()
    val y: FooBar = X()

    x <!CAST_NEVER_SUCCEEDS!>as<!> FooBar
    y as X
}

fun test4() {
    val x: X = X()
    val y: FooBar = X()

    x <!CAST_NEVER_SUCCEEDS!>as?<!> FooBar
    y as? X
}
