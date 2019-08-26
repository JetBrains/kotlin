// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun test(x: Int): Int {
    x myMap {
        return@myMap
    }

    return 0
}

fun myMap(x: Int): Int {
    x myMap {
        return@myMap
    }

    return 0
}

infix fun Int.myMap(x: () -> Unit) {}

fun box(): String {
    test(0)
    myMap(0)

    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
