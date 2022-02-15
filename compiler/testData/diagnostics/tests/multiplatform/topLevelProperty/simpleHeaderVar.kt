// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect var foo: String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual var foo: String = "JVM"

// MODULE: m3-js()()(m1-common)
// FILE: js.kt
actual var foo: String = "JS"
