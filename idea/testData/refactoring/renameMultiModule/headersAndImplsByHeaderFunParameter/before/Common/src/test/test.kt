package test

expect fun foo(/*rename*/n: Int)

fun test() {
    foo(1)
    foo(n = 1)
}