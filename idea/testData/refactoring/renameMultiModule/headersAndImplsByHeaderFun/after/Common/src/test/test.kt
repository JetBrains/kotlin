package test

expect fun baz()
expect fun baz(n: Int)
expect fun bar(n: Int)

fun test() {
    baz()
    baz(1)
    bar(1)
}