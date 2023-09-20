// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun <T> foo(t: T)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    actual fun <T> foo(t: T) {}
    fun foo(t: String) {}
}
