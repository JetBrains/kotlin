// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{JVM}!>fun foo()<!> {}
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!>

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!> {
    open fun foo() {}
}
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Bar<!> : Base {
}

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ExpectBase<!> {
    open fun foo()
}
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Baz<!> : ExpectBase

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

<!CONFLICTING_OVERLOADS!>actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>()<!> {}
actual class <!ACTUAL_WITHOUT_EXPECT, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>

actual class Bar : Base() {
    actual override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
}

actual open class ExpectBase {
    actual open fun foo() {}
}
actual class Baz : ExpectBase() {
    actual override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
}
