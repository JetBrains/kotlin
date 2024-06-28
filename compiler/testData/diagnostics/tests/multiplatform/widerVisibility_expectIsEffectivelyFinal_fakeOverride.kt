// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

open class Base {
    protected open fun foo() {}
}
expect class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo : Base() {
    public override fun foo() {
    }
}
