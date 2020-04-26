//WITH_RUNTIME

fun box(): String {
    var methodVar = "OK"

    fun localMethod() : String
    {
        return lazy { methodVar }::value.get()
    }

    return localMethod()
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
