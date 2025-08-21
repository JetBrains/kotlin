// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// IGNORE_FIR_DIAGNOSTICS

// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS{JVM}!>fun foo()<!> {}
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!>

open class Base {
    open fun foo() {}
}
expect class Bar : Base {
}

expect open class ExpectBase {
    open fun foo()
}
expect class Baz : ExpectBase

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

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, override */
