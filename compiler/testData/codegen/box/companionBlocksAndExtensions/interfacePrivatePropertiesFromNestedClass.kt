// LANGUAGE: +CompanionBlocks
// IGNORE_BACKEND: JS_IR, WASM
// JS_IR, WASM: KT-89290

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
    if (!I2.NestedClass().check()) return "fail 2"
    return "OK"
}
