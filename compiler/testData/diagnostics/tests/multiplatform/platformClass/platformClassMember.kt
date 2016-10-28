// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
platform class Foo {
    val foo: String

    fun bar(x: Int): Int
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
class Foo {
    val foo: String = "JVM"

    fun bar(x: Int): Int = x + 1
}

// MODULE: m3-js(m1-common)
// FILE: js.kt
class Foo {
    val foo: String = "JS"

    fun bar(x: Int): Int = x - 1
}
