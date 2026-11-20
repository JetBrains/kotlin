// DUMP_IR_DIFFERENCE: JKLIB
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
