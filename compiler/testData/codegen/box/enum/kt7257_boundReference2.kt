// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
enum class X {
    B {

        override val value = "OK"

        val bmr = B::value.get()

        override fun foo(): String {
            return bmr
        }
    };

    abstract val value: String

    abstract fun foo(): String
}

fun box() = X.B.foo()