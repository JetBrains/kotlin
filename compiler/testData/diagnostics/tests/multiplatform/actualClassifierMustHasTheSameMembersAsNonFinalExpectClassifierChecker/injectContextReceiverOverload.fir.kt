// LANGUAGE: +ContextReceivers
// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    actual fun foo() {}

    context(Int)
    fun foo() {} // accidential override can happen with this injected fun. That's why it's prohibited
}
