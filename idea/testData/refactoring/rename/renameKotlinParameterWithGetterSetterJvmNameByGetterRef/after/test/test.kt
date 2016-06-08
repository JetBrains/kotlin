package test

class A(@get:JvmName("getFooNew") @set:JvmName("setBar") var first: Int = 1)

fun test() {
    A().first
    A().first = 1
}