package test

class A {
    @set:JvmName("setBar")
    var second = 1
}

fun test() {
    A().second
    A().second = 1
}