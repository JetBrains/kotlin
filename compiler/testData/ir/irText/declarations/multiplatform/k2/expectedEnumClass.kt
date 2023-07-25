// IGNORE_BACKEND_K1: ANY
// ^^^ K1 as well as K1-based test infra do not support "fragment refinement".
// SKIP_KLIB_TEST
// REASON: KT-68988

// FIR_IDENTICAL
// LANGUAGE: +MultiPlatformProjects

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
