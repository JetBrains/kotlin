// LANGUAGE: +CompanionBlocksAndExtensions
// TARGET_BACKEND: JS_IR, NATIVE, WASM
// WITH_STDLIB
interface I {
    companion {
        const val O = "O"
        internal val K = "K"

        fun getOk() = O + K
    }
}

fun box() = I.getOk()
