// LANGUAGE: +CompanionBlocks
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI, NATIVE
// JS_IR, WASM: KT-89290
// NATIVE: KT-89375

interface I {
    companion {
        private val privateVal = "OK"
    }

    class NestedClass {
        fun read() = privateVal
    }
}

interface I2 {
    companion {
        private val privateVal = "OK"
    }

    companion object {
        fun read() = privateVal
    }
}

interface I3 {
    companion {
        private lateinit var late: String
        private val init = initLate()

        private fun initLate() {
            late = "late"
        }
    }

    class NestedClass {
        fun check() = ::late.isInitialized
    }
}

fun box(): String {
    if (I.NestedClass().read() != "OK") return "fail 1"
    if (I2.read() != "OK") return "fail 2"
    if (!I3.NestedClass().check()) return "fail 3"
    return "OK"
}
