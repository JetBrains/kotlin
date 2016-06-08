package test

class A(var first: Int = 1)

fun test() {
    A().first
    A().first = 1
}