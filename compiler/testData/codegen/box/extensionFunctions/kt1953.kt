// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val sb = StringBuilder()
    operator fun String.unaryPlus() {
        sb.append(this)
    }

    +"OK"
    return sb.toString()!!
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_STRING_BUILDER
