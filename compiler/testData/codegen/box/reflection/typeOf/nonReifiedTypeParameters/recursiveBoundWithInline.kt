// IGNORE_BACKEND: JVM_IR, NATIVE, JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// ^^^ This test fails during the second phase of compilation. Yet it's still used in first phase-only
//     tests such as *IrDeserializationTest*generated.
// KJS_WITH_FULL_RUNTIME

// FILE: lib.kt
import kotlin.reflect.typeOf

inline fun <reified T> bar() {
    baz<T>()
}

inline fun <reified T> baz() {
    typeOf<Set<T>>()
}

// FILE: main.kt
fun <T : Comparable<T>> foo() {
    bar<List<T>>()
    baz<List<T>>()
}

fun box(): String {
    foo<Int>()
    return "OK"
}
