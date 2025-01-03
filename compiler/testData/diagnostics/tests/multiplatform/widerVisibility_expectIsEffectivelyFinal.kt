// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!> {
    internal open fun foo() {}
}
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> : Base {
    override fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo : Base() {
    <!ACTUAL_WITHOUT_EXPECT!>public<!> actual override fun foo() {
    }
}
