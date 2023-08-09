// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: ANY
// FIR status: In FIR, declaring the same `expect` and `actual` classes in one compiler module is not possible (see KT-55177).

// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

// KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone
// IGNORE_BACKEND_K1: NATIVE

expect enum class MyEnum {
    FOO,
    BAR
}

actual enum class MyEnum {
    FOO,
    BAR,
    BAZ
}
