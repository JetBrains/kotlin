// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!><T> {
    open fun foo(t: T) {}
}

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!><R> : Base<R>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo<R>() : Base<R>() {
    fun <T> foo(t: T) {}
}
