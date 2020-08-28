// !DIAGNOSTICS: -UNUSED_PARAMETER

class A(x: String = "", y: String = "") {
    constructor(x: String, y: String): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(x, y)
    constructor(): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>("", "")
    constructor(): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>("", "")
}

class B {
    constructor(x: Int)
}

fun B(x: Int) {}

class Outer {
    class A(x: String = "", y: String = "") {
        constructor(x: String, y: String): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(x, y)
        constructor(): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>("", "")
        constructor(): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>("", "")
    }

    class B {
        constructor(x: Int)
    }

    fun B(x: Int) {}
}