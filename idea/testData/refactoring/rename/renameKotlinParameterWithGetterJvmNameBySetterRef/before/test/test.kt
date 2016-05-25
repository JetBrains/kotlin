package test

class A(@get:JvmName("getFoo") var first: Int = 1)

fun test() {
    A().first
    A().first = 1
}