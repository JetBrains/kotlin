// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

// MODULE: lib
// FILE: A.kt

package a

context(String)
fun f(useArg: Boolean, arg: String = "K") = if (useArg) arg else this@String

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    return with("O") {
        a.f(false) + a.f(true)
    }
}
