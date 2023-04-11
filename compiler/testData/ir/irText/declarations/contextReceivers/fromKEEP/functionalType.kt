// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND: JS_IR

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57435

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
