// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>WeakIncompatibility<!> {
    @Ann
    fun foo(p: String)
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>StrongIncompatibility<!> {
    @Ann
    fun foo(p: Int)
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class WeakIncompatibilityImpl {
    fun foo(differentName: String) {}
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>WeakIncompatibility<!> = WeakIncompatibilityImpl

class StrongIncompatibilityImpl {
    fun foo(p: String) {} // Different param type
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>StrongIncompatibility<!> = StrongIncompatibilityImpl
