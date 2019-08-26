// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
//WITH_RUNTIME

fun valueFromDB(value: Any): Any {
    return when (value) {
        is Char -> value
        is Number-> value.toChar()
        is String -> value.single()
        else -> error("Unexpected value of type Char: $value")
    }
}

fun box(): String {
    valueFromDB(1)
    return "" + valueFromDB("O") + valueFromDB("K")
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ single 
