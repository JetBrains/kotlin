package test

class E1 : Exception()
class E2 : Exception()

@kotlin.Throws(E1::class)
fun kt() {}

@kotlin.jvm.Throws(E2::class)
fun ktJvm() {}
