// IGNORE_BACKEND: JS_IR JS_IR_ES6 WASM_JS WASM_WASI
// IGNORE_BACKEND_K1: JVM_IR

// FILE: IO.kt
var initOrder = "IO;"

// FILE: F1.kt

interface I {
    fun testI() {}
    companion object {
        init {
            initOrder += "I;"
        }
    }
}

interface J {
    fun testJ() {}
    companion object {
        init {
            initOrder += "J;"
        }
    }
}

interface K : I {
    companion object {
        init {
            initOrder += "K;"
        }
        fun test() = initOrder
    }
}

class C : J, K {
    companion object {
        init {
            initOrder += "C;"
        }
        fun test() = initOrder
    }
}


// FILE: F2.kt

fun box() : String {
    val result = C.test()
    if (result != "IO;J;I;C;") return "FAIL: $result"
    val result2 = K.test()
    if (result2 != "${result}K;") return "FAIL: $result2"
    return "OK"
}