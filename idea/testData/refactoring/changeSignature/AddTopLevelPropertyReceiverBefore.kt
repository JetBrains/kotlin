package test

class A

open var <caret>p: Int
    get() = 1
    set(value: Int) {}

fun test() {
    val t = p
    p = 1
}