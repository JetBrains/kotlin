// MODULE: m1-common
// FILE: common.kt

interface Foo {
    fun foo()
}

expect <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class ImplicitFoo<!> : Foo

expect class ExplicitFoo : Foo {
    override fun foo()
}

expect <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class ImplicitFooCheck<!> : Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class ImplicitFoo : Foo {
    override fun foo() {}
}

actual class ExplicitFoo : Foo {
    actual override fun foo() {}
}

actual <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class ImplicitFooCheck<!> : Foo
