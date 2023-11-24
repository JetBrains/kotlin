// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6
// FIR status: don't support legacy feature (see https://youtrack.jetbrains.com/issue/KT-37591). UNRESOLVED_REFERENCE at '+'
// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt

enum class E(val value: String) {
    OK("K");

    companion object {
        @JvmField
        val OK = "O"
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box() = E.OK + E.OK.value
