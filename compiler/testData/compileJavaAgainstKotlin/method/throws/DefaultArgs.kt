package test

class E1: Exception()

throws(E1::class) jvmOverloads
fun one(a: Int = 1) {}

class One @throws(E1::class) constructor(a: Int = 1) {
    throws(E1::class)
    fun one(a: Int = 1) {}
}