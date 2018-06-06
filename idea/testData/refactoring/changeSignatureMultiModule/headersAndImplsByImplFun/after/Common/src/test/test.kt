package test

expect fun foo()
expect fun baz(n: Int)
expect fun bar(n: Int)

fun test() {
    foo()
    baz(1)
    bar(1)
}