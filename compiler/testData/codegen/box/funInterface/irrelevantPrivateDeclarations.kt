// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
fun interface A {
    fun invoke(s: String)

    private fun privateFun() {}
    private var privateProperty: String
        get() = ""
        set(value) {}

    companion object {
        fun s(a: A) {
            a.invoke("OK")
        }
    }
}

fun test(f: (String) -> Unit) {
    A.s(f)
}

fun box(): String {
    var result = "Fail"
    test { result = it }
    return result
}
