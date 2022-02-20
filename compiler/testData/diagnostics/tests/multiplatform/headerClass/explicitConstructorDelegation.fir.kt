// MODULE: m1-common
// FILE: common.kt
expect open class A {
    constructor(s: String)

    constructor(n: Number) : this("A")
}

expect class B : A {
    <!EXPLICIT_DELEGATION_CALL_REQUIRED!>constructor(i: Int)<!>

    constructor() : super("B")
}
