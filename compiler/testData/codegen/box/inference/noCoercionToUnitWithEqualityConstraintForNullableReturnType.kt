// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
// IGNORE_BACKEND_FIR: JVM_IR

class Inv<T>(val x: T?)

fun <R> foo(f: () -> R?): Inv<R> {
    val r = f()
    if (r != null) throw Exception("fail, result is not null: $r")
    return Inv(r)
}

fun box(): String {
    val r: Inv<Unit> = foo { if (false) Unit else null }
    return "OK"
}
