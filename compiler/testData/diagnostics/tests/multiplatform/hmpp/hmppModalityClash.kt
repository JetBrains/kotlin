// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
expect abstract <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!><!>() {
    abstract fun foo()
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Impl<!><!> : Foo() {}

<!CONFLICTING_OVERLOADS!>fun common()<!> {
    Impl().foo()
}

// MODULE: intermediate()()(common)
interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I<!> {
    fun foo()
}

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!>() {}

actual abstract <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!><!> : Base(), I {
    // In non-KMP world, these two f/o would squash into a single f/o final fun foo()
    // f/o abstract fun foo(): Unit in intermediate
    // f/o final fun foo(): Unit in platform
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}
