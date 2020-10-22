// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
class A {
    private lateinit var str: String

    public fun getMyStr(): String {
        try {
            val a = str
        } catch (e: RuntimeException) {
            return "OK"
        }

        return "FAIL"
    }
}

fun box(): String {
    val a = A()
    return a.getMyStr()
}