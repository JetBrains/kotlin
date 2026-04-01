// IGNORE_BACKEND: JVM_IR, NATIVE, JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ This test fails during the second phase of compilation. Yet it's still used in first phase-only
//     tests such as *IrDeserializationTest*generated, apart from Native, where special backend checks detect forbidden IR snippet.
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.typeOf

fun <T : Comparable<T>> foo() {
    typeOf<List<T>>()
}

fun box(): String {
    foo<Int>()
    return "OK"
}
