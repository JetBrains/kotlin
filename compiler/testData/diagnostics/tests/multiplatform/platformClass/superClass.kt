// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

interface I
open class C
interface J

platform class Foo : I, C, J

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
class Foo : I, C(), J

// MODULE: m3-js(m1-common)
// FILE: js.kt
class Foo : I, J, C()
