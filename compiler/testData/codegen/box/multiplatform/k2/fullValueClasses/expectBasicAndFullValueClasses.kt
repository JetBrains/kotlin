// LANGUAGE: +MultiPlatformProjects, +FullValueClasses
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING

// MODULE: common
// FILE: common.kt

expect value class Value1(val x: Char)
expect value class Value2(val x: Char)

// MODULE: main()()(common)
// FILE: test.kt

OPTIONAL_JVM_INLINE_ANNOTATION
actual value class Value1 actual constructor(val x: Char)
actual value class Value2 actual constructor(val x: Char)

fun box() = Value1('O').x.toString() + Value2('K').x
