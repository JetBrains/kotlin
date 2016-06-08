package test

class A {
    @get:JvmName("getFoo")
    @set:JvmName("setBar")
    var second = 1
}

fun test() {
    A().second
    A().second = 1
}