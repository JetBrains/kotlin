// ISSUE: KT-77103
// WITH_STDLIB
// LANGUAGE: -IrInlinerBeforeKlibSerialization
// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt
inline fun foo(block: () -> Unit) {}

fun app() {
    foo {
        fun bar() {
            val localDelegatedProperty by lazy { false }
        }
    }
}

// FILE: 2.kt
fun box(): String {
    app()
    return "OK"
}
