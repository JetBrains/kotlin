// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

open class Base<T> {
    open fun foo(t: T) {}
}

expect open class Foo : Base<String>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base<String>() {
    override fun foo(t: String) {}
}
