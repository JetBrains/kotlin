// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
expect class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Foo
