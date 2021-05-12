// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
enum class X {
    B {
        override val value = "OK"

        override val test = ::value.get()
    };

    abstract val value: String

    abstract val test: String
}

fun box() = X.B.test
