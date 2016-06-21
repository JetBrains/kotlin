package test

class A {
    @get:JvmName("getFooNew")
    @set:JvmName("setFooNew")
    var first = 1
}

fun test() {
    A().first
    A().first = 1
}