// !LANGUAGE: +ContextReceivers
// KT-61141: K1/Native does not support context receivers
// IGNORE_BACKEND_K1: NATIVE

class Param
class O {
    val o = "O"
}
class K {
    val k = "K"
}

context(O)
fun <T> K.f(g: context(O) K.(Param) -> T) = g(this@O, this@K, Param())

fun box() = with(O()) {
    K().f { o + k }
}
