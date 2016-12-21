// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
header class Foo {
    val foo: String

    fun bar(x: Int): Int
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
impl class Foo {
    impl val foo: String = "JVM"

    impl fun bar(x: Int): Int = x + 1
}

// MODULE: m3-js(m1-common)
// FILE: js.kt
impl class Foo {
    impl val foo: String = "JS"

    impl fun bar(x: Int): Int = x - 1
}
