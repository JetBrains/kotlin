// !LANGUAGE: +NewInference +VariadicGenerics
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun box() {
    innerBoxed(Box(Unit), Box(42))
}

class Box<T>(val value: T)

fun <vararg Ks> foo(
    vararg args: *Box<Ks>
) {}

fun <vararg Ts> innerBoxed(
    vararg args: *Box<Ts>
) {
    foo(*args)
}