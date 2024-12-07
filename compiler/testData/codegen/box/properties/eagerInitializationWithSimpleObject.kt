// KT-70778
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION
// WARNING: The file names are also a part of the test, don't change it!

// FILE: lib2.kt
package pack

class Test(val name: String = test()) {
    companion object {
        fun test() = "OK"
    }
}

// FILE: lib1.kt
import pack.Test

var result: String = "Fail: was not changed"

@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
val z = run { result = Test().name }

fun box(): String {
    return result
}
