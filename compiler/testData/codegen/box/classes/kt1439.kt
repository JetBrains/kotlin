class MyClass(var fnc : () -> String) {

    fun test(): String {
        return fnc()
    }

}

fun printtest() : String {
    return "OK"
}

fun box(): String {
    var c = MyClass({ printtest() })

    return c.test()
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
