// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: m1-common
// FILE: common.kt
expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I1<!> {
    fun foo(x: Int = 1)
    fun bar(x: Int = 1)
}

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I2<!> {
    fun foo(x: Int = 2)
    fun bar(x: Int = 2)
}

<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE!>expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ExpectInterface<!><!> : I1, I2 {
    override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>x: Int<!>)
}

<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE!>interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>CommonInterface<!><!> : I1, I2 {
    override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>x: Int<!>)
}

// MODULE: m1-jvm()()(m1-common)
// FILE: main.kt

actual interface I1 {
    actual fun foo(x: Int)
    actual fun bar(x: Int)
}

actual interface I2 {
    actual fun foo(x: Int)
    actual fun bar(x: Int)
}

actual interface ExpectInterface : I1, I2 {
    actual override fun foo(x: Int)
}
