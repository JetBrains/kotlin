package test

class E1: Exception()

@Throws(E1::class) @JvmOverloads
fun one(a: Int = 1) {}

class One @Throws(E1::class) constructor(a: Int = 1) {
    @Throws(E1::class)
    fun one(a: Int = 1) {}
}