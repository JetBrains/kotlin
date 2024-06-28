// MODULE: m1-common
// FILE: common.kt
expect open class A {
    constructor(s: String)

    constructor(n: Number) : <!EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL!>this<!>("A")
}

expect class B : A {
    constructor(i: Int)

    constructor() : <!EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL!>super<!>("B")
}
