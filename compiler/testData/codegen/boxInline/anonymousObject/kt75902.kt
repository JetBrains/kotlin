// ISSUE: KT-75902
// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM
// LANGUAGE: -IrInlinerBeforeKlibSerialization

// FILE: foo.kt
inline fun <T, R> foo(t: T, f: (T) -> R): R = f(t)

// FILE: box.kt
fun <S> bar(s: S): S {
    return foo(s) { value ->
        object {
            fun method1(): S {
                return object {
                    fun method2(): S {
                        return s
                    }
                }.method2()
            }
        }.method1()
    }
}

fun box(): String {
    return bar("OK")
}
