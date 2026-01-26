// DONT_TARGET_EXACT_BACKEND: JVM_IR
// ^EagerInitialization is not supported
// PROPERTY_LAZY_INITIALIZATION
// WASM_FAILS_IN_SINGLE_MODULE_MODE
// WASM_FAILS_IN_MULTI_MODULE_MODE
// MODULE: lib1
// FILE: lib.kt
var z1 = false
var z2 = false

// MODULE: lib2(lib1)
// FILE: lib2.kt

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
val x = foo()

private fun foo(): Int {
    z1 = true
    return 42
}

// Will be initialized since [x]'s initializer calls a function from the file.
val y = run { z2 = true; 117 }

// MODULE: main(lib1, lib2)
// FILE: main.kt

fun box(): String {
    return return if (z1 && z2) "OK" else "fail"
}
