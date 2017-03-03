package test

header fun foo()
header fun <caret>foo(n: Int)
header fun bar(n: Int)

fun test() {
    foo()
    foo(1)
    bar(1)
}