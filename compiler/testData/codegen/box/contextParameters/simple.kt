// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun String.self() = this

context(s: String)
fun f() = s.self()

context(s: String)
val v: String get() = s.self()

fun <T, R> context(t: T, f: context(T) () -> R): R = f(t)

fun box(): String {
    return with("O") { f() } + context("K") { v }
}