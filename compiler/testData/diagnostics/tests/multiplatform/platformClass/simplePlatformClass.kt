// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
header class Foo

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
class Foo

// MODULE: m3-js(m1-common)
// FILE: js.kt
class Foo
