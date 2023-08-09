// FIR_IDENTICAL
// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6
// KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone
// IGNORE_BACKEND_K2: NATIVE
// MODULE: lib
// FILE: lib.kt
expect enum class MyEnum {
    FOO,
    BAR
}

// MODULE: main()()(lib)
// FILE: main.kt
actual enum class MyEnum {
    FOO,
    BAR,
    BAZ
}
