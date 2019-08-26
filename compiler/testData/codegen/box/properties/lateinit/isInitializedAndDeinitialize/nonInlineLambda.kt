// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

class Foo {
    private lateinit var foo: String

    fun test(): Boolean {
        val result = { ::foo.isInitialized }()
        foo = ""
        return result
    }
}

fun box(): String {
    val f = Foo()
    if (f.test()) return "Fail 1"
    if (!f.test()) return "Fail 2"
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ isInitialized 
