// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// MODULE: lib
// FILE: A.kt

package a

context(s: String)
fun f(useArg: Boolean, arg: String = "K") = if (useArg) arg else s

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    return with("O") {
        a.f(false) + a.f(true)
    }
}
