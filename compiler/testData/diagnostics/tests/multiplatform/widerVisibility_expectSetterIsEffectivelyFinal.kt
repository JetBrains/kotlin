// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!> {
    open var foo: Int = 2
        internal set
}
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> : Base {
    override var foo: Int
        internal set
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo : Base() {
    actual override var foo: Int = 2
        <!ACTUAL_WITHOUT_EXPECT!>public<!> set
}
