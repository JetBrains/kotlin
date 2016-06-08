package test

class A(@get:JvmName("getFoo") @set:JvmName("setBar") var second: Int = 1)

fun test() {
    A().second
    A().second = 1
}