// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
interface Z {

    fun testFun(): String {
        return { privateFun() } ()
    }

    fun testProperty(): String {
        return { privateProp } ()
    }

    private fun privateFun(): String {
        return "O"
    }

    private val privateProp: String
        get() = "K"
}

object Z2 : Z {

}

fun box(): String {
    return Z2.testFun() + Z2.testProperty()
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
