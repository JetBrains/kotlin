// IGNORE_BACKEND: JS_IR
typealias S = String

typealias SF<T> = (T) -> S

val f: SF<S> = { it }

fun box(): S = f("OK")
