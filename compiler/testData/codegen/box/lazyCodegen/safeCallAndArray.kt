// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: NULLABLE_BOX_FUNCTION
class C {
    fun calc() : String {
        return "OK"
    }
}

fun box(): String? {
    val c: C? = C()
    val arrayList = arrayOf(c?.calc(), "")
    return arrayList[0]
}
