// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: REFLECTION

fun box(): String {
    lateinit var str: String
    try {
        println(str)
        return "Should throw an exception"
    }
    catch (e: UninitializedPropertyAccessException) {
        return "OK"
    }
    catch (e: Throwable) {
        return "Unexpected exception: ${e::class}"
    }
}