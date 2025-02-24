// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

class Param
class O {
    val o = "O"
}
class K {
    val k = "K"
}

context(o: O)
fun <T> K.f(g: context(O) K.(Param) -> T) = g(o, this, Param())

fun box() = with(O()) {
    K().f { o + k }
}
