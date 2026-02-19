// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// ^ returnType is not supported

// WITH_REFLECT

package test

fun <T> foo(x: T) = x

fun box(): String {
    val bar: kotlin.reflect.KFunction1<Int, Int> = ::foo
    val returnType = bar.returnType
    if (returnType.toString() != "T") return returnType.toString()
    return "OK"
}
