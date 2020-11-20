// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: CLASS_REFERENCES
class Q<TT> {
    fun <T> qz(x: T, block: (T) -> String) = block(x)

    fun problematic(): String {
        class CC

        return qz(CC::class) { "OK" }
    }
}

fun box() = Q<Int>().problematic()
