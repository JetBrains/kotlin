// LANGUAGE: +ContextParameters
// JVM_ABI_K1_K2_DIFF: different order of function annotations

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
