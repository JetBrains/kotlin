// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    actual fun foo() {}

    fun foo(overloaded: Int) {}
}
