// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I<!> {
    fun foo() {}
}
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> : I

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo : I by object : I {
    override fun foo() {}
}
