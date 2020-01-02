// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class In<in I>(arg: I)
class Out<out O>(val prop: O)
class Inv<T>(val prop: T)

interface Upper
class Lower : Upper

fun <K> id(arg: K): K = arg

fun test(lower: Lower) {
    id<Inv<Upper>>(Inv(lower))
    id<In<Upper>>(In(lower))
    id<Out<Upper>>(Out(lower))
}