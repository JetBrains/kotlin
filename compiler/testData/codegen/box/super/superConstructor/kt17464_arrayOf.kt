// IGNORE_BACKEND: WASM
open class A(val array: Array<Any>)

class B : A(arrayOf("OK"))

fun box() = B().array[0].toString()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ARRAYS
