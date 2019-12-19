// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

interface Bound
interface Upper : Bound
class Lower : Upper

class Inv<T>

fun <T : Bound, U : T> makeInv(v: U): Inv<T> = TODO()
fun <K> id(arg: K): K = arg

fun test(lower: Lower) {
    id<Inv<Upper>>(makeInv(lower))
}
