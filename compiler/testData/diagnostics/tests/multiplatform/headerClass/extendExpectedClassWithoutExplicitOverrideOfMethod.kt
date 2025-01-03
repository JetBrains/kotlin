// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!> {
    abstract fun foo()
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedImplicit<!> : Base

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedExplicit<!> : Base {
    override fun foo()
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedExplicitCheck<!> : Base {
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
