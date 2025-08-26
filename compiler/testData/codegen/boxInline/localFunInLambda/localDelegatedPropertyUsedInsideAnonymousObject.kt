// ISSUE: KT-77103
// WITH_STDLIB
// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt
inline fun <T> foo(block: () -> T) = block()

fun app() : String {
    return foo {
        val localDelegatedProperty by lazy { "OK" }
        object {
            fun get() = localDelegatedProperty
        }.get()
    }
}

// FILE: 2.kt
fun box(): String {
    app()
    return "OK"
}
