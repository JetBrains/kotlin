// FIR_IDENTICAL
// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: ANY

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