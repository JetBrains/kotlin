// !LANGUAGE: +NewInference
// WITH_RUNTIME
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM

fun <vararg Ts> variadic(vararg args: *Ts) {}

fun <T> materialize(): T = Unit as T

fun test() {
    variadic(materialize())
}