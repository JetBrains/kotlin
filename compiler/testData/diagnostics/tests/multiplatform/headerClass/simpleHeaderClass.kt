// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Foo

// MODULE: m3-js()()(m1-common)
// FILE: js.kt
actual class Foo
