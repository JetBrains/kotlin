// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun foo(useScriptArgs: Array<out Any?>?) {
    val constructorArgs: Array<out Any?> = arrayOf(useScriptArgs.orEmpty())
}

inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: emptyArray<T>()

fun box(): String {
    foo(arrayOf(1))
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ emptyArray 
