enum class X {
    B {
        override val value = "OK"

        override val test = ::value.get()
    };

    abstract val value: String

    abstract val test: String
}

fun box() = X.B.test


// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
