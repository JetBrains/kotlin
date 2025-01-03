// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    fun foo()
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ImplicitFoo<!> : Foo

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ExplicitFoo<!> : Foo {
    override fun foo()
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ImplicitFooCheck<!> : Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class ImplicitFoo : Foo {
    override fun foo() {}
}

actual class ExplicitFoo : Foo {
    actual override fun foo() {}
}

actual <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class ImplicitFooCheck<!> : Foo
