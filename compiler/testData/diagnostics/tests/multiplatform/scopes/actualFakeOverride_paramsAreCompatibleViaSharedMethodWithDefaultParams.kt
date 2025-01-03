// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Shared<!> {
    fun sharedMethod(withDefaultParam: Int = 2) {}
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> : Shared

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Foo : Shared
