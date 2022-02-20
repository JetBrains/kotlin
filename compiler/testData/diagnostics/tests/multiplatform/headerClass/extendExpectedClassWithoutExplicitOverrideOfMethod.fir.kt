// MODULE: m1-common
// FILE: common.kt

expect abstract class Base {
    abstract fun foo()
}

expect class DerivedImplicit : Base

expect class DerivedExplicit : Base {
    override fun foo()
}

expect class DerivedExplicitCheck : Base {
    override fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual abstract class Base {
    actual abstract fun foo()
}

actual class DerivedImplicit : Base() {
    override fun foo() {}
}

actual class DerivedExplicit : Base() {
    actual override fun foo() {}
}

actual class DerivedExplicitCheck : Base() {
    override fun foo() {}
}
