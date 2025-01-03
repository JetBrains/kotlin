// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Supertype<!>()
typealias <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SupertypeAlias<!> = Supertype

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> : Supertype {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : SupertypeAlias() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904
}
