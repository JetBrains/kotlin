// MODULE: m1-common
// FILE: common.kt

interface A

class B : A
expect class Foo(b: B) : <!IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS!>A by b<!>

expect class Bar : <!IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS!>A by B()<!>
