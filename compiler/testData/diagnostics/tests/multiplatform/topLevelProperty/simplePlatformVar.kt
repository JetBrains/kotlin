// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
header var foo: String

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
impl var foo: String = "JVM"

// MODULE: m3-js(m1-common)
// FILE: js.kt
impl var foo: String = "JS"
