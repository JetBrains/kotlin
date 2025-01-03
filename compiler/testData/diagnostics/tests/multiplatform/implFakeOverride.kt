// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    fun bar(): String
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

open class Bar {
    fun bar() = "bar"
}

actual class Foo : Bar()
