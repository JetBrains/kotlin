// WITH_RUNTIME
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
class C(val x: String) {
    fun foo(i: Int): Char = x[i]
}

fun box(): String {
    val array = CharArray(2, C("OK")::foo)
    return array[0].toString() + array[1].toString()
}
