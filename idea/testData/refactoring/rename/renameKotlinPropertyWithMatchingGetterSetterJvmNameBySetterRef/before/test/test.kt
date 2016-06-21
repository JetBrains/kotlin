package test

class A {
    @get:JvmName("getFoo")
    @set:JvmName("setFoo")
    var first = 1
}

fun test() {
    A().first
    A().first = 1
}