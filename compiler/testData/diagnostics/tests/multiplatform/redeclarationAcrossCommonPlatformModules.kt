// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS{JVM}!>fun foo()<!> {}
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

<!CONFLICTING_OVERLOADS!>fun foo()<!> {}
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>
