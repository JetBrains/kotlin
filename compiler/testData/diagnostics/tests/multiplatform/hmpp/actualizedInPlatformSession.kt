// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!><!>() {
    fun foo()
}

// MODULE: intermediate()()(common)
expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!>() {}

actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!><!> : Base() {
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}
