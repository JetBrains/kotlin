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

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: MINOR: NULLABLE_BOX_FUNCTION



