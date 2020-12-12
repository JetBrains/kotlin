// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: CALLABLE_REFERENCES_FAIL
inline fun <TT> id(x: TT): TT = x
inline fun <TT> String.extId(x: TT): String = this

private fun <T> ff(value: T?): String {
    // In PSI2IR, the funcref is approximated to Function1<Nothing, Pair<String, T>>
    value?.let(::id)
    return value?.let("OK"::extId)!!
}

fun box() = ff("arg")

