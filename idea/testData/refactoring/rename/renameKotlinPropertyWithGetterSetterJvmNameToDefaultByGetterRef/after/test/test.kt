package test

class A {
    @set:JvmName("setBar")
    var first = 1
}

fun test() {
    A().first
    A().first = 1
}