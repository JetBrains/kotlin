// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
header fun foo()

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
impl fun foo() {}

// MODULE: m3-js(m1-common)
// FILE: js.kt
impl fun foo() {}
