package test

expect fun foo()
expect fun foo(n: Int)
expect fun bar(n: Int)

fun test() {
    foo()
    foo(1)
    bar(1)
}