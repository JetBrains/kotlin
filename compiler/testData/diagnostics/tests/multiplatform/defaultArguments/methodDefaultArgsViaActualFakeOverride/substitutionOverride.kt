// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>() {
    fun foo(p: Int = 1)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
open class Base<T> {
    fun foo(p: T) {}
}

actual class Foo : Base<Int>()
