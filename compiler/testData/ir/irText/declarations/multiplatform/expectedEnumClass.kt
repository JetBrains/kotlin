// IGNORE_BACKEND_FIR: JVM_IR
// !LANGUAGE: +MultiPlatformProjects

expect enum class MyEnum {
    FOO,
    BAR
}

actual enum class MyEnum {
    FOO,
    BAR,
    BAZ
}