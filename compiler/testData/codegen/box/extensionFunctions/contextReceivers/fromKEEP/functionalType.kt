// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

class Param
class O {
    val o = "o"
}
class K {
    val k = "k"
}

context(O)
fun <T> K.f(g: context(O) K.(Param) -> T) = g(this@O, this@K, Param())

fun box() = with(O()) {
    K().f { o + k }
}