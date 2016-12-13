// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header open class Foo {
    open fun bar(): String
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

interface Bar {
    fun bar(): String
}

val bar: Bar
    get() = null!!

impl open class Foo : Bar by bar
