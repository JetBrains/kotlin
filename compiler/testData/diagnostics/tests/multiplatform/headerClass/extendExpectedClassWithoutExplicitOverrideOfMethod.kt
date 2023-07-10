// MODULE: m1-common
// FILE: common.kt

expect abstract class Base {
    abstract fun foo()
}

expect <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED{JVM}!>class DerivedImplicit<!> : Base

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
    override fun <!ACTUAL_MISSING!>foo<!>() {}
}
