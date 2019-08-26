// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// KT-2739 Error type inferred for hashSet(Pair, Pair, Pair)

fun <T> foo(vararg ts: T): T? = null

class Pair<A>(a: A)

fun box(): String {
    val v = foo(Pair(1))
    return if (v == null) "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: WASM_ARRAYS_UNSUPPORTED
