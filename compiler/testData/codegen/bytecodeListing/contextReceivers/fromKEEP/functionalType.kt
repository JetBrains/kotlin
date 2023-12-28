// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class Param
class O {
    val o = "O"
}
class K {
    val k = "K"
}

context(O)
fun <T> K.f(g: context(O) K.(Param) -> T) = g(this@O, this@K, Param())
