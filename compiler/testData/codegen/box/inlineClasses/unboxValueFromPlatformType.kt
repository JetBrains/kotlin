// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND: NATIVE

inline class SnekDirection(val direction: Int) {
    companion object {
        val Up = SnekDirection(0)
    }
}

fun testUnbox() : SnekDirection {
    val list = arrayListOf(SnekDirection.Up)
    return list[0]
}

fun box(): String {
    val a = testUnbox()
    return if (a.direction == 0) "OK" else "Fail"
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
