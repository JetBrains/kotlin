package test

header fun foo()
header fun foo(n: Int)
header fun bar(n: Int)

fun test() {
    foo()
    foo(1)
    bar(1)
}