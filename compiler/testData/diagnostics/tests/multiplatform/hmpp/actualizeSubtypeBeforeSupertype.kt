// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>A<!><!>() : B
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>C<!><!>() : B
expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>()

// MODULE: intermediate()()(common)
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!><!> : B() {
    // "Nothing to override" in metadata compilation. Unfortunately we don't check metadata compilation in diagnostic tests
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!><!> : B() {
    // Nothing to override in platform compilation.
    fun <!VIRTUAL_MEMBER_HIDDEN{JVM}!>foo<!>() {}
}

// MODULE: main()()(intermediate)
actual open class B {
    open fun foo() {}
}
