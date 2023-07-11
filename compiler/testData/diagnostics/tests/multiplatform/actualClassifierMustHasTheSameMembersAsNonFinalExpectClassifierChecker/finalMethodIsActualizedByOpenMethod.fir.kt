// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    // Hypothetically, it's more restricting than necessary. I can't see how actualizing final -> open can breaking anything.
    // But technically, actual and expect scopes don't match
    actual open fun foo() {
    }
}
