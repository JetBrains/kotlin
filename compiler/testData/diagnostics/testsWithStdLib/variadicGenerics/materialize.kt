// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNCHECKED_CAST -UNUSED_PARAMETER

fun <vararg Ts> variadic(vararg args: *Ts) {}

fun <T> materialize(): T = Unit as T

fun test() {
    variadic(materialize())
}