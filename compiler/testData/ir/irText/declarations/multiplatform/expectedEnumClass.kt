// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: ANY

expect enum class MyEnum {
    FOO,
    BAR
}

actual enum class MyEnum {
    FOO,
    BAR,
    BAZ
}