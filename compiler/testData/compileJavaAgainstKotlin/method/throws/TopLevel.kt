package test

class E1: Exception()
class E2: Exception()

throws()
fun none() {}

throws(javaClass<E1>())
fun one() {}

throws(javaClass<E1>(), javaClass<E2>())
fun two() {}