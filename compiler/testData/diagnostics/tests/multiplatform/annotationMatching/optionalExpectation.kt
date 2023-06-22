// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class MyAnnotation

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@OptIn(ExperimentalMultiplatform::class)
actual annotation class MyAnnotation
