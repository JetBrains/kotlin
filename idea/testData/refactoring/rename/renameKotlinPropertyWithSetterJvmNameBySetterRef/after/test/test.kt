package test

class A {
    @set:JvmName("setBarNew")
    var first = 1
}

fun test() {
    A().first
    A().first = 1
}