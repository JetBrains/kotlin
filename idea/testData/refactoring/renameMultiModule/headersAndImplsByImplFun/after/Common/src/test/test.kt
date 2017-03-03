package test

header fun baz()
header fun baz(n: Int)
header fun bar(n: Int)

fun test() {
    baz()
    baz(1)
    bar(1)
}