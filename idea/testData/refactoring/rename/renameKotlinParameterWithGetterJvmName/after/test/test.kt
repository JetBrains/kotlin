package test

class A(@get:JvmName("getFoo") var second: Int = 1)

fun test() {
    A().second
    A().second = 1
}