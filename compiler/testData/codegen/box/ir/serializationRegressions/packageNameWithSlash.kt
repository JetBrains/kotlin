// ISSUE: KT-85639
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// IGNORE_HEADER_MODE: JS_IR
// ^^^ A slash in a package name is bad for ClassId: it uses the slash to separate the package from the class name.

// MODULE: lib
// FILE: lib.kt
package lib.`b/c`

class X {
    fun value(): String = "class"
}

fun topLevel(): String = "function"

// MODULE: main(lib)
// FILE: main.kt
import lib.`b/c`.X
import lib.`b/c`.topLevel

fun box(): String {
    if (X().value() != "class") return "FAIL: class"
    if (topLevel() != "function") return "FAIL: function"
    return "OK"
}
