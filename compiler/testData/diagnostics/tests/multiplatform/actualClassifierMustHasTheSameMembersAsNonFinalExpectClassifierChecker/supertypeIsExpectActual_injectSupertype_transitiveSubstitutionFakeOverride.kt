// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!><T> {
    fun injected(param: T)
}

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Base<T> {
    actual fun injected(param: T) {}
}

open class Transitive : Base<String>()

actual open class Foo : Transitive() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904
}
