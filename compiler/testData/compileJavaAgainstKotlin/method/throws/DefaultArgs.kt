package test

class E1: Exception()

throws(javaClass<E1>()) overloads
fun one(a: Int = 1) {}

class One [throws(javaClass<E1>())] (a: Int = 1) {
    throws(javaClass<E1>())
    fun one(a: Int = 1) {}
}