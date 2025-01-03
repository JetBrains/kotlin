// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!> {
    fun existingMethodInBase()
}

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> : Base {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Base {
    actual fun existingMethodInBase() {}
    fun injected() {}
}

actual open class Foo : Base() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904
}
