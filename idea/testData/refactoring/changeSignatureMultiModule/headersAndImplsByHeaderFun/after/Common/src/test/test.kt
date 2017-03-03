package test

header fun foo()
header fun baz(n: Int)
header fun bar(n: Int)

fun test() {
    foo()
    baz(1)
    bar(1)
}