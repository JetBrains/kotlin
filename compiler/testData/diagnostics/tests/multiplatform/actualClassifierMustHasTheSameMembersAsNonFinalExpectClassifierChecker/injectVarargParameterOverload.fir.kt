// MODULE: m1-common
// FILE: common.kt

open class Base {
    open fun foo(t: String) {}
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    open fun foo(vararg t: String) {} // injected
}
