package test

expect fun foo(<caret>n: Int)

fun test() {
    foo(1)
    foo(n = 1)
}