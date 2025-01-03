// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    fun injectedMethod() {}
}
