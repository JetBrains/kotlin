// JVM_ABI_K1_K2_DIFF: KT-63872

typealias S = String

typealias SF<T> = (T) -> S

val f: SF<S> = { it }

fun box(): S = f("OK")
