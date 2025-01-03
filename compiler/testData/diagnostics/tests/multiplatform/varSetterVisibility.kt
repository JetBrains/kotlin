// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
expect var <!REDECLARATION!>v1<!>: Boolean

expect var <!REDECLARATION!>v2<!>: Boolean
    internal set

expect var <!REDECLARATION!>v3<!>: Boolean
    internal set

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!> {
    var foo: Boolean
}

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C2<!> {
    var foo: Boolean
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual var v1: Boolean = false
    <!ACTUAL_WITHOUT_EXPECT!>private<!> set

actual var v2: Boolean = false

actual var v3: Boolean = false
    <!ACTUAL_WITHOUT_EXPECT!>private<!> set

actual open class C {
    actual var foo: Boolean = false
        <!ACTUAL_WITHOUT_EXPECT!>protected<!> set
}

open class C2Typealias {
    var foo: Boolean = false
        protected set
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>C2<!> = C2Typealias
