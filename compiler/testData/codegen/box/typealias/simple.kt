// IGNORE_BACKEND: WASM
typealias S = String

typealias SF<T> = (T) -> S

val f: SF<S> = { it }

fun box(): S = f("OK")
