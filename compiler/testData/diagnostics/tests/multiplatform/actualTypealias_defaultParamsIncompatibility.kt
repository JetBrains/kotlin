// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    fun foo(a: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = FooImpl

class FooImpl {
    fun foo(a: Int = 2) {
    }
}
