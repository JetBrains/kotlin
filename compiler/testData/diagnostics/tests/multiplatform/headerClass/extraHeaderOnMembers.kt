// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class H {
    <!JVM:WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET!>expect<!> fun foo()
}

// MODULE: m1-jvm(m1-common)
// FILE: jvm.kt

actual class H {
    actual fun foo() {}
}
