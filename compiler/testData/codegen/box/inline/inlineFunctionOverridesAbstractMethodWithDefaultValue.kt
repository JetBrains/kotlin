// ISSUE: KT-82017
// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI, NATIVE, JVM_IR
interface I {
    abstract fun foo(a: String = "FAIL"): String
}

class A(): I {
    inline override fun foo(a: String): String = "OK"
}

fun box() = A().foo()