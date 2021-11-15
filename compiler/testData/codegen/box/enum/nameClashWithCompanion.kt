// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
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
