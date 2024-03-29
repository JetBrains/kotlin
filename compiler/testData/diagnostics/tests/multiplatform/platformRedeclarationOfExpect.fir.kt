// MODULE: common
// FILE: common.kt
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>

// MODULE: main()()(common)
// FILE: test.kt
expect class Foo
