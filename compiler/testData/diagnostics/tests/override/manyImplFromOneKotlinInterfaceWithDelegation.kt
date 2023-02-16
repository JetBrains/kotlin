// FIR_IDENTICAL
// FILE: A.kt

interface A {
    fun foo() {}
}

// FILE: B.kt

interface B : A

// FILE: C.kt

interface C : A

// FILE: test.kt

class Adapter : B, C

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class D<!>(val adapter: Adapter) : B by adapter, C by adapter
