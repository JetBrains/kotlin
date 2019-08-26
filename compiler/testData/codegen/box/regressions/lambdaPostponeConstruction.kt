// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class MyList<T>

operator fun <T> MyList<T>.plusAssign(element: T) {}

val listOfFunctions = MyList<(Int) -> Int>()

fun foo() {
    listOfFunctions.plusAssign({ it -> it })
    listOfFunctions += { it -> it }
}

fun box(): String {
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
