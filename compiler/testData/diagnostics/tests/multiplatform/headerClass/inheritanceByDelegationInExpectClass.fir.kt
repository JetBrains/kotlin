// MODULE: m1-common
// FILE: common.kt

interface A

class B : A
expect class Foo(b: B) : A by b

expect class Bar : A by B()
