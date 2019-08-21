// !LANGUAGE: +NewInference

fun <vararg Ts> variadic(vararg args: *Ts) {}

fun <T> materialize(): T = Unit as T

fun test() {
    variadic(materialize())
}